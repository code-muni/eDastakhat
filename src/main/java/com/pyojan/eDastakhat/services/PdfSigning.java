package com.pyojan.eDastakhat.services;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import com.pyojan.eDastakhat.libs.KeyStoreManager;
import com.pyojan.eDastakhat.libs.SignValidator;
import com.pyojan.eDastakhat.models.SignatureModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;

import static com.pyojan.eDastakhat.libs.Response.generateErrorResponse;
import static com.pyojan.eDastakhat.libs.Response.generateSuccessResponse;

/**
 * Class responsible for signing PDF documents based on the provided signature model.
 */
public class PdfSigning {

    private SignatureModel signatureModel;
    private String outDir;

    public PdfSigning(String pdfContentJSONFilePath, String outRootDir) {
        constructorProcess(pdfContentJSONFilePath, outRootDir);
    }

    /**
     * Processes the constructor arguments and initializes the signature model.
     *
     * @param pdfContentJSONFilePath Path to the JSON file containing PDF content information.
     * @param outRootDir             Root directory for storing signed PDF files.
     */
    private void constructorProcess(String pdfContentJSONFilePath, String outRootDir) {
        try {

            Path path = Paths.get(pdfContentJSONFilePath);
            if (outRootDir == null || outRootDir.isEmpty()) {
                outRootDir = path.getParent().toString();
            }
            this.outDir = outRootDir;
            signatureModel = new SignValidator(path).validateSignatureModel();
        } catch (IOException e) {
            generateErrorResponse(e);
        }
    }

    /**
     * Executes the PDF signing process based on the initialized signature model.
     */
    public void executeSign() {
        try {
            if (signatureModel != null) {

                String signData;

                if (signatureModel.getPdf().getBase64Content() == null)
                    throw new IllegalArgumentException("Expected a base64 encoded PDF content. Please ensure that the provided base64 content represents a valid PDF document.");
                SignatureModel.Pdf pdf = signatureModel.getPdf();

                if (signatureModel.getOptions() == null)
                    throw new IllegalArgumentException("Expected a signature options object. Please ensure that the provided options are valid.");
                SignatureModel.Options options = signatureModel.getOptions();

                byte[] decodePdf = Base64.getDecoder().decode(pdf.getBase64Content());
                byte[] password = pdf.getPassword() == null ? "".getBytes() : pdf.getPassword().getBytes();

                PdfReader pdfReader = new PdfReader(decodePdf, password);

                int totalPages = pdfReader.getNumberOfPages();
                Rectangle rectangle = getSignatureRectangle(options.getCoord());
                boolean changesAllowed = options.isChangesAllowed();
                boolean isTimestamp = options.getTimestamp().isEnabled() && (options.getTimestamp().getUrl() != null && !options.getTimestamp().getUrl().isEmpty());

                Path pfxPath = Paths.get(signatureModel.getCertInfo().getPfxPath());
                KeyStoreManager keyStoreManager = new KeyStoreManager(pfxPath, signatureModel.getCertInfo().getPassword());

                if ("A".equalsIgnoreCase(signatureModel.getOptions().getPage())) {
                    if (changesAllowed)
                        throw new IllegalArgumentException("Signing all pages with Changes Not Allowed [ changesAllowed: false ] is currently under development.");

                    signData = signAllPages(pdfReader, keyStoreManager.getPrivateKey(), keyStoreManager.getProvider().getName(), keyStoreManager.getCertificateChain(), rectangle, signatureModel.getPdf().getPassword(), isTimestamp);
                } else {
                    int signaturePageNumber = getSignaturePageNumber(options.getPage(), totalPages);
                    signData = sign(pdfReader, keyStoreManager.getPrivateKey(), keyStoreManager.getProvider().getName(), keyStoreManager.getCertificateChain(), signaturePageNumber, isTimestamp, changesAllowed, rectangle);
                }

                String fileName = generateFileName();
                HashMap<String, String> signDataMap = new HashMap<>();

                String savedPath = writePdfToDisk(outDir + "/" + fileName + ".pdf", signData);
                signDataMap.put("fileName", fileName);
                signDataMap.put("filePath", savedPath);

                generateSuccessResponse("SUCCESS", signDataMap);
            }

        } catch (GeneralSecurityException | IOException e) {
            generateErrorResponse(e);
        }
    }

