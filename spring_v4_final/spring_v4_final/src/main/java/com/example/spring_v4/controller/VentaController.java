package com.example.spring_v4.controller;

import com.example.spring_v4.entity.*;
import com.example.spring_v4.repository.*;
import com.example.spring_v4.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaRepository ventaRepo;
    private final CuotaRepository cuotaRepo;
    private final ProductoRepository productoRepo;
    private final LogoImagenRepository logoRepo;
    private final ClienteRepository clienteRepo;

    @Value("${miapi.token}")
    private String apiToken;

    public VentaController(VentaRepository ventaRepo, CuotaRepository cuotaRepo,
                           ProductoRepository productoRepo, LogoImagenRepository logoRepo,
                           ClienteRepository clienteRepo) {
        this.ventaRepo    = ventaRepo;
        this.cuotaRepo    = cuotaRepo;
        this.productoRepo = productoRepo;
        this.logoRepo     = logoRepo;
        this.clienteRepo  = clienteRepo;
    }

    // =====================================
    // LISTADO
    // =====================================
    @GetMapping({"", "/"})
    public String listar(Model model) {
        model.addAttribute("ventas", ventaRepo.findAll());
        logoRepo.findByActivoTrue().stream().findFirst()
                .ifPresent(l -> model.addAttribute("logoActivo", l));
        return "ventas";
    }

    // =====================================
    // GUARDAR NUEVA VENTA
    // =====================================
    @PostMapping("/guardar")
    @Transactional
    public String guardar(
            @RequestParam String clienteDocumento,
            @RequestParam String clienteNombre,
            @RequestParam String serie,
            @RequestParam String formaPago,
            @RequestParam(defaultValue = "false") boolean esCredito,
            @RequestParam(required = false) BigDecimal pagoInicial,
            @RequestParam(required = false) Integer nCuotas,
            @RequestParam(required = false) String intervaloPago,
            @RequestParam List<Integer> productosId,
            @RequestParam List<String> productosNombre,
            @RequestParam List<BigDecimal> productosPrecio,
            @RequestParam List<Integer> productosCantidad,
            Authentication auth,
            RedirectAttributes ra) {

        // ── Validar stock disponible (backend) ──────────────────────────────
        for (int i = 0; i < productosId.size(); i++) {
            Producto prod = productoRepo.findById(productosId.get(i)).orElse(null);
            if (prod == null) {
                ra.addFlashAttribute("error", "Producto no encontrado: " + productosNombre.get(i));
                return "redirect:/ventas";
            }
            if (prod.getStock() == null || productosCantidad.get(i) > prod.getStock()) {
                ra.addFlashAttribute("error",
                    "Stock insuficiente para '" + prod.getNombre() +
                    "'. Disponible: " + (prod.getStock() != null ? prod.getStock() : 0) +
                    ", solicitado: " + productosCantidad.get(i) + ".");
                return "redirect:/ventas";
            }
        }

        // Número de documento — sincronizado para evitar duplicados bajo carga concurrente
        String numDoc;
        synchronized (VentaController.class) {
            long count = ventaRepo.countBySerie(serie) + 1;
            numDoc = serie.contains("N001") ? String.format("N001-%08d", count) : String.format("DOC-%08d", count);
        }

        // Calcular total y armar detalles
        BigDecimal total = BigDecimal.ZERO;
        List<DetalleVenta> detalles = new ArrayList<>();
        for (int i = 0; i < productosNombre.size(); i++) {
            DetalleVenta d = new DetalleVenta();
            d.setProductoNombre(productosNombre.get(i));
            d.setPrecio(productosPrecio.get(i));
            d.setCantidad(productosCantidad.get(i));
            BigDecimal sub = productosPrecio.get(i).multiply(BigDecimal.valueOf(productosCantidad.get(i)));
            d.setSubtotal(sub);
            total = total.add(sub);
            detalles.add(d);
        }

        Venta v = new Venta();
        v.setNumeroDocumento(numDoc);
        v.setSerie(serie);
        v.setClienteDocumento(clienteDocumento);
        v.setClienteNombre(clienteNombre);
        v.setVendedor(auth != null ? auth.getName() : "Sistema");
        v.setFecha(LocalDateTime.now());
        v.setTotal(total);
        v.setFormaPago(formaPago);
        v.setEsCredito(esCredito);
        v.setEstado("COMPLETADA");

        // Crédito
        BigDecimal deuda = BigDecimal.ZERO;
        if (esCredito) {
            // Si no se ingresó pago inicial, se trata como 0
            BigDecimal pi = (pagoInicial != null) ? pagoInicial : BigDecimal.ZERO;
            if (pi.compareTo(total) >= 0) {
                ra.addFlashAttribute("error", "El pago inicial no puede ser igual o mayor al total. Si cubre el monto completo, usa la forma de pago Contado.");
                return "redirect:/ventas";
            }
            deuda = total.subtract(pi);
            if (deuda.compareTo(BigDecimal.ZERO) < 0) deuda = BigDecimal.ZERO;
            v.setPagoInicial(pi);
            v.setNCuotas(nCuotas != null ? nCuotas : 1);
            v.setIntervaloPago(intervaloPago);
            v.setDeuda(deuda);
            if (deuda.compareTo(BigDecimal.ZERO) > 0) v.setEstado("PENDIENTE");
        } else {
            v.setDeuda(BigDecimal.ZERO);
        }

        // Asignar venta a detalles
        for (DetalleVenta d : detalles) d.setVenta(v);
        v.setDetalles(detalles);

        ventaRepo.save(v);

        // ── Descontar stock de cada producto vendido ────────────────────────
        for (int i = 0; i < productosId.size(); i++) {
            int idx = i; // efectivo-final para lambda
            productoRepo.findById(productosId.get(idx)).ifPresent(prod -> {
                prod.setStock(prod.getStock() - productosCantidad.get(idx));
                productoRepo.save(prod);
            });
        }

        // Generar cuotas si es crédito
        int numCuotas = (nCuotas != null && nCuotas > 0) ? nCuotas : 1;
        if (esCredito && deuda.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal montoCuota = deuda.divide(BigDecimal.valueOf(numCuotas), 2, RoundingMode.HALF_UP);
            LocalDate fecha = LocalDate.now();
            int diasIntervalo = 30;
            if ("SEMANAL".equals(intervaloPago)) diasIntervalo = 7;
            else if ("QUINCENAL".equals(intervaloPago)) diasIntervalo = 15;

            for (int i = 1; i <= numCuotas; i++) {
                fecha = fecha.plusDays(diasIntervalo);
                Cuota c = new Cuota();
                c.setVenta(v);
                c.setNumeroCuota(i);
                c.setFechaVencimiento(fecha);
                c.setMonto(montoCuota);
                c.setEstado("PENDIENTE");
                cuotaRepo.save(c);
            }
        }

        ra.addFlashAttribute("success", "Venta registrada: " + numDoc);
        return "redirect:/ventas";
    }

    // =====================================
    // ANULAR VENTA
    // =====================================
    @PostMapping("/anular/{id}")
    @Transactional
    public String anular(@PathVariable Integer id, RedirectAttributes ra) {
        Venta v = ventaRepo.findById(id).orElse(null);
        if (v == null) {
            ra.addFlashAttribute("error", "Venta no encontrada.");
            return "redirect:/ventas";
        }
        // Solo restaurar stock si la venta tenía stock descontado:
        // - Ventas admin (no PEDIDO-ONLINE) siempre descontaron stock en guardar()
        // - Pedidos online solo si estaban APROBADOS (stock descontado al aprobar)
        boolean restaurarStock = !"PEDIDO-ONLINE".equals(v.getSerie())
                || "APROBADO".equals(v.getAprobacionPedido());

        if (restaurarStock && v.getDetalles() != null) {
            for (DetalleVenta d : v.getDetalles()) {
                if (d.getProductoId() == null) continue;
                productoRepo.findById(d.getProductoId()).ifPresent(p -> {
                    int stockActual = p.getStock() != null ? p.getStock() : 0;
                    p.setStock(stockActual + d.getCantidad());
                    productoRepo.save(p);
                });
            }
        }
        v.setEstado("ANULADA");
        ventaRepo.save(v);
        ra.addFlashAttribute("success", "Venta anulada.");
        return "redirect:/ventas";
    }

    // =====================================
    // APROBAR / RECHAZAR PEDIDO ONLINE
    // =====================================
    @PostMapping("/{id}/aprobar")
    @ResponseBody
    @Transactional
    public Map<String, Object> aprobar(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Venta v = ventaRepo.findById(id).orElseThrow();
            // Validar y descontar stock antes de aprobar
            if (v.getDetalles() != null) {
                for (DetalleVenta d : v.getDetalles()) {
                    if (d.getProductoId() == null) continue;
                    Producto p = productoRepo.findById(d.getProductoId()).orElse(null);
                    if (p == null) {
                        res.put("ok", false);
                        res.put("msg", "Producto \"" + d.getProductoNombre() + "\" ya no existe.");
                        return res;
                    }
                    int stockActual = p.getStock() != null ? p.getStock() : 0;
                    if (d.getCantidad() > stockActual) {
                        res.put("ok", false);
                        res.put("msg", "Stock insuficiente para \"" + p.getNombre() +
                                "\". Disponible: " + stockActual + ", requerido: " + d.getCantidad());
                        return res;
                    }
                }
                for (DetalleVenta d : v.getDetalles()) {
                    if (d.getProductoId() == null) continue;
                    productoRepo.findById(d.getProductoId()).ifPresent(p -> {
                        int stockActual = p.getStock() != null ? p.getStock() : 0;
                        p.setStock(stockActual - d.getCantidad());
                        productoRepo.save(p);
                    });
                }
            }
            v.setAprobacionPedido("APROBADO");
            v.setEstado("COMPLETADA");
            ventaRepo.save(v);
            res.put("ok", true);
        } catch (Exception e) {
            res.put("ok", false);
            res.put("msg", e.getMessage());
        }
        return res;
    }

    @PostMapping("/{id}/rechazar")
    @ResponseBody
    @Transactional
    public Map<String, Object> rechazar(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Venta v = ventaRepo.findById(id).orElseThrow();
            boolean esPedidoOnlineAprobado = "PEDIDO-ONLINE".equals(v.getSerie())
                    && "APROBADO".equals(v.getAprobacionPedido());
            v.setAprobacionPedido("RECHAZADO");
            v.setEstado("ANULADA");
            ventaRepo.save(v);
            // Solo restaurar stock si era un pedido online YA APROBADO
            // (porque el stock solo se descuenta al aprobar)
            // Pedidos PENDIENTE nunca tuvieron stock descontado
            if (esPedidoOnlineAprobado && v.getDetalles() != null) {
                for (DetalleVenta d : v.getDetalles()) {
                    if (d.getProductoId() != null) {
                        productoRepo.findById(d.getProductoId()).ifPresent(p -> {
                            int stockActual = p.getStock() != null ? p.getStock() : 0;
                            p.setStock(stockActual + d.getCantidad());
                            productoRepo.save(p);
                        });
                    }
                }
            }
            res.put("ok", true);
        } catch (Exception e) {
            res.put("ok", false);
            res.put("msg", e.getMessage());
        }
        return res;
    }

    // =====================================
    // DETALLE VENTA (JSON para modal)
    // =====================================
    @GetMapping("/detalle/{id}")
    @ResponseBody
    public Map<String, Object> detalle(@PathVariable Integer id) {
        Venta v = ventaRepo.findById(id).orElseThrow();
        Map<String, Object> map = new HashMap<>();
        map.put("id",              v.getId());
        map.put("numeroDocumento", v.getNumeroDocumento());
        map.put("clienteNombre",   v.getClienteNombre());
        map.put("clienteDocumento",v.getClienteDocumento());
        map.put("vendedor",        v.getVendedor());
        map.put("fecha",           v.getFecha() != null ? v.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        map.put("formaPago",       v.getFormaPago());
        map.put("esCredito",       v.getEsCredito());
        map.put("total",           v.getTotal());
        map.put("deuda",           v.getDeuda() != null ? v.getDeuda() : 0);
        map.put("estado",          v.getEstado());
        map.put("pagoInicial",     v.getPagoInicial() != null ? v.getPagoInicial() : 0);
        map.put("nCuotas",         v.getNCuotas() != null ? v.getNCuotas() : 0);
        map.put("intervaloPago",   v.getIntervaloPago() != null ? v.getIntervaloPago() : "-");

        // Detalles de productos
        List<Map<String, Object>> detallesList = new ArrayList<>();
        if (v.getDetalles() != null) {
            for (DetalleVenta d : v.getDetalles()) {
                Map<String, Object> dm = new HashMap<>();
                dm.put("productoNombre", d.getProductoNombre());
                dm.put("precio",    d.getPrecio());
                dm.put("cantidad",  d.getCantidad());
                dm.put("subtotal",  d.getSubtotal());
                detallesList.add(dm);
            }
        }
        map.put("detalles", detallesList);

        // Cuotas
        List<Map<String, Object>> cuotasList = new ArrayList<>();
        for (Cuota c : cuotaRepo.findByVentaId(id)) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("id",               c.getId());
            cm.put("numeroCuota",      c.getNumeroCuota());
            cm.put("fechaVencimiento", c.getFechaVencimiento().toString());
            cm.put("monto",            c.getMonto());
            cm.put("estado",           c.getEstado());
            cm.put("fechaPago",  c.getFechaPago()  != null ? c.getFechaPago().toString()  : null);
            cm.put("medioPago",  c.getMedioPago()  != null ? c.getMedioPago()             : "-");
            cuotasList.add(cm);
        }
        map.put("cuotas", cuotasList);
        return map;
    }

    // =====================================
    // PAGAR CUOTA
    // =====================================
    @PostMapping("/cuota/pagar")
    @ResponseBody
    public Map<String, Object> pagarCuota(
            @RequestParam Integer cuotaId,
            @RequestParam String medioPago,
            @RequestParam(required = false) String tipoPago,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) String observacion,
            @RequestParam(required = false) BigDecimal montoPagado) {
        Map<String, Object> result = new HashMap<>();
        try {
            Cuota c = cuotaRepo.findById(cuotaId).orElseThrow();

            // Validar que todas las cuotas anteriores estén pagadas
            List<Cuota> todasLasCuotas = cuotaRepo.findByVentaId(c.getVenta().getId());
            boolean hayPendienteAnterior = todasLasCuotas.stream()
                    .anyMatch(cu -> cu.getNumeroCuota() < c.getNumeroCuota()
                                && "PENDIENTE".equals(cu.getEstado()));

            if (hayPendienteAnterior) {
                result.put("success", false);
                result.put("mensaje", "Debes pagar primero la cuota #"
                        + todasLasCuotas.stream()
                            .filter(cu -> "PENDIENTE".equals(cu.getEstado()))
                            .mapToInt(Cuota::getNumeroCuota)
                            .min().orElse(c.getNumeroCuota()));
                return result;
            }

            c.setEstado("PAGADO");
            c.setFechaPago(LocalDate.now());
            c.setMedioPago(medioPago);
            if (referencia  != null && !referencia.isBlank())  c.setReferencia(referencia);
            if (observacion != null && !observacion.isBlank()) c.setObservacion(observacion);
            c.setMontoPagado("MIXTO".equals(tipoPago) && montoPagado != null ? montoPagado : c.getMonto());
            cuotaRepo.save(c);

            // Recalcular deuda de la venta
            Venta v = c.getVenta();
            BigDecimal deudaRestante = cuotaRepo.findByVentaId(v.getId()).stream()
                    .filter(cu -> "PENDIENTE".equals(cu.getEstado()))
                    .map(Cuota::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            v.setDeuda(deudaRestante);
            if (deudaRestante.compareTo(BigDecimal.ZERO) == 0) v.setEstado("COMPLETADA");
            ventaRepo.save(v);

            result.put("success", true);
            result.put("deudaRestante", deudaRestante);
        } catch (Exception e) {
            result.put("success", false);
            result.put("mensaje", e.getMessage());
        }
        return result;
    }

    // =====================================
    // BUSCAR PRODUCTOS (para el select del modal)
    // =====================================
    @GetMapping("/api/productos")
    @ResponseBody
    public List<Map<String, Object>> buscarProductos(@RequestParam(defaultValue = "") String q) {
        List<Map<String, Object>> lista = new ArrayList<>();
        productoRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getEstado()))
                .filter(p -> p.getNombre().toLowerCase().contains(q.toLowerCase()))
                .limit(10)
                .forEach(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("nombre", p.getNombre());
                    m.put("precio", p.getPrecio());
                    m.put("stock", p.getStock());
                    lista.add(m);
                });
        return lista;
    }

    // =====================================
    // CONSULTAR DNI/RUC (reutiliza lógica)
    // =====================================
    @GetMapping("/api/consultar")
    @ResponseBody
    public Map<String, Object> consultarDocumento(@RequestParam String documento) {
        Map<String, Object> result = new HashMap<>();

        // 1️⃣ Buscar primero en la base de datos local (tabla clientes)
        var clienteLocal = clienteRepo.findFirstByDocumento(documento);
        if (clienteLocal.isPresent()) {
            result.put("success", true);
            result.put("nombre", clienteLocal.get().getNombre());
            result.put("fuente", "BD");
            return result;
        }

        // 2️⃣ No está en BD → consultar API externa
        try {
            String url;
            if (documento.length() == 8) {
                url = "https://miapi.cloud/v1/dni/" + documento;
            } else if (documento.length() == 11) {
                url = "https://miapi.cloud/v1/ruc/" + documento;
            } else {
                result.put("success", false);
                result.put("mensaje", "Ingresa 8 dígitos (DNI) o 11 dígitos (RUC)");
                return result;
            }

            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiToken)
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            System.out.println("VENTAS API [" + response.statusCode() + "]: " + body);

            String nombre = ClienteController.extraerNombreCompleto(body);

            if (nombre != null && !nombre.isEmpty()) {
                result.put("success", true);
                result.put("nombre", nombre);
            } else {
                result.put("success", false);
                result.put("mensaje", "No se encontró datos (código " + response.statusCode() + ")");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("mensaje", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    // Próximo número de documento
    @GetMapping("/api/siguiente-numero")
    @ResponseBody
    public Map<String, Object> siguienteNumero(@RequestParam(defaultValue = "NOTA DE VENTA (N001)") String serie) {
        long count = ventaRepo.countBySerie(serie) + 1;
        Map<String, Object> m = new HashMap<>();
        m.put("numero", String.format("%08d", count));
        return m;
    }
}
