package com.example.spring_v4.controller;

import com.example.spring_v4.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class HomeController {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private MovimientoInventarioRepository movimientoRepository;
    @Autowired private LogoImagenRepository logoImagenRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private ClienteRepository clienteRepository;

    @GetMapping("/")
    public String inicio(Model model) {
        
        model.addAttribute("totalProductos",   productoRepository.count());
        model.addAttribute("totalCategorias",  categoriaRepository.count());
        model.addAttribute("totalProveedores", proveedorRepository.count());
        model.addAttribute("totalUsuarios",    usuarioRepository.count());
        model.addAttribute("totalRoles",       rolRepository.count());
        model.addAttribute("totalMovimientos", movimientoRepository.count());

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia    = hoy.plusDays(1).atStartOfDay().minusNanos(1);
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes    = hoy.plusMonths(1).withDayOfMonth(1).atStartOfDay().minusNanos(1);

        BigDecimal ventasHoy = ventaRepository.sumTotalEntre(inicioDia, finDia);
        BigDecimal ventasMes = ventaRepository.sumTotalEntre(inicioMes, finMes);
        long       countVentasHoy   = ventaRepository.countVentasEntre(inicioDia, finDia);
        long       productosVendHoy = ventaRepository.sumProductosVendidosEntre(inicioDia, finDia);
        long       totalClientes    = clienteRepository.count();

        long countMes = ventaRepository.countVentasEntre(inicioMes, finMes);
        BigDecimal ticketProm = (countMes > 0)
                ? ventasMes.divide(BigDecimal.valueOf(countMes), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        model.addAttribute("ventasHoy",       ventasHoy);
        model.addAttribute("ventasMes",       ventasMes);
        model.addAttribute("ticketProm",      ticketProm);
        model.addAttribute("countVentasHoy",  countVentasHoy);
        model.addAttribute("productosVendHoy",productosVendHoy);
        model.addAttribute("totalClientes",   totalClientes);

        LocalDate limite7d = hoy.plusDays(7);
        model.addAttribute("alertaVencidos",   productoRepository.countVencidos(hoy));
        model.addAttribute("alertaPorVencer",  productoRepository.countPorVencer(hoy, limite7d));
        model.addAttribute("alertaAgotados",   productoRepository.countByEstadoTrueAndStockLessThanEqual(0));
        model.addAttribute("alertaStockBajo",  productoRepository.countStockBajo());

        var logos = logoImagenRepository.findByActivoTrue();
        model.addAttribute("logoActivo", logos.isEmpty() ? null : logos.get(0));

        return "index";
    }
}
