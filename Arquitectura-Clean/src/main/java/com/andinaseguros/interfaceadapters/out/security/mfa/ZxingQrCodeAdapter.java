package com.andinaseguros.interfaceadapters.out.security.mfa;

import com.andinaseguros.usecases.port.out.security.QrCodeGeneratorPort;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ZxingQrCodeAdapter implements QrCodeGeneratorPort {
    @Override
    public String generarDataUri(String contenido) {
        try {
            var matrix = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 280, 280);
            var output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el QR de MFA", e);
        }
    }
}
