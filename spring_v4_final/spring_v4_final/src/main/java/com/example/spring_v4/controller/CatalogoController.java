package com.example.spring_v4.controller;

import com.example.spring_v4.entity.*;
import com.example.spring_v4.repository.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/catalogo")
public class CatalogoController {

    @Value("${app.yape.numero:}")        private String yapeNumero;
    @Value("${app.yape.nombre:Tienda}")  private String yapeNombre;
    @Value("${app.yape.qr:/imagenes/yape-qr.png}") private String yapeQr;
    @Value("${app.transferencia.banco:}")  private String transBanco;
    @Value("${app.transferencia.cuenta:}") private String transCuenta;
    @Value("${app.transferencia.cci:}")    private String transCci;

    private final ProductoRepository    productoRepository;
    private final CategoriaRepository   categoriaRepository;
    private final SliderImagenRepository sliderRepo;
    private final LogoImagenRepository  logoRepo;
    private final VentaRepository       ventaRepo;

    public CatalogoController(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               SliderImagenRepository sliderRepo,
                               LogoImagenRepository logoRepo,
                               VentaRepository ventaRepo) {
        this.productoRepository  = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.sliderRepo          = sliderRepo;
        this.logoRepo            = logoRepo;
        this.ventaRepo           = ventaRepo;
    }

    // ─── CATÁLOGO PÚBLICO ───────────────────────────────────────────────────
    @GetMapping({"", "/"})
    public String catalogo(Model model) {
        List<Producto>     productos  = productoRepository.findByEstadoTrue();
        List<Categoria>    categorias = categoriaRepository.findByEstadoTrue();
        List<SliderImagen> sliders    = sliderRepo.findByActivoTrue();
        List<LogoImagen>   logos      = logoRepo.findByActivoTrue();

        model.addAttribute("productos",   productos);
        model.addAttribute("categorias",  categorias);
        model.addAttribute("sliders",     sliders);
        model.addAttribute("logoActivo",  logos.isEmpty() ? null : logos.get(0));
        model.addAttribute("yapeNumero",  yapeNumero);
        model.addAttribute("yapeNombre",  yapeNombre);
        model.addAttribute("yapeQr",      yapeQr);
        model.addAttribute("transBanco",  transBanco);
        model.addAttribute("transCuenta", transCuenta);
        model.addAttribute("transCci",    transCci);
        return "catalogo";
    }

    // ─── VERIFICAR UNICIDAD DE CÓDIGO DE OPERACIÓN ───────────────────────────
    @GetMapping("/verificar-codigo")
    @ResponseBody
    public Map<String, Object> verificarCodigo(@RequestParam String codigo) {
        Map<String, Object> res = new HashMap<>();
        boolean disponible = !ventaRepo.existsByCodigoOperacion(codigo.trim());
        res.put("disponible", disponible);
        return res;
    }

    // ─── BÚSQUEDA DE CLIENTES (autocompletar DNI) ──────────────────────────
    @GetMapping("/clientes/buscar")
    @ResponseBody
    public List<Map<String, String>> buscarClientes(@RequestParam(defaultValue = "") String q) {
        if (q.trim().length() < 2) return List.of();
        List<Venta> ventas = ventaRepo.findTop10ByClienteDocumentoStartingWith(q.trim());
        // Deduplicar por documento
        LinkedHashMap<String, String> seen = new LinkedHashMap<>();
        for (Venta v : ventas) {
            if (v.getClienteDocumento() != null && !seen.containsKey(v.getClienteDocumento())) {
                seen.put(v.getClienteDocumento(),
                         v.getClienteNombre() != null ? v.getClienteNombre() : "");
            }
        }
        return seen.entrySet().stream()
                .map(e -> Map.of("doc", e.getKey(), "nombre", e.getValue()))
                .collect(Collectors.toList());
    }

    // ─── PEDIDO ONLINE (simulación de pago) ────────────────────────────────
    private static final String VOUCHER_DIR = "src/main/resources/static/imagenes/vouchers/";

