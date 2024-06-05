package com.pyojan.eDastakhat.libs;

import com.pyojan.eDastakhat.services.PfxProcessor;
import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class KeyStoreManager extends PfxProcessor {

    private final Path pfxPath;
    private final String password;

    private String alias;
    private KeyStore keyStore;
    @Getter
    private final BouncyCastleProvider provider = new BouncyCastleProvider();

    public KeyStoreManager(Path pfxPath, String password) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        this.pfxPath = pfxPath;
        this.password = password;

        getKeyStore();
    }

    public void getKeyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        Security.addProvider(provider);
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        byte[] pfxBytes = Files.readAllBytes(pfxPath);
        keyStore.load(new ByteArrayInputStream(pfxBytes), password.toCharArray());

        alias = getAliasFromKeyStore();
    }


    private String getAliasFromKeyStore() throws KeyStoreException, CertificateExpiredException {

        while (keyStore.aliases().hasMoreElements()) {
            String alias = keyStore.aliases().nextElement();
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate instanceof X509Certificate) {
                X509Certificate x509Certificate = (X509Certificate) certificate;
                if (isUserCertificate(x509Certificate) ) {
                    isCertificateNotExpired(x509Certificate);
                    return alias;
                }
            }
        }
        throw new RuntimeException("User certificate not found in KeyStore.");
    }

    public PrivateKey getPrivateKey() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        return (PrivateKey) keyStore.getKey(alias, password.toCharArray());
    }

    public Certificate[] getCertificateChain() throws KeyStoreException {
        return keyStore.getCertificateChain(alias);
    }
}
