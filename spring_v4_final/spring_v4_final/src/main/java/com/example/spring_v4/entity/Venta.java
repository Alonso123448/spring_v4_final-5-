package com.example.spring_v4.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String numeroDocumento;   // N001-00000001
    private String serie;             // NOTA DE VENTA (N001)

    private String clienteDocumento;
    private String clienteNombre;
    private String vendedor;

    private LocalDateTime fecha;
    private BigDecimal total;

    private String formaPago;         // CONTADO / CREDITO
    private Boolean esCredito = false;

    private BigDecimal pagoInicial;
    private Integer nCuotas;
    private String intervaloPago;     // SEMANAL, QUINCENAL, MENSUAL
    private BigDecimal deuda;

    private String estado;            // PENDIENTE, COMPLETADA, ANULADA

    // Pedidos online: aprobación del admin
    private String aprobacionPedido;  // PENDIENTE, APROBADO, RECHAZADO (null = no aplica)
    private String rutaVoucher;       // ruta de la imagen de comprobante de pago
    private String clienteTelefono;   // teléfono del cliente (pedidos online)
    private String clienteCorreo;     // correo del cliente (pedidos online)

    private String codigoOperacion;   // código de operación del pago (único)
    private String tipoEntrega;       // DOMICILIO / TIENDA
    private String direccionEntrega;  // dirección si tipoEntrega=DOMICILIO
    private String rutaVoucherAdmin;  // comprobante adjuntado por el admin al aprobar
    private String notasAdmin;        // notas/observaciones del admin al aprobar

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cuota> cuotas;

    // GETTERS Y SETTERS
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public String getClienteDocumento() { return clienteDocumento; }
    public void setClienteDocumento(String clienteDocumento) { this.clienteDocumento = clienteDocumento; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getVendedor() { return vendedor; }
    public void setVendedor(String vendedor) { this.vendedor = vendedor; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public Boolean getEsCredito() { return esCredito; }
    public void setEsCredito(Boolean esCredito) { this.esCredito = esCredito; }
    public BigDecimal getPagoInicial() { return pagoInicial; }
    public void setPagoInicial(BigDecimal pagoInicial) { this.pagoInicial = pagoInicial; }
    public Integer getNCuotas() { return nCuotas; }
    public void setNCuotas(Integer nCuotas) { this.nCuotas = nCuotas; }
    public String getIntervaloPago() { return intervaloPago; }
    public void setIntervaloPago(String intervaloPago) { this.intervaloPago = intervaloPago; }
    public BigDecimal getDeuda() { return deuda; }
    public void setDeuda(BigDecimal deuda) { this.deuda = deuda; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
    public List<Cuota> getCuotas() { return cuotas; }
    public void setCuotas(List<Cuota> cuotas) { this.cuotas = cuotas; }
    public String getAprobacionPedido() { return aprobacionPedido; }
    public void setAprobacionPedido(String aprobacionPedido) { this.aprobacionPedido = aprobacionPedido; }
    public String getRutaVoucher() { return rutaVoucher; }
    public void setRutaVoucher(String rutaVoucher) { this.rutaVoucher = rutaVoucher; }
    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }
    public String getClienteCorreo() { return clienteCorreo; }
    public void setClienteCorreo(String clienteCorreo) { this.clienteCorreo = clienteCorreo; }
    public String getCodigoOperacion() { return codigoOperacion; }
    public void setCodigoOperacion(String codigoOperacion) { this.codigoOperacion = codigoOperacion; }
    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String tipoEntrega) { this.tipoEntrega = tipoEntrega; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public String getRutaVoucherAdmin() { return rutaVoucherAdmin; }
    public void setRutaVoucherAdmin(String rutaVoucherAdmin) { this.rutaVoucherAdmin = rutaVoucherAdmin; }
    public String getNotasAdmin() { return notasAdmin; }
    public void setNotasAdmin(String notasAdmin) { this.notasAdmin = notasAdmin; }
}