    private static final Set<String> EXTENSIONES_IMAGEN = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @PostMapping("/pedido")
    @ResponseBody
    @Transactional
    public Map<String, Object> crearPedido(
            @RequestParam String clienteNombre,
            @RequestParam String clienteDocumento,
            @RequestParam(required = false, defaultValue = "") String clienteTelefono,
            @RequestParam(required = false, defaultValue = "") String clienteCorreo,
            @RequestParam(required = false, defaultValue = "CONTADO") String tipoCompra,
            @RequestParam String metodoPago,
            @RequestParam(required = false, defaultValue = "DOMICILIO") String tipoEntrega,
            @RequestParam(required = false, defaultValue = "") String direccionEntrega,
            @RequestParam(required = false, defaultValue = "") String codigoOperacion,
            @RequestParam(required = false) MultipartFile voucher,
            @RequestParam List<Integer> productosId,
            @RequestParam List<String>  productosNombre,
            @RequestParam List<Integer> productosCantidad) {

        Map<String, Object> res = new HashMap<>();

        // ── Validar archivo voucher ────────────────────────────────────────
        if (voucher != null && !voucher.isEmpty()) {
            String origName = voucher.getOriginalFilename() != null ? voucher.getOriginalFilename().toLowerCase() : "";
            String ext = origName.contains(".") ? origName.substring(origName.lastIndexOf('.')) : "";
            if (!EXTENSIONES_IMAGEN.contains(ext)) {
                res.put("ok", false);
                res.put("msg", "El voucher debe ser una imagen (jpg, png, gif, webp).");
                return res;
            }
        }

        // ── Validar stock y obtener precios reales desde BD ────────────────
        List<Producto> productosDB = new ArrayList<>();
        for (int i = 0; i < productosId.size(); i++) {
            Producto p = productoRepository.findById(productosId.get(i)).orElse(null);
            if (p == null) {
                res.put("ok", false);
                res.put("msg", "Producto no encontrado: " + productosNombre.get(i));
                return res;
            }
            int stockDisp = p.getStock() != null ? p.getStock() : 0;
            if (productosCantidad.get(i) > stockDisp) {
                res.put("ok", false);
                res.put("msg", "Stock insuficiente para \"" + p.getNombre() +
                        "\". Disponible: " + stockDisp +
                        ", solicitado: " + productosCantidad.get(i));
                return res;
            }
            productosDB.add(p);
        }

        // ── Calcular total con precios reales de BD (nunca del frontend) ───
        BigDecimal total = BigDecimal.ZERO;
        List<DetalleVenta> detalles = new ArrayList<>();
        for (int i = 0; i < productosDB.size(); i++) {
            Producto p = productosDB.get(i);
            BigDecimal precioReal = p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO;
            DetalleVenta d = new DetalleVenta();
            d.setProductoNombre(p.getNombre());
            d.setProductoId(p.getId());
            d.setPrecio(precioReal);
            d.setCantidad(productosCantidad.get(i));
            BigDecimal sub = precioReal.multiply(BigDecimal.valueOf(productosCantidad.get(i)));
            d.setSubtotal(sub);
            total = total.add(sub);
            detalles.add(d);
        }

        // ── Número de pedido — sincronizado para evitar duplicados ────────
        String serie = "PEDIDO-ONLINE";
        String numDoc;
        synchronized (CatalogoController.class) {
            long count = ventaRepo.countBySerie(serie) + 1;
            numDoc = String.format("PED-%08d", count);
        }

        // ── Crear venta ────────────────────────────────────────────────────
        Venta v = new Venta();
        v.setNumeroDocumento(numDoc);
        v.setSerie(serie);
        v.setClienteNombre(clienteNombre);
        v.setClienteDocumento(clienteDocumento);
        v.setVendedor("Tienda Online");
        v.setFecha(LocalDateTime.now());
        v.setTotal(total);
        v.setFormaPago(metodoPago);
        v.setEsCredito("CREDITO".equalsIgnoreCase(tipoCompra));
        v.setClienteTelefono(clienteTelefono);
        v.setClienteCorreo(clienteCorreo);
        v.setTipoEntrega(tipoEntrega);
        v.setDireccionEntrega(direccionEntrega);
        if (!codigoOperacion.isBlank()) v.setCodigoOperacion(codigoOperacion.trim());
        v.setDeuda(BigDecimal.ZERO);
        v.setEstado("PENDIENTE");

        // Todos los pedidos online quedan PENDIENTE de aprobación
        v.setAprobacionPedido("PENDIENTE");

        for (DetalleVenta d : detalles) d.setVenta(v);
        v.setDetalles(detalles);
        ventaRepo.save(v);

        // Guardar voucher si fue enviado
        if (voucher != null && !voucher.isEmpty()) {
            try {
                Files.createDirectories(Paths.get(VOUCHER_DIR));
                String origName = voucher.getOriginalFilename() != null ? voucher.getOriginalFilename().toLowerCase() : "";
                String ext = origName.contains(".") ? origName.substring(origName.lastIndexOf('.')) : ".jpg";
                String nombreArchivo = "voucher_" + v.getId() + ext;
                Files.write(Paths.get(VOUCHER_DIR + nombreArchivo), voucher.getBytes());
                v.setRutaVoucher("/imagenes/vouchers/" + nombreArchivo);
                ventaRepo.save(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // El stock se descuenta solo cuando el admin aprueba el pedido
        // No se toca el stock aquí

        res.put("ok",     true);
        res.put("pedido", numDoc);
        res.put("total",  total);
        return res;
    }
}
