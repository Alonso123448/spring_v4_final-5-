package com.example.spring_v4.controller;

import com.example.spring_v4.entity.DetalleVenta;
import com.example.spring_v4.entity.Venta;
import com.example.spring_v4.entity.Producto;
import com.example.spring_v4.repository.ProductoRepository;
import com.example.spring_v4.repository.VentaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/verificacion-pagos")
public class VerificacionPagosController {

    private final VentaRepository    ventaRepo;
    private final ProductoRepository productoRepo;

    public VerificacionPagosController(VentaRepository ventaRepo, ProductoRepository productoRepo) {
        this.ventaRepo    = ventaRepo;
        this.productoRepo = productoRepo;
    }

    @GetMapping
    public String listar(Model model) {
        List<Venta> pedidos = ventaRepo.findBySerieOrderByFechaDesc("PEDIDO-ONLINE");
        long pendientes = pedidos.stream().filter(p -> "PENDIENTE".equals(p.getAprobacionPedido())).count();
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("pendientes", pendientes);
        return "verificacion-pagos";
    }

    private static final String VOUCHER_ADMIN_DIR = "src/main/resources/static/imagenes/vouchers/";
    private static final Set<String> EXTENSIONES_IMAGEN = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @PostMapping("/aprobar/{id}")
    @ResponseBody
    @Transactional
    public Map<String, Object> aprobar(@PathVariable Integer id,
                                        @RequestParam(required = false) MultipartFile voucherAdmin,
                                        @RequestParam(required = false, defaultValue = "") String notasAdmin) {
        Map<String, Object> res = new HashMap<>();
        Venta v = ventaRepo.findById(id).orElse(null);
        if (v == null) { res.put("ok", false); res.put("msg", "Pedido no encontrado"); return res; }
        if (voucherAdmin != null && !voucherAdmin.isEmpty()) {
            String origName = voucherAdmin.getOriginalFilename() != null ? voucherAdmin.getOriginalFilename().toLowerCase() : "";
            String ext = origName.contains(".") ? origName.substring(origName.lastIndexOf('.')) : "";
            if (!EXTENSIONES_IMAGEN.contains(ext)) {
                res.put("ok", false); res.put("msg", "El voucher debe ser una imagen (jpg, png, gif, webp)."); return res;
            }
        }
        if (v.getDetalles() != null) {
            for (DetalleVenta d : v.getDetalles()) {
                if (d.getProductoId() == null) continue;
                Producto p = productoRepo.findById(d.getProductoId()).orElse(null);
                if (p == null) {
                    res.put("ok", false);
                    res.put("msg", "Producto \"" + d.getProductoNombre() + "\" ya no existe en el sistema.");
                    return res;
                }
                int stockActual = p.getStock() != null ? p.getStock() : 0;
                if (d.getCantidad() > stockActual) {
                    res.put("ok", false);
                    res.put("msg", "Stock insuficiente para \"" + p.getNombre() +
                            "\". Disponible: " + stockActual +
                            ", requerido: " + d.getCantidad() +
                            ". No se puede aprobar el pedido.");
                    return res;
                }
            }
        }

        if (v.getDetalles() != null) {
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
        if (!notasAdmin.isBlank()) v.setNotasAdmin(notasAdmin.trim());
        if (voucherAdmin != null && !voucherAdmin.isEmpty()) {
            try {
                Files.createDirectories(Paths.get(VOUCHER_ADMIN_DIR));
                String origName = voucherAdmin.getOriginalFilename() != null ? voucherAdmin.getOriginalFilename().toLowerCase() : "";
                String ext = origName.contains(".") ? origName.substring(origName.lastIndexOf('.')) : ".jpg";
                String nombre = "admin_voucher_" + v.getId() + ext;
                Files.write(Paths.get(VOUCHER_ADMIN_DIR + nombre), voucherAdmin.getBytes());
                v.setRutaVoucherAdmin("/imagenes/vouchers/" + nombre);
            } catch (Exception e) { e.printStackTrace(); }
        }
        ventaRepo.save(v);
        res.put("ok", true);
        return res;
    }

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public Map<String, Object> detalle(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        Venta v = ventaRepo.findById(id).orElse(null);
        if (v == null) { res.put("ok", false); return res; }
        res.put("ok", true);
        res.put("numero", v.getNumeroDocumento());
        res.put("clienteNombre", v.getClienteNombre());
        res.put("clienteDocumento", v.getClienteDocumento());
        res.put("clienteTelefono", v.getClienteTelefono() != null ? v.getClienteTelefono() : "");
        res.put("clienteCorreo", v.getClienteCorreo() != null ? v.getClienteCorreo() : "");
        res.put("tipoEntrega", v.getTipoEntrega() != null ? v.getTipoEntrega() : "");
        res.put("direccionEntrega", v.getDireccionEntrega() != null ? v.getDireccionEntrega() : "");
        res.put("formaPago", v.getFormaPago());
        res.put("codigoOperacion", v.getCodigoOperacion() != null ? v.getCodigoOperacion() : "");
        res.put("total", v.getTotal());
        res.put("estado", v.getAprobacionPedido());
        res.put("rutaVoucher", v.getRutaVoucher() != null ? v.getRutaVoucher() : "");
        res.put("rutaVoucherAdmin", v.getRutaVoucherAdmin() != null ? v.getRutaVoucherAdmin() : "");
        res.put("notasAdmin", v.getNotasAdmin() != null ? v.getNotasAdmin() : "");
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        if (v.getDetalles() != null) {
            for (var d : v.getDetalles()) {
                Map<String, Object> item = new HashMap<>();
                item.put("nombre", d.getProductoNombre());
                item.put("cantidad", d.getCantidad());
                item.put("precio", d.getPrecio());
                item.put("subtotal", d.getSubtotal());
                String imgRuta = "";
                Producto prodImg = null;
                if (d.getProductoId() != null) {
                    prodImg = productoRepo.findById(d.getProductoId()).orElse(null);
                }
                if (prodImg == null && d.getProductoNombre() != null) {
                    List<Producto> matches = productoRepo.findByNombreIgnoreCase(d.getProductoNombre().trim());
                    if (!matches.isEmpty()) prodImg = matches.get(0);
                }
                if (prodImg != null && prodImg.getImagenes() != null && !prodImg.getImagenes().isEmpty()) {
                    imgRuta = prodImg.getImagenes().get(0).getRuta();
                }
                item.put("imagen", imgRuta);
                items.add(item);
            }
        }
        res.put("detalles", items);
        return res;
    }

    @GetMapping("/pendientes-count")
    @ResponseBody
    public Map<String, Object> pendientesCount() {
        List<Venta> pedidos = ventaRepo.findBySerieOrderByFechaDesc("PEDIDO-ONLINE");
        long count = pedidos.stream().filter(p -> "PENDIENTE".equals(p.getAprobacionPedido())).count();
        return Map.of("count", count);
    }

    @PostMapping("/rechazar/{id}")
    @ResponseBody
    @Transactional
    public Map<String, Object> rechazar(@PathVariable Integer id,
                                         @RequestParam(required = false, defaultValue = "") String motivo) {
        Map<String, Object> res = new HashMap<>();
        Venta v = ventaRepo.findById(id).orElse(null);
        if (v == null) { res.put("ok", false); res.put("msg", "Pedido no encontrado"); return res; }
        
        v.setAprobacionPedido("RECHAZADO");
        v.setEstado("ANULADA");
        ventaRepo.save(v);
        res.put("ok", true);
        return res;
    }
}
