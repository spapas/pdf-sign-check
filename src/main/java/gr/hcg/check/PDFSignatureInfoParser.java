package gr.hcg.check;

/**
 * Created by serafeim on 13/7/2017.
 *
 * Some original code was copied from PDFBox examples:
 * https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/signature/ShowSignature.java?revision=1797969&view=markup
 * Author: Ben Litchfield
 */

import java.io.*;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.*;
import java.text.SimpleDateFormat;
import java.util.*;

import gr.hcg.sign.SigUtils;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.CollectionStore;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.StoreException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;


public class PDFSignatureInfoParser {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    /**
     * This is the entry point for the application.
     *
     * @param args The command-line arguments.
     *
     * @throws IOException If there is an error reading the file.
     * @throws CertificateException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.InvalidKeyException
     * @throws java.security.NoSuchProviderException
     * @throws java.security.SignatureException
     */
    public static void main(String[] args) throws IOException, CertificateException,
            NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException, InvalidNameException, TSPException {
        PDFSignatureInfoParser show = new PDFSignatureInfoParser();
        List<PDFSignatureInfo> pdfSignatureInfo = show.getPDFSignatureInfo(new FileInputStream(new File("..\\s1.pdf")));
        for(PDFSignatureInfo psi: pdfSignatureInfo) {
            System.out.println(psi);
        }
    }

