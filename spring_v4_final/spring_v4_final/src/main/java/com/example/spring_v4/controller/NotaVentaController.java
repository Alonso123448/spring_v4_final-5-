package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Cliente;
import com.example.spring_v4.entity.Venta;
import com.example.spring_v4.repository.ClienteRepository;
import com.example.spring_v4.repository.LogoImagenRepository;
import com.example.spring_v4.repository.VentaRepository;
import com.example.spring_v4.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notas-venta")
public class NotaVentaController {

    private final VentaRepository ventaRepo;
    private final ClienteRepository clienteRepo;
    private final LogoImagenRepository logoRepo;
    private final EmailService emailService;

    @Value("${app.nombre:Inventario}")
    private String appNombre;

    @Value("${app.rubro:Sistema de Gestión}")
    private String appRubro;

    public NotaVentaController(VentaRepository ventaRepo,
                               ClienteRepository clienteRepo,
                               LogoImagenRepository logoRepo,
                               EmailService emailService) {
        this.ventaRepo    = ventaRepo;
        this.clienteRepo  = clienteRepo;
        this.logoRepo     = logoRepo;
        this.emailService = emailService;
    }

    
    @GetMapping({"", "/"})
    public String listar(Model model) {

        List<Venta> ventas = ventaRepo.findAll();
        ventas.sort((a, b) -> Integer.compare(b.getId(), a.getId())); 

        Map<String, Cliente> clienteMap = new HashMap<>();
        for (Venta v : ventas) {
            String doc = v.getClienteDocumento();
            if (doc != null && !clienteMap.containsKey(doc)) {
                clienteRepo.findFirstByDocumento(doc)
                           .ifPresent(c -> clienteMap.put(doc, c));
            }
        }

        model.addAttribute("ventas", ventas);
        model.addAttribute("clienteMap", clienteMap);
        model.addAttribute("appNombre", appNombre);
        model.addAttribute("appRubro", appRubro);
        logoRepo.findByActivoTrue().stream().findFirst()
                .ifPresent(l -> model.addAttribute("logoActivo", l));
        return "notas-venta";
    }

  
    @GetMapping("/{id}/imprimir")
    public String imprimir(@PathVariable Integer id, Model model) {

        Venta venta = ventaRepo.findById(id).orElseThrow();
        model.addAttribute("venta", venta);
        model.addAttribute("appNombre", appNombre);
        model.addAttribute("appRubro", appRubro);

        if (venta.getClienteDocumento() != null) {
            clienteRepo.findFirstByDocumento(venta.getClienteDocumento())
                       .ifPresent(c -> model.addAttribute("cliente", c));
        }

        logoRepo.findByActivoTrue().stream().findFirst()
                .ifPresent(l -> model.addAttribute("logoActivo", l));
        return "nota-venta-imprimir";
    }

   
    @PostMapping("/{id}/enviar-correo")
    @ResponseBody
    public Map<String, Object> enviarCorreo(
            @PathVariable Integer id,
            @RequestParam String correo) {

        Map<String, Object> res = new HashMap<>();
        try {
            Venta venta = ventaRepo.findById(id).orElseThrow();

            if (correo == null || !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                res.put("ok", false);
                res.put("msg", "Correo electrónico no válido.");
                return res;
            }

            emailService.enviarComprobante(correo, venta);
            res.put("ok", true);
            res.put("msg", "Comprobante enviado a " + correo);

        } catch (Exception e) {
            res.put("ok", false);
            res.put("msg", "Error al enviar: " + e.getMessage());
        }
        return res;
    }
}
