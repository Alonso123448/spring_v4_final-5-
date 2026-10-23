package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Proveedor;
import com.example.spring_v4.repository.ProductoRepository;
import com.example.spring_v4.repository.ProveedorRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    public ProveedorController(
            ProveedorRepository proveedorRepository,
            ProductoRepository productoRepository) {

        this.proveedorRepository = proveedorRepository;
        this.productoRepository  = productoRepository;
    }

 
    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "proveedores",
                proveedorRepository.findAll());

        return "Proveedores/proveedores";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        model.addAttribute(
                "proveedor",
                new Proveedor());

        return "Proveedores/form-proveedor";
    }

   
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Proveedor proveedor,
            RedirectAttributes ra) {

        // Validar nombre obligatorio
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            ra.addFlashAttribute("error", "El nombre del proveedor es obligatorio.");
            return "redirect:/proveedores";
        }

        String nombreTrim = proveedor.getNombre().trim();

        if (proveedor.getId() == null) {
            if (proveedorRepository.existsByNombreIgnoreCase(nombreTrim)) {
                ra.addFlashAttribute("error",
                    "Ya existe un proveedor con el nombre '" + nombreTrim + "'. Elige un nombre diferente.");
                return "redirect:/proveedores";
            }
        } else {
            if (proveedorRepository.existsByNombreIgnoreCaseAndIdNot(nombreTrim, proveedor.getId())) {
                ra.addFlashAttribute("error",
                    "Ya existe otro proveedor con el nombre '" + nombreTrim + "'. Elige un nombre diferente.");
                return "redirect:/proveedores";
            }
        }

        try {
            Proveedor guardar;

            if (proveedor.getId() != null) {
                guardar = proveedorRepository.findById(proveedor.getId()).orElseThrow();
            } else {
                guardar = new Proveedor();
            }

            guardar.setNombre(nombreTrim);
            guardar.setTelefono(proveedor.getTelefono());
            guardar.setDireccion(proveedor.getDireccion());
            guardar.setEmail(proveedor.getEmail());
            guardar.setEstado(proveedor.getEstado());

            proveedorRepository.save(guardar);
            ra.addFlashAttribute("success", "Proveedor '" + nombreTrim + "' guardado correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Ocurrió un error al guardar el proveedor.");
        }

        return "redirect:/proveedores";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            Model model) {

        Proveedor proveedor =
                proveedorRepository
                        .findById(id)
                        .orElseThrow();

        model.addAttribute(
                "proveedor",
                proveedor);

        return "Proveedores/form-proveedor";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Integer id,
            RedirectAttributes ra) {

        Proveedor proveedor = proveedorRepository.findById(id).orElseThrow();

        if (productoRepository.existsByProveedorId(id)) {
            ra.addFlashAttribute("error",
                "El proveedor '" + proveedor.getNombre() + "' está en uso por uno o más productos y no puede ser eliminado.");
            return "redirect:/proveedores";
        }

        try {
            proveedorRepository.deleteById(id);
            ra.addFlashAttribute("success", "Proveedor eliminado correctamente.");
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "No se pudo eliminar el proveedor '" + proveedor.getNombre() + "'.");
        }

        return "redirect:/proveedores";
    }
}
