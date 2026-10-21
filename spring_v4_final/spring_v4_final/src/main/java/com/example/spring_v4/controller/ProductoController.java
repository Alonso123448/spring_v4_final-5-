package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Categoria;
import com.example.spring_v4.entity.ImagenProducto;
import com.example.spring_v4.entity.MovimientoInventario;
import com.example.spring_v4.entity.Producto;
import com.example.spring_v4.entity.Proveedor;

import com.example.spring_v4.repository.CategoriaRepository;
import com.example.spring_v4.repository.ImagenProductoRepository;
import com.example.spring_v4.repository.MovimientoInventarioRepository;
import com.example.spring_v4.repository.ProductoRepository;
import com.example.spring_v4.repository.ProveedorRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private static final int MAX_IMAGENES = 3;
    private static final String BASE_DIR   = "src/main/resources/static/imagenes/productos/";

    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/svg+xml"
    );
    private static final Set<String> EXT_PERMITIDAS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg"
    );

    private boolean esImagenValida(MultipartFile archivo) {
        String mime = archivo.getContentType();
        if (mime == null || !MIME_PERMITIDOS.contains(mime.toLowerCase())) return false;
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.contains(".")) return false;
        String ext = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase();
        return EXT_PERMITIDAS.contains(ext);
    }

    private final ProductoRepository productoRepository;
    private final ImagenProductoRepository imagenProductoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public ProductoController(
            ProductoRepository productoRepository,
            ImagenProductoRepository imagenProductoRepository,
            CategoriaRepository categoriaRepository,
            ProveedorRepository proveedorRepository,
            MovimientoInventarioRepository movimientoInventarioRepository) {
        this.productoRepository = productoRepository;
        this.imagenProductoRepository = imagenProductoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    // =====================================
    // LISTAR
    // =====================================
    @GetMapping({"", "/"})
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoRepository.findAll());
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("proveedores", proveedorRepository.findAll());
        return "productos";
    }

    // =====================================
    // GUARDAR / EDITAR
    // =====================================
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Producto producto,
            @RequestParam("archivos") MultipartFile[] archivos,
            RedirectAttributes ra) {

        // ── VALIDACIONES DE NEGOCIO ──
        List<String> errores = new ArrayList<>();

        if (producto.getNombre() == null || producto.getNombre().isBlank())
            errores.add("El nombre del producto es obligatorio.");

        if (producto.getPrecio() == null || producto.getPrecio().doubleValue() <= 0)
            errores.add("El precio debe ser mayor a 0.");

        if (producto.getStock() == null)
            errores.add("El stock es obligatorio.");

        if (producto.getCategoria() == null || producto.getCategoria().getId() == null)
            errores.add("Debes seleccionar una categoría.");

        if (producto.getProveedor() == null || producto.getProveedor().getId() == null)
            errores.add("Debes seleccionar un proveedor.");

        if (!errores.isEmpty()) {
            ra.addFlashAttribute("error", String.join(" | ", errores));
            return "redirect:/productos";
        }

        try {
            boolean esNuevo = (producto.getId() == null);
            Producto productoGuardar;

            if (!esNuevo) {
                productoGuardar = productoRepository.findById(producto.getId()).orElseThrow();
            } else {
                productoGuardar = new Producto();
            }

            productoGuardar.setNombre(producto.getNombre());
            productoGuardar.setDetalle(producto.getDetalle());
            productoGuardar.setPrecio(producto.getPrecio());
            productoGuardar.setStock(producto.getStock());
            productoGuardar.setStockMinimo(producto.getStockMinimo());
            productoGuardar.setFechaVencimiento(producto.getFechaVencimiento());
            productoGuardar.setEstado(producto.getEstado());

            Categoria categoria = categoriaRepository.findById(producto.getCategoria().getId()).orElseThrow();
            productoGuardar.setCategoria(categoria);

            Proveedor proveedor = proveedorRepository.findById(producto.getProveedor().getId()).orElseThrow();
            productoGuardar.setProveedor(proveedor);

            Producto guardado = productoRepository.save(productoGuardar);

            // MOVIMIENTO inicial
            if (esNuevo && guardado.getStock() != null && guardado.getStock() > 0) {
                MovimientoInventario movimiento = new MovimientoInventario();
                movimiento.setProducto(guardado);
                movimiento.setTipo("ENTRADA");
                movimiento.setCantidad(guardado.getStock());
                movimiento.setFecha(LocalDateTime.now());
                movimientoInventarioRepository.save(movimiento);
            }

            // CARPETA por producto: imagenes/productos/{id}/
            String carpetaProducto = BASE_DIR + guardado.getId() + "/";
            File directorio = new File(carpetaProducto);
            if (!directorio.exists()) directorio.mkdirs();

            // Contar imágenes existentes
            int imagenesActuales = (guardado.getImagenes() != null) ? guardado.getImagenes().size() : 0;

            List<ImagenProducto> listaImagenes = new ArrayList<>();

            List<String> archivosRechazados = new ArrayList<>();

            for (MultipartFile archivo : archivos) {
                if (!archivo.isEmpty()) {
                    if (imagenesActuales + listaImagenes.size() >= MAX_IMAGENES) break;

                    if (!esImagenValida(archivo)) {
                        archivosRechazados.add(archivo.getOriginalFilename());
                        continue;
                    }

                    String nombreArchivo = System.currentTimeMillis() + "_" + archivo.getOriginalFilename();
                    Files.write(Paths.get(carpetaProducto + nombreArchivo), archivo.getBytes());

                    ImagenProducto img = new ImagenProducto();
                    img.setRuta("/imagenes/productos/" + guardado.getId() + "/" + nombreArchivo);
                    img.setProducto(guardado);
                    listaImagenes.add(img);
                }
            }

            if (!archivosRechazados.isEmpty()) {
                ra.addFlashAttribute("advertenciaImagen",
                    "Archivo(s) rechazado(s) por formato no permitido: " + String.join(", ", archivosRechazados) +
                    ". Solo se aceptan: JPG, PNG, GIF, WEBP, BMP, SVG.");
            }

            if (!listaImagenes.isEmpty()) {
                imagenProductoRepository.saveAll(listaImagenes);
            }

            if (esNuevo) {
                return "redirect:/productos?success=guardado";
            } else {
                return "redirect:/productos?success=editado";
            }

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Ocurrió un error al guardar el producto. Verifica los datos e intenta nuevamente.");
        }
        return "redirect:/productos";
    }

    // =====================================
    // ELIMINAR IMAGEN INDIVIDUAL
    // =====================================
    @PostMapping("/imagen/eliminar/{id}")
    public String eliminarImagen(@PathVariable Integer id) {
        try {
            ImagenProducto img = imagenProductoRepository.findById(id).orElseThrow();
            File archivo = new File("src/main/resources/static" + img.getRuta());
            if (archivo.exists()) archivo.delete();
            imagenProductoRepository.delete(img);
            return "redirect:/productos?success=imagenEliminada";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/productos";
    }

    // =====================================
    // ELIMINAR PRODUCTO
    // =====================================
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        try {
            Producto producto = productoRepository.findById(id).orElseThrow();
            if (producto.getImagenes() != null) {
                for (ImagenProducto img : producto.getImagenes()) {
                    File archivo = new File("src/main/resources/static" + img.getRuta());
                    if (archivo.exists()) archivo.delete();
                }
            }
            productoRepository.delete(producto);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/productos?success=eliminado";
    }

    // =====================================
    // BUSCAR (incluye imágenes)
    // =====================================
    @GetMapping("/buscar/{id}")
    @ResponseBody
    public Map<String, Object> buscar(@PathVariable Integer id) {
        Producto p = productoRepository.findById(id).orElseThrow();
        Map<String, Object> data = new HashMap<>();
        data.put("id", p.getId());
        data.put("nombre", p.getNombre());
        data.put("detalle", p.getDetalle());
        data.put("precio", p.getPrecio());
        data.put("stock", p.getStock());
        data.put("stockMinimo", p.getStockMinimo());
        data.put("fechaVencimiento", p.getFechaVencimiento());
        data.put("estado", p.getEstado());
        data.put("categoriaId", p.getCategoria() != null ? p.getCategoria().getId() : null);
        data.put("proveedorId", p.getProveedor() != null ? p.getProveedor().getId() : null);

        // Incluir imágenes existentes
        List<Map<String,Object>> imgs = new ArrayList<>();
        if (p.getImagenes() != null) {
            for (ImagenProducto img : p.getImagenes()) {
                Map<String,Object> imgData = new HashMap<>();
                imgData.put("id", img.getId());
                imgData.put("ruta", img.getRuta());
                imgs.add(imgData);
            }
        }
        data.put("imagenes", imgs);
        data.put("totalImagenes", imgs.size());
        data.put("maxImagenes", MAX_IMAGENES);

        return data;
    }
}
