package com.example.spring_v4.controller;

import com.example.spring_v4.entity.MovimientoInventario;
import com.example.spring_v4.entity.Producto;
import com.example.spring_v4.repository.MovimientoInventarioRepository;
import com.example.spring_v4.repository.ProductoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/movimientos")
public class MovimientoInventarioController {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;

    public MovimientoInventarioController(
            MovimientoInventarioRepository movimientoRepository,
            ProductoRepository productoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository   = productoRepository;
    }

   
    @GetMapping
    public String listar(Model model) {
        List<MovimientoInventario> movimientos = movimientoRepository.findAll();
        movimientos.sort((a, b) -> b.getId().compareTo(a.getId())); // más recientes primero
        model.addAttribute("movimientos", movimientos);
        model.addAttribute("productos", productoRepository.findAll());
        return "movimientos";
    }

   
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute MovimientoInventario movimiento,
            Authentication auth,
            RedirectAttributes ra) {

        Integer cantidad = movimiento.getCantidad();
        if (cantidad == null || cantidad <= 0) {
            ra.addFlashAttribute("error", "La cantidad debe ser mayor a 0.");
            return "redirect:/movimientos";
        }

        Producto producto;
        try {
            producto = productoRepository
                    .findById(movimiento.getProducto().getId())
                    .orElseThrow();
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Producto no encontrado.");
            return "redirect:/movimientos";
        }

        int stockActual = producto.getStock() != null ? producto.getStock() : 0;

        String tipo = movimiento.getTipo();
        if (tipo == null || (!tipo.equals("ENTRADA") && !tipo.equals("SALIDA"))) {
            ra.addFlashAttribute("error", "El tipo de movimiento no es válido.");
            return "redirect:/movimientos";
        }

        if (tipo.equals("ENTRADA")) {
            producto.setStock(stockActual + cantidad);

        } else { // SALIDA
            if (cantidad > stockActual) {
                ra.addFlashAttribute("error",
                        "Stock insuficiente. Stock actual de '" + producto.getNombre()
                        + "': " + stockActual + " unidades. Intentaste sacar: " + cantidad + ".");
                return "redirect:/movimientos";
            }
            producto.setStock(stockActual - cantidad);
        }

        productoRepository.save(producto);

        movimiento.setProducto(producto);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setUsuario(auth != null ? auth.getName() : "Sistema");

        movimientoRepository.save(movimiento);

        ra.addFlashAttribute("success",
                "Movimiento registrado correctamente. Stock actualizado de '"
                + producto.getNombre() + "': " + producto.getStock() + " unidades.");

        return "redirect:/movimientos";
    }

}
