package gr.hcg.sign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Component
public class Signer {

    @Value("${signer.keystore.pin}")
    public String keystorePin;

    @Value("${signer.keystore.name}")
    public String keystoreName;

    @Value("${signer.tsaurl}")
    public String tsaUrl;

    public static byte[] readBytes(InputStream is ) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();

    }

    public static void setIfNotNull(CreateVisibleSignatureMem signing, String signName, String signLocation, String signReason, String visibleLine1, String visibleLine2) {

        if(signName!=null) {
            signing.signatureName = signName;
        }
        if(signLocation!=null) {
            signing.signatureLocation = signLocation;
        }
        if(signReason!=null) {
            signing.signatureReason = signReason;
        }
        if(visibleLine1!=null) {
            signing.visibleLine1 = visibleLine1;
        }
        if(visibleLine2!=null) {
            signing.visibleLine2 = visibleLine2;
        }
    }


    public void sign(InputStream is, OutputStream os, String signName, String signLocation, String signReason, String visibleLine1, String visibleLine2) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {

        InputStream ksInputStream = CreateVisibleSignatureMem.class.getClassLoader().getResourceAsStream(keystoreName);

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        char[] pin = keystorePin.toCharArray();
        keystore.load(ksInputStream, pin);

        CreateVisibleSignatureMem signing = new CreateVisibleSignatureMem(keystore, pin.clone());
        setIfNotNull(signing, signName, signLocation, signReason, visibleLine1, visibleLine2);

        InputStream imageResource = CreateVisibleSignatureMem.class.getClassLoader().getResourceAsStream("hcg.png");
        signing.setImageBytes(readBytes(imageResource));

        // Set the signature rectangle top - left - width - height
        Rectangle2D humanRect = new Rectangle2D.Float(5, 5, 150, 40);

        signing.signPDF(is, os, humanRect, tsaUrl, "Signature1");
    }

}
