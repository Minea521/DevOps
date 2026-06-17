package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ProfileRepository;
import com.example.demo.repository.TemplateRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final TemplateRepository templateRepository;

    private final Path photoDir = Paths.get("uploads/photos");

    // ===================== CRUD =====================
    public Profile createProfile(Profile profile, MultipartFile photo) throws IOException {
        if (profile.getUuid() == null) {
            profile.setUuid(UUID.randomUUID().toString());
        }
        if (profile.getRegistrationNumber() == null || profile.getRegistrationNumber().isBlank()) {
            profile.setRegistrationNumber(generateRegistrationNumber(profile.getDepartment()));
        }

        // Assign default template if none selected
        if (profile.getTemplate() == null) {
            profile.setTemplate(templateRepository.findByCode("DEFAULT").orElse(null));
        }

        handlePhotoUpload(profile, photo);
        return profileRepository.save(profile);
    }

    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Optional<Profile> getProfileByUuid(String uuid) {
        return profileRepository.findByUuid(uuid);
    }

    public void deleteProfile(Long id) {
        profileRepository.deleteById(id);
    }

    // ===================== Helpers =====================
    private String generateRegistrationNumber(String department) {
        String dept = (department == null || department.isBlank()) ? "GEN" : department.substring(0, 3).toUpperCase();
        return LocalDate.now().getYear() + "-" + dept + "-" + String.format("%03d", (int) (Math.random() * 900) + 100);
    }

    private void handlePhotoUpload(Profile profile, MultipartFile photo) throws IOException {
        if (photo != null && !photo.isEmpty()) {
            Files.createDirectories(photoDir);
            String fileName = UUID.randomUUID() + "_" + photo.getOriginalFilename();
            Files.copy(photo.getInputStream(), photoDir.resolve(fileName));
            profile.setPhotoFileName(fileName);
            profile.setPhotoContentType(photo.getContentType());
        }
    }

    // ===================== PDF + QR + Barcode =====================
    public byte[] generateIdCardPdf(Profile profile) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        Template t = profile.getTemplate() != null ? profile.getTemplate() : new Template();

        document.add(new Paragraph(t.getOrganizationName())
                .setFontSize(18).setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(profile.getFullName())
                .setFontSize(16).setBold().setTextAlignment(TextAlignment.CENTER));

        if (profile.hasPhoto() && profile.getPhotoFileName() != null) {
            Path path = photoDir.resolve(profile.getPhotoFileName());
            if (Files.exists(path)) {
                Image img = new Image(ImageDataFactory.create(path.toString()));
                img.setWidth(130).setHeight(160);
                document.add(img);
            }
        }

        // QR Code
        byte[] qr = generateQrCode(profile.getUuid());
        Image qrImg = new Image(ImageDataFactory.create(qr));
        qrImg.setWidth(100).setHeight(100);
        document.add(qrImg);

        // Barcode
        byte[] barcode = generateBarcode(profile.getRegistrationNumber(), profile.getBarcodeType());
        Image barImg = new Image(ImageDataFactory.create(barcode));
        document.add(barImg);

        document.close();
        return baos.toByteArray();
    }

    private byte[] generateQrCode(String content) throws WriterException, IOException {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix matrix = qrWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", os);
        return os.toByteArray();
    }

    private byte[] generateBarcode(String content, BarcodeType type) throws Exception {
        BarcodeFormat format = (type == BarcodeType.EAN_13) ? BarcodeFormat.EAN_13 : BarcodeFormat.CODE_128;
        com.google.zxing.Writer writer = (format == BarcodeFormat.EAN_13) ?
                new com.google.zxing.oned.EAN13Writer() : new com.google.zxing.oned.Code128Writer();

        BitMatrix matrix = writer.encode(content, format, 300, 80);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", os);
        return os.toByteArray();
    }

    // ===================== Batch Generation =====================
    public byte[] generateBatchPdf(List<Profile> profiles) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        for (Profile p : profiles) {
            document.add(new Paragraph("=== ID Card: " + p.getFullName() + " ==="));
            byte[] singleCard = generateIdCardPdf(p); // Note: This is simplified
            // For better batch, you can improve later
        }

        document.close();
        return baos.toByteArray();
    }
}