    private static byte[] getbyteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > -1 ) {
            baos.write(buffer, 0, len);
        }
        baos.flush();

        return baos.toByteArray();
        //InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
    }


    public static List<PDFSignatureInfo> getPDFSignatureInfo(InputStream is ) throws IOException, CertificateException,
            NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException, InvalidNameException, TSPException {

        byte[] byteArray = getbyteArray(is);
        return getPDFSignatureInfo(byteArray);
    }

    public static List<PDFSignatureInfo> getPDFSignatureInfo(byte[] byteArray ) throws IOException, CertificateException,
            NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException, InvalidNameException, TSPException {

        List<PDFSignatureInfo> lpsi = new ArrayList<PDFSignatureInfo>();

        // Try to open the input file as PDF
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(byteArray))) {
            // Get Signature dictionaries of PDF
            for (PDSignature sig : document.getSignatureDictionaries()) {

                PDFSignatureInfo psi = new PDFSignatureInfo();
                lpsi.add(psi);

                COSDictionary sigDict = sig.getCOSObject();
                COSString contents = (COSString) sigDict.getDictionaryObject(COSName.CONTENTS);


                Set<Map.Entry<COSName, COSBase>> entries = sigDict.entrySet();
                for(Map.Entry<COSName, COSBase> entry: entries) {
                    // Don't return contents
                    if(!entry.getKey().equals(COSName.CONTENTS)) {
                        psi.entries.put(entry.getKey().getName(), entry.getValue().toString());
                    }
                }

                psi.reason = sig.getReason();
                psi.name = sig.getName();
                psi.signDate = sig.getSignDate().getTime();
                psi.subFilter= sig.getSubFilter();
                psi.contactInfo = sig.getContactInfo();
                psi.filter = sig.getFilter();
                psi.location = sig.getLocation();

                // download the signed content
                byte[] buf;
                buf = sig.getSignedContent(new ByteArrayInputStream(byteArray));

                int[] byteRange = sig.getByteRange();
                if (byteRange.length != 4) {
                    throw new IOException("Signature byteRange must have 4 items");
                } else {
                    long fileLen = byteArray.length;
                    long rangeMax = byteRange[2] + (long) byteRange[3];
                    // multiply content length with 2 (because it is in hex in the PDF) and add 2 for < and >
                    int contentLen = sigDict.getString(COSName.CONTENTS).length() * 2 + 2;
                    if (fileLen != rangeMax || byteRange[0] != 0 || byteRange[1] + contentLen != byteRange[2]) {
                        // a false result doesn't necessarily mean that the PDF is a fake
                        // System.out.println("Signature does not cover whole document");
                        psi.coversWholeDocument = false;
                    } else {
                        //System.out.println("Signature covers whole document");
                        psi.coversWholeDocument = true;
                    }
                }

                String subFilter = sig.getSubFilter();
                if (subFilter != null) {
                    switch (subFilter) {
                        case "adbe.pkcs7.detached":
                        case "ETSI.CAdES.detached":
                            verifyPKCS7(buf, contents, sig, psi);

                            //TODO check certificate chain, revocation lists, timestamp...
                            break;
                        case "adbe.pkcs7.sha1": {
                            // example: PDFBOX-1452.pdf
                            COSString certString = (COSString) sigDict.getDictionaryObject( COSName.CONTENTS);
                            byte[] certData = certString.getBytes();
                            CertificateFactory factory = CertificateFactory.getInstance("X.509");
                            ByteArrayInputStream certStream = new ByteArrayInputStream(certData);
                            Collection<? extends Certificate> certs = factory.generateCertificates(certStream);
                            //System.out.println("certs=" + certs);
                            byte[] hash = MessageDigest.getInstance("SHA1").digest(buf);
                            verifyPKCS7(hash, contents, sig, psi);

                            //TODO check certificate chain, revocation lists, timestamp...
                            break;
                        }
                        case "adbe.x509.rsa_sha1": {
                            // example: PDFBOX-2693.pdf
                            COSString certString = (COSString) sigDict.getDictionaryObject( COSName.getPDFName("Cert"));
                            byte[] certData = certString.getBytes();
                            CertificateFactory factory = CertificateFactory.getInstance("X.509");
                            ByteArrayInputStream certStream = new ByteArrayInputStream(certData);
                            Collection<? extends Certificate> certs = factory.generateCertificates(certStream);
                            //System.out.println("certs=" + certs);

                            //TODO verify signature
                            psi.signatureVerified="Unable to verify adbe.x509.rsa_sha1 subfilter";
                            break;

                        }
                        default:
                            throw new IOException("Unknown certificate type " + subFilter);

                    }
                } else {
                    throw new IOException("Missing subfilter for cert dictionary");
                }
            }
        } catch (CMSException | OperatorCreationException ex) {
            throw new IOException(ex);
        }

        return lpsi;
    }

    /**
     * Verify a PKCS7 signature.
     *
     * @param byteArray the byte sequence that has been signed
     * @param contents the /Contents field as a COSString
     * @param sig the PDF signature (the /V dictionary)
     * @throws CertificateException
     * @throws CMSException
     * @throws StoreException
     * @throws OperatorCreationException
     */
    private static void verifyPKCS7(byte[] byteArray, COSString contents, PDSignature sig, PDFSignatureInfo psi)
            throws CMSException, CertificateException, StoreException, OperatorCreationException,
            NoSuchAlgorithmException, NoSuchProviderException, InvalidNameException, IOException, TSPException {
        // inspiration:
        // http://stackoverflow.com/a/26702631/535646
        // http://stackoverflow.com/a/9261365/535646
        CMSProcessable signedContent = new CMSProcessableByteArray(byteArray);
        CMSSignedData signedData = new CMSSignedData(signedContent, contents.getBytes());
        Store certificatesStore = signedData.getCertificates();
        Collection<SignerInformation> signers = signedData.getSignerInfos().getSigners();
        SignerInformation signerInformation = signers.iterator().next();
        Collection matches = certificatesStore.getMatches(signerInformation.getSID());
        X509CertificateHolder certificateHolder = (X509CertificateHolder) matches.iterator().next();
        X509Certificate certFromSignedData = new JcaX509CertificateConverter().getCertificate(certificateHolder);

        checkTSA(psi, certificatesStore, signerInformation);

        CertificateInfo ci = new CertificateInfo();
        psi.certificateInfo = ci;
        ci.issuerDN = certFromSignedData.getIssuerDN().toString();
        ci.subjectDN = certFromSignedData.getSubjectDN().toString();

        ci.notValidAfter = certFromSignedData.getNotAfter();
        ci.notValidBefore = certFromSignedData.getNotBefore();

        ci.signAlgorithm = certFromSignedData.getSigAlgName();
        ci.serial = certFromSignedData.getSerialNumber().toString();

        LdapName ldapDN = new LdapName(ci.issuerDN);
        for(Rdn rdn: ldapDN.getRdns()) {
            ci.issuerOIDs.put(rdn.getType(), rdn.getValue().toString());
        }

        ldapDN = new LdapName(ci.subjectDN);
        for(Rdn rdn: ldapDN.getRdns()) {
            ci.subjectOIDs.put(rdn.getType(), rdn.getValue().toString());
        }

        certFromSignedData.checkValidity(sig.getSignDate().getTime());


        if (isSelfSigned(certFromSignedData)) {
            //System.err.println("Certificate is self-signed, LOL!");
            psi.isSelfSigned = true;
        } else {
            //System.out.println("Certificate is not self-signed");
            psi.isSelfSigned = false;
            // todo rest of chain
        }

        if (signerInformation.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certFromSignedData))) {
            //System.out.println("Signature verified");
            psi.signatureVerified="YES";
        } else {
            //System.out.println("Signature verification failed");
            psi.signatureVerified="NO";
        }
    }

    private static void checkTSA(PDFSignatureInfo psi, Store certificatesStore, SignerInformation signerInformation) throws CMSException, IOException, TSPException, CertificateException, OperatorCreationException, NoSuchAlgorithmException {
        TimeStampToken timeStampToken = SigUtils.extractTimeStampTokenFromSignerInformation(signerInformation);
        if(timeStampToken==null) {
            psi.hasTsaToken = false;
        } else {
            psi.hasTsaToken = true;
        }

        if (timeStampToken != null) {
            // tested with QV_RCA1_RCA3_CPCPS_V4_11.pdf
            // https://www.quovadisglobal.com/~/media/Files/Repository/QV_RCA1_RCA3_CPCPS_V4_11.ashx
            // also 021496.pdf and 036351.pdf from digitalcorpora
            gr.hcg.check2.SigUtils.validateTimestampToken(timeStampToken);
            @SuppressWarnings("unchecked") // TimeStampToken.getSID() is untyped
            Collection<X509CertificateHolder> tstMatches =
                    timeStampToken.getCertificates().getMatches(timeStampToken.getSID());
            X509CertificateHolder tstCertHolder = tstMatches.iterator().next();
            X509Certificate certFromTimeStamp = new JcaX509CertificateConverter().getCertificate(tstCertHolder);
            // merge both stores using a set to remove duplicates
            HashSet<X509CertificateHolder> certificateHolderSet = new HashSet<X509CertificateHolder>();
            certificateHolderSet.addAll(certificatesStore.getMatches(null));
            certificateHolderSet.addAll(timeStampToken.getCertificates().getMatches(null));
            /*
            try {
                gr.hcg.check2.SigUtils.verifyCertificateChain(new CollectionStore<X509CertificateHolder>(certificateHolderSet),
                        certFromTimeStamp,
                        timeStampToken.getTimeStampInfo().getGenTime());
            } catch(Exception e) {
                throw new TSPException("Error while verifying TSA");
            }

             */
            gr.hcg.check2.SigUtils.checkTimeStampCertificateUsage(certFromTimeStamp);

            // compare the hash of the signature with the hash in the timestamp
            byte[] tsMessageImprintDigest = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
            String hashAlgorithm = timeStampToken.getTimeStampInfo().getMessageImprintAlgOID().getId();
            byte[] sigMessageImprintDigest = MessageDigest.getInstance(hashAlgorithm).digest(signerInformation.getSignature());
            if (Arrays.equals(tsMessageImprintDigest, sigMessageImprintDigest)) {
                // Happy path...
            } else {
                throw new TSPException("Error while verifying TSA");
            }
        }
    }

    // https://svn.apache.org/repos/asf/cxf/tags/cxf-2.4.1/distribution/src/main/release/samples/sts_issue_operation/src/main/java/demo/sts/provider/cert/CertificateVerifier.java

    /**
     * Checks whether given X.509 certificate is self-signed.
     */
    private static boolean isSelfSigned(X509Certificate cert) throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (SignatureException | InvalidKeyException sigEx) {
            return false;
        }
    }

    private static boolean isRevoked(Certificate cert) throws CertificateException, IOException, CRLException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        String crlURLString = "http://crl.ermis.gov.gr/HPARCAPServants/LatestCRL.crl";
        URL crlURL = new URL(crlURLString);
        InputStream crlStream = crlURL.openStream();
        X509CRL crl = (X509CRL)certFactory.generateCRL(crlStream);
        return crl.isRevoked(cert);

    }


}
