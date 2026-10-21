package com.example.spring_v4.service;

import com.example.spring_v4.entity.DetalleVenta;
import com.example.spring_v4.entity.Venta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.nombre:Inventario}")
    private String appNombre;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── Enviar enlace recuperación de contraseña ─────────────────────────────
    public void enviarRecuperacion(String destinatario, String username, String enlace) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(remitente);
        helper.setTo(destinatario);
        helper.setSubject(appNombre + " — Recuperación de contraseña");

        String html = """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f0f4f8;font-family:'Segoe UI',Arial,sans-serif;">
            <div style="max-width:480px;margin:30px auto;background:white;border-radius:18px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,.1);">
              <div style="background:linear-gradient(135deg,#667eea,#764ba2);padding:28px;text-align:center;">
                <div style="font-size:40px;margin-bottom:8px;">🔑</div>
                <h2 style="margin:0;color:white;font-size:20px;font-weight:900;">Recuperar contraseña</h2>
                <p style="margin:6px 0 0;color:rgba(255,255,255,.75);font-size:13px;">%s</p>
              </div>
              <div style="padding:32px;text-align:center;">
                <p style="color:#475569;font-size:14px;margin-bottom:24px;">
                  Hola <strong>%s</strong>, recibimos una solicitud para restablecer tu contraseña.<br>
                  Haz clic en el botón para continuar:
                </p>
                <a href="%s"
                   style="display:inline-block;background:linear-gradient(135deg,#667eea,#764ba2);
                          color:white;text-decoration:none;padding:14px 32px;border-radius:12px;
                          font-weight:700;font-size:15px;">
                  Restablecer contraseña
                </a>
                <p style="color:#94a3b8;font-size:12px;margin-top:20px;">
                  ⏱ Este enlace expira en <strong>30 minutos</strong>.
                </p>
                <p style="color:#94a3b8;font-size:11px;margin-top:6px;">
                  Si no solicitaste esto, ignora este mensaje.
                </p>
              </div>
              <div style="background:#f8fafc;padding:14px 28px;font-size:12px;color:#94a3b8;text-align:center;border-top:1px solid #e2e8f0;">
                Enviado automáticamente por <strong>%s</strong>
              </div>
            </div>
            </body>
            </html>
        """.formatted(appNombre, username, enlace, appNombre);

        helper.setText(html, true);
        mailSender.send(message);
    }

    // ── Enviar código 2FA ────────────────────────────────────────────────────
    public void enviarCodigo2FA(String destinatario, String codigo, String username) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(remitente);
        helper.setTo(destinatario);
        helper.setSubject(appNombre + " — Código de verificación");

        String html = """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f0f4f8;font-family:'Segoe UI',Arial,sans-serif;">
            <div style="max-width:480px;margin:30px auto;background:white;border-radius:18px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,.1);">
              <div style="background:linear-gradient(135deg,#667eea,#764ba2);padding:28px;text-align:center;">
                <div style="font-size:40px;margin-bottom:8px;">🔐</div>
                <h2 style="margin:0;color:white;font-size:20px;font-weight:900;">Verificación de acceso</h2>
                <p style="margin:6px 0 0;color:rgba(255,255,255,.75);font-size:13px;">%s</p>
              </div>
              <div style="padding:32px;text-align:center;">
                <p style="color:#475569;font-size:14px;margin-bottom:20px;">
                  Hola <strong>%s</strong>, usa este código para completar tu inicio de sesión:
                </p>
                <div style="background:#f8fafc;border:2px dashed #667eea;border-radius:14px;padding:24px;margin:0 auto;max-width:240px;">
                  <div style="font-size:42px;font-weight:900;letter-spacing:10px;color:#667eea;">%s</div>
                </div>
                <p style="color:#94a3b8;font-size:12px;margin-top:18px;">
                  ⏱ Este código expira en <strong>5 minutos</strong>.
                </p>
                <p style="color:#94a3b8;font-size:11px;margin-top:6px;">
                  Si no fuiste tú, ignora este mensaje.
                </p>
              </div>
              <div style="background:#f8fafc;padding:14px 28px;font-size:12px;color:#94a3b8;text-align:center;border-top:1px solid #e2e8f0;">
                Enviado automáticamente por <strong>%s</strong>
              </div>
            </div>
            </body>
            </html>
        """.formatted(appNombre, username, codigo, appNombre);

        helper.setText(html, true);
        mailSender.send(message);
    }

    // ── Enviar comprobante de venta ──────────────────────────────────────────
    public void enviarComprobante(String destinatario, Venta venta) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(remitente);
        helper.setTo(destinatario);

        // Asunto dinámico según tipo de documento
        String tipo = "Comprobante";
        if (venta.getSerie() != null) {
            if (venta.getSerie().contains("BOLETA"))  tipo = "Boleta de Venta";
            else if (venta.getSerie().contains("FACTURA")) tipo = "Factura";
            else if (venta.getSerie().contains("NOTA"))    tipo = "Nota de Venta";
            else if (venta.getSerie().contains("PEDIDO"))  tipo = "Confirmación de Pedido";
        }
        helper.setSubject(appNombre + " — " + tipo + " " + (venta.getNumeroDocumento() != null ? venta.getNumeroDocumento() : "#" + venta.getId()));

        // Construir HTML del correo
        String fecha = venta.getFecha() != null
                ? venta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "-";

        StringBuilder detallesHtml = new StringBuilder();
        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                detallesHtml.append("""
                    <tr>
                        <td style="padding:10px 14px;border-bottom:1px solid #f1f5f9;">%s</td>
                        <td style="padding:10px 14px;border-bottom:1px solid #f1f5f9;text-align:center;">%d</td>
                        <td style="padding:10px 14px;border-bottom:1px solid #f1f5f9;text-align:right;color:#2563eb;font-weight:700;">S/ %.2f</td>
                        <td style="padding:10px 14px;border-bottom:1px solid #f1f5f9;text-align:right;font-weight:700;">S/ %.2f</td>
                    </tr>
                """.formatted(d.getProductoNombre(), d.getCantidad(),
                              d.getPrecio().doubleValue(), d.getSubtotal().doubleValue()));
            }
        }

        String estadoColor = "COMPLETADA".equals(venta.getEstado()) ? "#10b981"
                           : "PENDIENTE".equals(venta.getEstado())  ? "#f59e0b" : "#ef4444";

        String html = """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f0f4f8;font-family:'Segoe UI',Arial,sans-serif;">
            <div style="max-width:600px;margin:30px auto;background:white;border-radius:18px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,.1);">

              <!-- HEADER -->
              <div style="background:linear-gradient(135deg,#0f172a,#1e293b);padding:28px;color:white;">
                <div style="display:flex;justify-content:space-between;align-items:center;">
                  <div>
                    <h2 style="margin:0;font-size:22px;font-weight:900;">%s</h2>
                    <p style="margin:4px 0 0;color:#94a3b8;font-size:12px;text-transform:uppercase;letter-spacing:2px;">Comprobante de pago</p>
                  </div>
                  <div style="text-align:right;">
                    <div style="font-size:11px;color:#94a3b8;text-transform:uppercase;letter-spacing:1px;">%s</div>
                    <div style="font-size:26px;font-weight:900;color:white;">%s</div>
                    <div style="display:inline-block;margin-top:6px;padding:4px 12px;border-radius:20px;font-size:11px;font-weight:700;background:%s;color:white;">%s</div>
                  </div>
                </div>
              </div>

              <!-- DATOS CLIENTE -->
              <div style="padding:24px 28px;">
                <table style="width:100%%;border-collapse:collapse;margin-bottom:24px;">
                  <tr>
                    <td style="padding:8px 0;"><span style="font-size:11px;color:#94a3b8;text-transform:uppercase;letter-spacing:1px;">Cliente</span><br><strong>%s</strong></td>
                    <td style="padding:8px 0;"><span style="font-size:11px;color:#94a3b8;text-transform:uppercase;letter-spacing:1px;">Documento</span><br><strong>%s</strong></td>
                    <td style="padding:8px 0;"><span style="font-size:11px;color:#94a3b8;text-transform:uppercase;letter-spacing:1px;">Fecha</span><br><strong>%s</strong></td>
                  </tr>
                </table>

                <!-- PRODUCTOS -->
                <table style="width:100%%;border-collapse:collapse;margin-bottom:20px;">
                  <thead>
                    <tr style="background:#0f172a;">
                      <th style="padding:10px 14px;color:#93c5fd;font-size:11px;text-transform:uppercase;text-align:left;border-radius:8px 0 0 8px;">Producto</th>
                      <th style="padding:10px 14px;color:#93c5fd;font-size:11px;text-transform:uppercase;text-align:center;">Cant.</th>
                      <th style="padding:10px 14px;color:#93c5fd;font-size:11px;text-transform:uppercase;text-align:right;">P. Unit.</th>
                      <th style="padding:10px 14px;color:#93c5fd;font-size:11px;text-transform:uppercase;text-align:right;border-radius:0 8px 8px 0;">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>%s</tbody>
                </table>

                <!-- TOTAL -->
                <div style="background:linear-gradient(135deg,#0f172a,#1e293b);border-radius:14px;padding:16px 20px;display:flex;justify-content:space-between;align-items:center;">
                  <span style="color:#94a3b8;font-size:13px;font-weight:600;text-transform:uppercase;letter-spacing:1px;">Total</span>
                  <span style="font-size:28px;font-weight:900;color:#60a5fa;">S/ %.2f</span>
                </div>
              </div>

              <!-- FOOTER -->
              <div style="background:#f8fafc;padding:14px 28px;font-size:12px;color:#94a3b8;text-align:center;border-top:1px solid #e2e8f0;">
                Este comprobante fue generado automáticamente por <strong>%s</strong>. Gracias por su compra ❤
              </div>

            </div>
            </body>
            </html>
        """.formatted(
            appNombre,
            tipo,
            venta.getNumeroDocumento() != null ? venta.getNumeroDocumento() : "#" + venta.getId(),
            estadoColor,
            venta.getEstado() != null ? venta.getEstado() : "—",
            venta.getClienteNombre() != null ? venta.getClienteNombre() : "—",
            venta.getClienteDocumento() != null ? venta.getClienteDocumento() : "—",
            fecha,
            detallesHtml.toString(),
            venta.getTotal().doubleValue(),
            appNombre
        );

        helper.setText(html, true);
        mailSender.send(message);
    }
}
