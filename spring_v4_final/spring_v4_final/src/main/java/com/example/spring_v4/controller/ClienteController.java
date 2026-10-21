package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Cliente;
import com.example.spring_v4.repository.ClienteRepository;
import com.example.spring_v4.repository.LogoImagenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepo;
    private final LogoImagenRepository logoRepo;

    @Value("${miapi.token}")
    private String apiToken;

    public ClienteController(ClienteRepository clienteRepo, LogoImagenRepository logoRepo) {
        this.clienteRepo = clienteRepo;
        this.logoRepo = logoRepo;
    }

    // =====================================
    // LISTADO
    // =====================================
    @GetMapping({"", "/"})
    public String listar(Model model) {
        model.addAttribute("clientes", clienteRepo.findAll());
        logoRepo.findByActivoTrue().stream().findFirst()
                .ifPresent(l -> model.addAttribute("logoActivo", l));
        return "clientes";
    }

    // =====================================
    // GUARDAR
    // =====================================
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Cliente cliente, RedirectAttributes ra) {
        if (cliente.getTelefono() == null || cliente.getTelefono().trim().isEmpty()) {
            ra.addFlashAttribute("error", "El teléfono del cliente es obligatorio.");
            return "redirect:/clientes";
        }
        if (cliente.getDocumento() == null || cliente.getDocumento().trim().isEmpty()) {
            ra.addFlashAttribute("error", "El documento del cliente es obligatorio.");
            return "redirect:/clientes";
        }
        // Evitar duplicados: si es nuevo (id null) y ya existe el documento
        boolean esNuevo = cliente.getId() == null;
        if (esNuevo && clienteRepo.existsByDocumento(cliente.getDocumento().trim())) {
            ra.addFlashAttribute("error", "Ya existe un cliente con el documento " + cliente.getDocumento().trim() + ".");
            return "redirect:/clientes";
        }
        clienteRepo.save(cliente);
        ra.addFlashAttribute("success", "Cliente guardado exitosamente.");
        return "redirect:/clientes";
    }

    // =====================================
    // EDITAR - cargar datos
    // =====================================
    @GetMapping("/editar/{id}")
    @ResponseBody
    public Cliente editar(@PathVariable Integer id) {
        return clienteRepo.findById(id).orElseThrow();
    }

    // =====================================
    // ELIMINAR
    // =====================================
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            clienteRepo.deleteById(id);
            ra.addFlashAttribute("success", "Cliente eliminado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar el cliente porque tiene datos asociados.");
        }
        return "redirect:/clientes";
    }

    // =====================================
    // TOGGLE ESTADO
    // =====================================
    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Integer id) {
        Cliente c = clienteRepo.findById(id).orElseThrow();
        c.setEstado(!c.getEstado());
        clienteRepo.save(c);
        return "redirect:/clientes";
    }

    // =====================================
    // API CONSULTAR DNI / RUC
    // =====================================
    @GetMapping("/api/consultar")
    @ResponseBody
    public Map<String, Object> consultarDocumento(@RequestParam String documento) {
        Map<String, Object> result = new HashMap<>();

        // 1️⃣ Buscar primero en la base de datos local
        var clienteLocal = clienteRepo.findFirstByDocumento(documento);
        if (clienteLocal.isPresent()) {
            result.put("success", true);
            result.put("nombre", clienteLocal.get().getNombre());
            result.put("tipo", clienteLocal.get().getTipoDocumento() != null ? clienteLocal.get().getTipoDocumento() : (documento.length() == 8 ? "DNI" : "RUC"));
            result.put("fuente", "BD");
            return result;
        }

        // 2️⃣ No está en BD → consultar API externa
        try {
            String url;
            String tipo;
            if (documento.length() == 8) {
                url = "https://miapi.cloud/v1/dni/" + documento;
                tipo = "DNI";
            } else if (documento.length() == 11) {
                url = "https://miapi.cloud/v1/ruc/" + documento;
                tipo = "RUC";
            } else {
                result.put("success", false);
                result.put("mensaje", "Ingresa 8 dígitos (DNI) o 11 dígitos (RUC)");
                return result;
            }

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiToken)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();
            System.out.println("MIAPICLOUD RESPONSE [" + response.statusCode() + "]: " + body);

            String nombre = extraerNombreCompleto(body);

            if (nombre != null && !nombre.isEmpty()) {
                result.put("success", true);
                result.put("nombre", nombre);
                result.put("tipo", tipo);
            } else {
                result.put("success", false);
                result.put("mensaje", "No se encontró datos. Código: " + response.statusCode());
                result.put("raw", body);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("mensaje", "Error: " + e.getClass().getSimpleName() + " - " + (e.getMessage() != null ? e.getMessage() : "sin detalle"));
        }
        return result;
    }

    public static String extraerCampo(String json, String campo) {
        String key = "\"" + campo + "\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        String val = json.substring(start, end);
        return val.isBlank() ? null : val;
    }

    /**
     * Intenta extraer el nombre completo del JSON de la API.
     * Prueba múltiples campos y construye desde partes si es necesario.
     * Formato final: NOMBRES APELLIDO_PATERNO APELLIDO_MATERNO
     */
    public static String extraerNombreCompleto(String json) {
        // 1. Campo directo de nombre completo
        String nombre = extraerCampo(json, "nombre_completo");
        if (nombre == null) nombre = extraerCampo(json, "nombreCompleto");
        if (nombre == null) nombre = extraerCampo(json, "razonSocial");
        if (nombre == null) nombre = extraerCampo(json, "razon_social");
        if (nombre == null) nombre = extraerCampo(json, "nombre");

        if (nombre != null && !nombre.isBlank()) return nombre.trim();

        // 2. Construir desde partes individuales
        String nombres = extraerCampo(json, "nombres");
        String apePat  = extraerCampo(json, "ape_paterno");       // miapi.cloud
        if (apePat == null) apePat = extraerCampo(json, "apellidoPaterno");
        if (apePat == null) apePat = extraerCampo(json, "apellido_paterno");
        String apeMat  = extraerCampo(json, "ape_materno");       // miapi.cloud
        if (apeMat == null) apeMat = extraerCampo(json, "apellidoMaterno");
        if (apeMat == null) apeMat = extraerCampo(json, "apellido_materno");

        if (nombres != null || apePat != null) {
            // Formato: NOMBRES APELLIDO_PAT APELLIDO_MAT
            return ((nombres  != null ? nombres  + " " : "")
                  + (apePat   != null ? apePat   + " " : "")
                  + (apeMat   != null ? apeMat         : "")).trim();
        }
        return null;
    }
}