    private Rectangle getSignatureRectangle(int[] coordinates) {
        return new Rectangle(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
    }

    private String sign(PdfReader reader, PrivateKey privateKey, String provider, Certificate[] certChain, int pageNumber, boolean isTimestamp, boolean isChangesAllowed, Rectangle rectangle) {
        ByteArrayOutputStream signedPdfOutputStream = new ByteArrayOutputStream();
        PdfStamper stamper = null;

        try {
            SignatureModel.Options options = signatureModel.getOptions();
            stamper = PdfStamper.createSignature(reader, signedPdfOutputStream, '\0', null, true);

            PdfSignatureAppearance appearance = getPdfSignatureAppearance(stamper, rectangle, pageNumber,
                    String.format("eDastakhat::PAGE:%d", pageNumber), isChangesAllowed,
                    options.getReason(), options.getLocation(), options.isGreenTick());
            TSAClient tsaClient = !isTimestamp ? null : new TSAClientBouncyCastle(options.getTimestamp().getUrl(), options.getTimestamp().getUsername(), options.getTimestamp().getPassword(), 8192, "SHA-256");

            ExternalDigest digest = new BouncyCastleDigest();
            ExternalSignature signature = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, provider);

            MakeSignature.signDetached(
                    appearance,
                    digest,
                    signature,
                    certChain,
                    null,
                    null,
                    tsaClient,
                    0,
                    MakeSignature.CryptoStandard.CADES
            );
        } catch (DocumentException | IOException | GeneralSecurityException e) {
            generateErrorResponse(e);
        } finally {
            if (stamper != null) {
                try {
                    stamper.close();
                } catch (DocumentException | IOException e) {
                    generateErrorResponse(e);
                }
            }
        }

        return Base64.getEncoder().encodeToString(signedPdfOutputStream.toByteArray());
    }

    private String signAllPages(PdfReader reader, PrivateKey privateKey, String provider, Certificate[] certChain, Rectangle rectangle, String pdfPassword, boolean isTimestamp) throws IOException {
        String tempSignedPdf = null;

        PdfReader readerHolder = reader;

        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            tempSignedPdf = sign(readerHolder, privateKey, provider, certChain, page, isTimestamp, true, rectangle);

            byte[] decode = Base64.getDecoder().decode(tempSignedPdf);
            readerHolder = new PdfReader(decode, pdfPassword.getBytes());
        }
        return tempSignedPdf;
    }

    private PdfSignatureAppearance getPdfSignatureAppearance(PdfStamper stamper, Rectangle rectangle, int pageNumber, String fieldName, boolean isChangesAllowed, String reason, String location, boolean isGreenTrick) {
        PdfSignatureAppearance appearance = stamper.getSignatureAppearance();

        if (rectangle != null) {
            appearance.setVisibleSignature(rectangle, pageNumber, fieldName);
        }

        appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.NAME_AND_DESCRIPTION);
        appearance.setAcro6Layers(!isGreenTrick);

        if (!isChangesAllowed) {
            appearance.setCertificationLevel(PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED);
        }

        appearance.setReason(reason);
        appearance.setLocation(location);

        return appearance;
    }

    private int getSignaturePageNumber(String page, int totalPages) {
        int pageNumber;

        if ("L".equalsIgnoreCase(page)) {
            pageNumber = totalPages;
        } else if ("F".equalsIgnoreCase(page)) {
            pageNumber = 1;
        } else {
            try {
                pageNumber = Integer.parseInt(page);

                if (pageNumber > totalPages) {
                    throw new IllegalArgumentException("Invalid page number specified: " + page + ". The document has only " + totalPages + " pages.");
                }

            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid page option: " + page);
            }
        }

        return pageNumber;
    }

    private String generateFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH-mm-ss");
        return now.format(formatter) + ".pdf";
    }

    private String writePdfToDisk(String filePath, String base64Content) throws IOException {
        // Convert the file path string to a Path object
        Path path = Paths.get(filePath);

        // Create parent directories if they don't exist
        Path parentDirectory = path.getParent();
        if (parentDirectory != null && !Files.exists(parentDirectory)) {
            Files.createDirectories(parentDirectory);
        }

        // Decode the Base64 content
        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);

        // Write the decoded bytes to the specified file path
        Files.write(path, decodedBytes);

        // Return the path as a string
        return path.toString();
    }
}
