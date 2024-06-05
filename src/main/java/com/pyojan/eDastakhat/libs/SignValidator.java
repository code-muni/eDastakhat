package com.pyojan.eDastakhat.libs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pyojan.eDastakhat.models.SignatureModel;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class SignValidator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final SignatureModel signatureModel;

    public SignValidator(Path pdfContentJSONFilePath) throws IOException {
        if (pdfContentJSONFilePath == null) {
            throw new NullPointerException("pdfContentJSONFilePath is required");
        }
        this.signatureModel = loadSignatureModel(pdfContentJSONFilePath);
    }

    private SignatureModel loadSignatureModel(Path pdfContentJSONFilePath) throws IOException {
        String jsonContent = new String(Files.readAllBytes(pdfContentJSONFilePath));
        return gson.fromJson(jsonContent, SignatureModel.class);
    }

    public SignatureModel validateSignatureModel() throws IOException, IllegalArgumentException {
        validatePdfPayloadModel();
        validateCertInfoModel();
        validatePdfModel();

        return signatureModel;
    }

    private void validatePdfPayloadModel() throws IllegalArgumentException {
        Validator validator = new Validator();
        List<ConstraintViolation> violations = validator.validate(signatureModel);
        if (!violations.isEmpty()) {
            List<String> errors = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
            throw new IllegalArgumentException(String.valueOf(errors));
        }
    }

    private void validateCertInfoModel() throws IOException {
        SignatureModel.CertInfo certInfo = signatureModel.getCertInfo();
        String pfxPath = certInfo.getPfxPath();
        File file = new File(pfxPath);
        if (!file.exists()) {
            throw new IOException("PFX path is incorrect or the file does not exist.");
        }
    }

    private void validatePdfModel() throws IllegalArgumentException {
        SignatureModel.Pdf pdf = signatureModel.getPdf();
        String base64Content = pdf.getBase64Content();
        try {
            Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PDF base64 content. Please ensure that the provided base64 content represents a valid PDF document.");

        }
    }
}

