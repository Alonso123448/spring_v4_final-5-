package com.example.spring_v4.controller;

import com.example.spring_v4.entity.DetalleVenta;
import com.example.spring_v4.entity.Producto;
import com.example.spring_v4.entity.Venta;
import com.example.spring_v4.repository.LogoImagenRepository;
import com.example.spring_v4.repository.ProductoRepository;
import com.example.spring_v4.repository.VentaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final VentaRepository ventaRepo;
    private final ProductoRepository productoRepo;
    private final LogoImagenRepository logoRepo;

    public ReporteController(VentaRepository ventaRepo,
                             ProductoRepository productoRepo,
                             LogoImagenRepository logoRepo) {
        this.ventaRepo = ventaRepo;
        this.productoRepo = productoRepo;
        this.logoRepo = logoRepo;
    }

    private static final String[] MESES = {
        "Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"
    };

    @GetMapping({"", "/"})
    public String reportes(Model model) {

        List<Venta> todas = ventaRepo.findAll();

        // ── Ventas válidas: no anuladas, online solo aprobadas, con fecha y total ──
        List<Venta> validas = todas.stream()
                .filter(v -> !"ANULADA".equals(v.getEstado()))
                .filter(v -> v.getAprobacionPedido() == null || "APROBADO".equals(v.getAprobacionPedido()))
                .filter(v -> v.getFecha() != null && v.getTotal() != null)
                .toList();

        LocalDate hoy = LocalDate.now();
        YearMonth mesActual = YearMonth.from(hoy);

        // ── KPIs ──────────────────────────────────────────────────────────
        BigDecimal ventasHoy = BigDecimal.ZERO;
        BigDecimal ventasMes = BigDecimal.ZERO;
        BigDecimal ventasAnio = BigDecimal.ZERO;
        long transaccionesMes = 0;
        BigDecimal creditosPorCobrar = BigDecimal.ZERO;

        for (Venta v : validas) {
            LocalDate f = v.getFecha().toLocalDate();
            if (f.equals(hoy))                          ventasHoy  = ventasHoy.add(v.getTotal());
            if (YearMonth.from(f).equals(mesActual))  { ventasMes  = ventasMes.add(v.getTotal()); transaccionesMes++; }
            if (f.getYear() == hoy.getYear())           ventasAnio = ventasAnio.add(v.getTotal());
        }
        // Créditos por cobrar (deuda pendiente, incluso de ventas antiguas)
        for (Venta v : todas) {
            if (!"ANULADA".equals(v.getEstado())
                    && Boolean.TRUE.equals(v.getEsCredito())
                    && v.getDeuda() != null
                    && v.getDeuda().compareTo(BigDecimal.ZERO) > 0) {
                creditosPorCobrar = creditosPorCobrar.add(v.getDeuda());
            }
        }

        // ── Ventas por mes (últimos 12 meses) ─────────────────────────────
        List<String> mesLabels = new ArrayList<>();
        List<BigDecimal> mesTotales = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = mesActual.minusMonths(i);
            mesLabels.add(MESES[ym.getMonthValue() - 1] + " " + ym.getYear());
            BigDecimal suma = BigDecimal.ZERO;
            for (Venta v : validas) {
                if (YearMonth.from(v.getFecha().toLocalDate()).equals(ym)) {
                    suma = suma.add(v.getTotal());
                }
            }
            mesTotales.add(suma);
        }

        // ── Productos cacheados para lookups (id y nombre) ────────────────
        List<Producto> productos = productoRepo.findAll();
        Map<Integer, Producto> prodPorId = new HashMap<>();
        Map<String, Producto> prodPorNombre = new HashMap<>();
        for (Producto p : productos) {
            prodPorId.put(p.getId(), p);
            if (p.getNombre() != null) prodPorNombre.put(p.getNombre().trim().toLowerCase(), p);
        }

        // ── Top 10 productos más vendidos + Ingresos por categoría ────────
        Map<String, Integer> vendidosPorProducto = new LinkedHashMap<>();
        Map<String, BigDecimal> ingresosPorCategoria = new LinkedHashMap<>();

        for (Venta v : validas) {
            if (v.getDetalles() == null) continue;
            for (DetalleVenta d : v.getDetalles()) {
                String nombre = d.getProductoNombre() != null ? d.getProductoNombre().trim() : "Desconocido";
                int cant = d.getCantidad() != null ? d.getCantidad() : 0;
                vendidosPorProducto.merge(nombre, cant, Integer::sum);

                // categoría: por id, fallback por nombre
                Producto p = d.getProductoId() != null ? prodPorId.get(d.getProductoId()) : null;
                if (p == null) p = prodPorNombre.get(nombre.toLowerCase());
                String cat = (p != null && p.getCategoria() != null && p.getCategoria().getNombre() != null)
                        ? p.getCategoria().getNombre() : "Otros";
                BigDecimal sub = d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO;
                ingresosPorCategoria.merge(cat, sub, BigDecimal::add);
            }
        }

        List<Map<String, Object>> topProductos = vendidosPorProducto.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("nombre", e.getKey());
                    m.put("cantidad", e.getValue());
                    return m;
                })
                .toList();

        List<Map<String, Object>> categorias = ingresosPorCategoria.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("nombre", e.getKey());
                    m.put("total", e.getValue());
                    return m;
                })
                .toList();

        // ── Ventas por método de pago ─────────────────────────────────────
        Map<String, BigDecimal> pagoTotales = new LinkedHashMap<>();
        Map<String, Long> pagoConteos = new LinkedHashMap<>();
        for (Venta v : validas) {
            String metodo = v.getFormaPago() != null && !v.getFormaPago().isBlank()
                    ? v.getFormaPago().trim().toUpperCase() : "OTRO";
            pagoTotales.merge(metodo, v.getTotal(), BigDecimal::add);
            pagoConteos.merge(metodo, 1L, Long::sum);
        }
        List<Map<String, Object>> metodosPago = pagoTotales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("metodo", e.getKey());
                    m.put("total", e.getValue());
                    m.put("cantidad", pagoConteos.get(e.getKey()));
                    return m;
                })
                .toList();

        // ── Ventas por origen (catálogo vs administrativo) ────────────────
        BigDecimal totalCatalogo = BigDecimal.ZERO, totalAdmin = BigDecimal.ZERO;
        long pedidosCatalogo = 0, pedidosAdmin = 0;
        for (Venta v : validas) {
            if ("PEDIDO-ONLINE".equals(v.getSerie())) {
                totalCatalogo = totalCatalogo.add(v.getTotal()); pedidosCatalogo++;
            } else {
                totalAdmin = totalAdmin.add(v.getTotal()); pedidosAdmin++;
            }
        }

        // ── Productos con stock bajo ──────────────────────────────────────
        List<Map<String, Object>> stockBajo = new ArrayList<>();
        for (Producto p : productos) {
            if (!Boolean.TRUE.equals(p.getEstado())) continue;
            int stock = p.getStock() != null ? p.getStock() : 0;
            int minimo = p.getStockMinimo() != null ? p.getStockMinimo() : 5;
            if (stock <= minimo) {
                Map<String, Object> m = new HashMap<>();
                m.put("nombre", p.getNombre() != null ? p.getNombre() : "-");
                m.put("categoria", p.getCategoria() != null && p.getCategoria().getNombre() != null
                        ? p.getCategoria().getNombre() : "-");
                m.put("stock", stock);
                m.put("minimo", minimo);
                m.put("sinStock", stock == 0);
                stockBajo.add(m);
            }
        }
        stockBajo.sort(Comparator.comparingInt(m -> (int) m.get("stock")));

        // ── Modelo ────────────────────────────────────────────────────────
        model.addAttribute("ventasHoy", ventasHoy);
        model.addAttribute("ventasMes", ventasMes);
        model.addAttribute("ventasAnio", ventasAnio);
        model.addAttribute("transaccionesMes", transaccionesMes);
        model.addAttribute("creditosPorCobrar", creditosPorCobrar);
        model.addAttribute("stockBajoCount", stockBajo.size());

        model.addAttribute("mesLabels", mesLabels);
        model.addAttribute("mesTotales", mesTotales);
        model.addAttribute("topProductos", topProductos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("metodosPago", metodosPago);

        model.addAttribute("totalCatalogo", totalCatalogo);
        model.addAttribute("totalAdmin", totalAdmin);
        model.addAttribute("pedidosCatalogo", pedidosCatalogo);
        model.addAttribute("pedidosAdmin", pedidosAdmin);

        model.addAttribute("stockBajo", stockBajo);

        logoRepo.findByActivoTrue().stream().findFirst()
                .ifPresent(l -> model.addAttribute("logoActivo", l));

        return "reportes";
    }
}
