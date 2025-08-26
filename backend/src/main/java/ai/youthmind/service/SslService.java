package ai.youthmind.service;

import ai.youthmind.config.SslConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

@Service
public class SslService {

    @Autowired
    private SslConfig sslConfig;

    public CertificateHealth checkCertificateHealth() {
        try (InputStream is = getKeystoreInputStream()) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(is, sslConfig.getKeyStorePassword().toCharArray());

            Enumeration<String> aliases = keystore.aliases();

            if (!aliases.hasMoreElements()) {
                return new CertificateHealth("DOWN", "No certificates found in keystore");
            }

            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);

            return evaluateCertificate(cert);
        } catch (Exception e) {
            return new CertificateHealth("DOWN", "Certificate check failed: " + e.getMessage());
        }
    }

    private InputStream getKeystoreInputStream() throws Exception {
        String path = sslConfig.getKeyStore();
        if (path.startsWith("classpath:")) {
            return getClass().getResourceAsStream(path.substring(10));
        } else if (path.startsWith("file:")) {
            return new FileInputStream(path.substring(5));
        } else {
            // Default to classpath
            return getClass().getResourceAsStream(path);
        }
    }

    private CertificateHealth evaluateCertificate(X509Certificate cert) {
        Date now = new Date();
        Date expiry = cert.getNotAfter();
        long remainingDays = (expiry.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);

        if (now.after(expiry)) {
            return new CertificateHealth("DOWN",
                    "Certificate expired on " + expiry,
                    expiry,
                    cert.getIssuerX500Principal().getName(),
                    remainingDays);
        }

        if (remainingDays <= sslConfig.getWarnBeforeDays()) {
            return new CertificateHealth("WARNING",
                    "Expires in " + remainingDays + " days",
                    expiry,
                    cert.getIssuerX500Principal().getName(),
                    remainingDays);
        }

        return new CertificateHealth("UP",
                "Valid for " + remainingDays + " days",
                expiry,
                cert.getIssuerX500Principal().getName(),
                remainingDays);
    }

    public record CertificateHealth(
            String status,
            String message,
            Date expiry,
            String issuer,
            Long daysRemaining
    ) {
        // For DOWN status without certificate details
        public CertificateHealth(String status, String message) {
            this(status, message, null, null, null);
        }
    }

}
