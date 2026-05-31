package com.lms.progress.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.lms.progress.entity.Certificate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class CertificatePdfService {

    @Value("${application.base-url:http://localhost:8080}")
    private String baseUrl;

    // main method — generates PDF as byte array
    public byte[] generateCertificate(Certificate certificate)
            throws IOException, WriterException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);

        // landscape orientation — certificates look better wide
        Document document = new Document(pdf, PageSize.A4.rotate());
        document.setMargins(40, 60, 40, 60);

        // colors
        DeviceRgb primaryColor   = new DeviceRgb(79, 70, 229);   // indigo
        DeviceRgb goldColor      = new DeviceRgb(217, 174, 78);   // gold
        DeviceRgb lightGray      = new DeviceRgb(248, 248, 248);
        DeviceRgb darkGray       = new DeviceRgb(75, 75, 75);

        // fonts
        PdfFont boldFont    = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
        PdfFont italicFont  = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_OBLIQUE);

        // ── HEADER ───────────────────────────────────────────────────

        // platform name
        document.add(new Paragraph("LMS PLATFORM")
                .setFont(boldFont)
                .setFontSize(28)
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        // gold line under header
        SolidLine line = new SolidLine(2f);
        line.setColor(goldColor);
        document.add(new LineSeparator(line).setMarginBottom(20));

        // certificate title
        document.add(new Paragraph("Certificate of Completion")
                .setFont(italicFont)
                .setFontSize(20)
                .setFontColor(darkGray)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30));

        // ── BODY ─────────────────────────────────────────────────────

        document.add(new Paragraph("This certifies that")
                .setFont(regularFont)
                .setFontSize(14)
                .setFontColor(darkGray)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));

        // student name — largest text on page
        document.add(new Paragraph(
                certificate.getStudent().getFirstName() + " " +
                        certificate.getStudent().getLastName())
                .setFont(boldFont)
                .setFontSize(36)
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));

        document.add(new Paragraph("has successfully completed")
                .setFont(regularFont)
                .setFontSize(14)
                .setFontColor(darkGray)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));

        // course name
        document.add(new Paragraph(certificate.getCourse().getTitle())
                .setFont(boldFont)
                .setFontSize(24)
                .setFontColor(new DeviceRgb(30, 30, 30))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30));

        // gold separator
        SolidLine line2 = new SolidLine(1f);
        line2.setColor(goldColor);
        document.add(new LineSeparator(line2).setMarginBottom(20));

        // ── FOOTER — date, cert number, QR code ──────────────────────

        // two column table for footer
        Table footer = new Table(UnitValue.createPercentArray(
                new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // left cell — issue date
        String formattedDate = certificate.getIssuedAt()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        footer.addCell(createFooterCell(
                "Issue Date\n" + formattedDate,
                regularFont, boldFont, darkGray
        ));

        // center cell — certificate number
        footer.addCell(createFooterCell(
                "Certificate Number\n" + certificate.getCertificateNumber(),
                regularFont, boldFont, darkGray
        ));

        // right cell — QR code
        String verifyUrl = baseUrl + "/api/progress/certificates/verify/"
                + certificate.getCertificateNumber();

        byte[] qrBytes = generateQRCode(verifyUrl, 120, 120);
        Image qrImage = new Image(ImageDataFactory.create(qrBytes))
                .setWidth(80)
                .setHeight(80)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        com.itextpdf.layout.element.Cell qrCell =
                new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph("Scan to verify")
                                .setFont(regularFont)
                                .setFontSize(9)
                                .setFontColor(darkGray)
                                .setTextAlignment(TextAlignment.CENTER))
                        .add(qrImage)
                        .setBorder(null)              // ← just set null directly
                        .setTextAlignment(TextAlignment.CENTER);

        // remove cell borders
        qrCell.setBorder(null);
        footer.addCell(qrCell);

        document.add(footer);

        document.close();

        log.info("Certificate PDF generated for: {}",
                certificate.getCertificateNumber());

        return outputStream.toByteArray();
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private com.itextpdf.layout.element.Cell createFooterCell(
            String text, PdfFont regularFont,
            PdfFont boldFont, DeviceRgb color) {

        String[] parts = text.split("\n");
        com.itextpdf.layout.element.Cell cell =
                new com.itextpdf.layout.element.Cell();
        cell.setBorder(null);

        if (parts.length > 0) {
            cell.add(new Paragraph(parts[0])
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setFontColor(color)
                    .setTextAlignment(TextAlignment.CENTER));
        }
        if (parts.length > 1) {
            cell.add(new Paragraph(parts[1])
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setFontColor(color)
                    .setTextAlignment(TextAlignment.CENTER));
        }
        return cell;
    }

    private byte[] generateQRCode(String content, int width, int height)
            throws WriterException, IOException {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrWriter.encode(
                content, BarcodeFormat.QR_CODE, width, height
        );

        BufferedImage bufferedImage = MatrixToImageWriter
                .toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", baos);
        return baos.toByteArray();
    }
}