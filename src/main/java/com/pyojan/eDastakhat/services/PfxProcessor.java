package com.pyojan.eDastakhat.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pyojan.eDastakhat.models.PfxJSONContentModel;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import static com.pyojan.eDastakhat.libs.Response.*;

public class PfxProcessor {
    private static final Gson jsonPrinter = new GsonBuilder().setPrettyPrinting().create();


    public PfxProcessor() {
        try {
            BouncyCastleProvider provider = new BouncyCastleProvider();
            Security.addProvider(provider);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            generateErrorResponse(e);
        }
    }

    public void readPfx(String pfxFilePath, String pfxPass, String vaultRootDir) {
        if (pfxFilePath == null) {
            throw new IllegalArgumentException("The PFX file path cannot be null.");
        }

        Path path = Paths.get(pfxFilePath);

        // Check if the file exists and is accessible
        if (Files.notExists(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("The PFX file does not exist or is not accessible: " + pfxFilePath);
        }

        String vaultPath = path.getParent().resolve("eDastkhatVault").toString();

        try (InputStream pfxInputStream = Files.newInputStream(path)) {
            // Load the PFX file into a new KeyStore instance
            char[] pass = pfxPass.toCharArray();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(pfxInputStream, pass);

            // Get the X509 Certificate from the KeyStore
            X509Certificate x509Certificate = getX509Certificate(keyStore);

            String certSerialNumber = x509Certificate.getSerialNumber().toString(16).toUpperCase();
            String rootDir = (vaultRootDir == null) ? String.format("%s/%s", vaultPath, certSerialNumber) : vaultRootDir + "/eDastkhatVault/" + certSerialNumber;

            copyPfxToJks(keyStore, new String(pass), rootDir);  // Pass keyStore instance here

            generateStdoutResponseOfPfx(x509Certificate);

        } catch (IllegalArgumentException | IOException | NoSuchAlgorithmException | CertificateException |
                 KeyStoreException | UnrecoverableKeyException e) {
            generateErrorResponse(e);
        }
    }

    private void copyPfxToJks(KeyStore keyStore, String pfxPassword, String filePath) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, CertificateException {
        // Ensure directory exists
        File directory = new File(filePath).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory: " + directory);
        }

        KeyStore jksKeyStore = KeyStore.getInstance("JKS");
        jksKeyStore.load(null, pfxPassword.toCharArray());
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, pfxPassword.toCharArray());
                java.security.cert.Certificate[] certificateChain = keyStore.getCertificateChain(alias);
                jksKeyStore.setKeyEntry(alias, privateKey, pfxPassword.toCharArray(), certificateChain);
            }
        }

        try (OutputStream outputStream = Files.newOutputStream(Paths.get(filePath))) {
            jksKeyStore.store(outputStream, pfxPassword.toCharArray());
        }
    }

    public void generateStdoutResponseOfPfx(X509Certificate x509Certificate) {
        HashMap<String, String> result = new HashMap<>();

        // Extracting commonName from the subject
        String subjectName = x509Certificate.getSubjectX500Principal().getName();
        String[] subjectParts = subjectName.split(",");
        result.put("commonName", extractAttribute(subjectParts[0], "CN"));
        result.put("subject", subjectName);

        // Getting serial number
        result.put("serialNumber", x509Certificate.getSerialNumber().toString(16).toUpperCase());

        // Getting validity dates
        result.put("notBefore", x509Certificate.getNotBefore().toString());
        result.put("notAfter", x509Certificate.getNotAfter().toString());

        // Extracting issuer
        String issuerName = x509Certificate.getIssuerX500Principal().getName();
        String[] issuerParts = issuerName.split(",");
        result.put("issuer", extractAttribute(issuerParts[2], "O"));

        generateSuccessResponse(result);

    }

    public X509Certificate getX509Certificate(KeyStore keyStore) throws KeyStoreException, CertificateException {
        X509Certificate x509Certificate = null;
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509Cert = (X509Certificate) cert;
                    if (isUserCertificate(x509Cert)) {
                        isCertificateNotExpired(x509Cert);
                        x509Certificate = x509Cert;
                        break;
                    } else {
                        throw new CertificateValidationException("Certificate is not a user certificate.");
                    }
                } else {
                    throw new CertificateException("Certificate is not an X509 certificate.");
                }
            }
        }
        if (x509Certificate == null) {
            throw new CertificateNotFoundException("Only user certificate is allowed.");
        }
        return x509Certificate;
    }

    protected boolean isUserCertificate(X509Certificate x509Cert) {
        return x509Cert.getBasicConstraints() == -1;
    }

    protected void isCertificateNotExpired(X509Certificate x509Certificate) throws CertificateExpiredException {
        String serialNumber = x509Certificate.getSerialNumber().toString(16).toUpperCase();
        try {
            Date currentDate = new Date();
            x509Certificate.checkValidity(currentDate);
        } catch (java.security.cert.CertificateExpiredException | CertificateNotYetValidException e) {
            if(e.getMessage().startsWith("NotAfter")) {
                throw new CertificateExpiredException("Certificate with serial number '" + serialNumber + "' has expired.");
            } else {
                throw new CertificateExpiredException("Certificate with serial number '" + serialNumber + "' is not yet valid.");
            }
        }
    }

    // Method to extract an attribute from a string formatted as key=value
    private String extractAttribute(String input, String attribute) {
        String[] parts = input.split("=");
        if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(attribute)) {
            return parts[1].trim();
        }
        return null;
    }


    // Custom Exceptions
    public static class CertificateExpiredException extends CertificateException {
        public CertificateExpiredException(String message) {
            super(message);
        }
    }

    static class CertificateValidationException extends CertificateException {
        public CertificateValidationException(String message) {
            super(message);
        }
    }

    static class CertificateNotFoundException extends CertificateException {
        public CertificateNotFoundException(String message) {
            super(message);
        }
    }

}
