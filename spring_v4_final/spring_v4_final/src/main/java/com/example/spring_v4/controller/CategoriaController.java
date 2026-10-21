package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Categoria;
import com.example.spring_v4.repository.CategoriaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    // =========================
    // LISTAR
    // =========================

    @GetMapping({"", "/"})
    public String listar(Model model) {

        model.addAttribute(
                "categorias",
                categoriaRepository.findAll());

        return "Categorias/categorias";
    }

    // =========================
    // GUARDAR / EDITAR
    // =========================

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Categoria categoria, RedirectAttributes ra) {

        // Validar nombre obligatorio
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            ra.addFlashAttribute("error", "El nombre de la categoría es obligatorio.");
            return "redirect:/categorias";
        }

        String nombreTrim = categoria.getNombre().trim();
        boolean editando = categoria.getId() != null;

        // Validar unicidad de nombre
        if (!editando) {
            if (categoriaRepository.existsByNombreIgnoreCase(nombreTrim)) {
                ra.addFlashAttribute("error",
                    "Ya existe una categoría con el nombre '" + nombreTrim + "'. Elige un nombre diferente.");
                return "redirect:/categorias";
            }
        } else {
            if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombreTrim, categoria.getId())) {
                ra.addFlashAttribute("error",
                    "Ya existe otra categoría con el nombre '" + nombreTrim + "'. Elige un nombre diferente.");
                return "redirect:/categorias";
            }
        }

        try {
            Categoria guardar;

            if (editando) {
                guardar = categoriaRepository.findById(categoria.getId()).orElseThrow();
            } else {
                guardar = new Categoria();
            }

            guardar.setNombre(nombreTrim);
            guardar.setDescripcion(categoria.getDescripcion());
            guardar.setEstado(categoria.getEstado() != null ? categoria.getEstado() : true);

            categoriaRepository.save(guardar);

            ra.addFlashAttribute("success", editando ? "Categoría editada correctamente." : "Categoría guardada correctamente.");
            return editando ? "redirect:/categorias?success=editado" : "redirect:/categorias?success=guardado";

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Ocurrió un error al guardar la categoría.");
            return "redirect:/categorias";
        }
    }

    // =========================
    // ELIMINAR
    // =========================

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {

        try {

            categoriaRepository.deleteById(id);

            return "redirect:/categorias?success=eliminado";

        } catch (Exception e) {

            e.printStackTrace();

            return "redirect:/categorias?error=relacion";
        }
    }

    // =========================
    // BUSCAR
    // =========================

    @GetMapping("/buscar/{id}")
    @ResponseBody
    public Map<String, Object> buscar(@PathVariable Integer id) {

        Categoria c = categoriaRepository
                .findById(id)
                .orElseThrow();

        Map<String, Object> data = new HashMap<>();

        data.put("id", c.getId());
        data.put("nombre", c.getNombre());
        data.put("descripcion", c.getDescripcion());
        data.put("estado", c.getEstado());

        return data;
    }
}