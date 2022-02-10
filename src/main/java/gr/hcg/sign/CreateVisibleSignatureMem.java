package gr.hcg.sign;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.fixup.PDDocumentFixup;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Hex;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * This is a second example for visual signing a pdf. It doesn't use the "design pattern" influenced
 * PDVisibleSignDesigner, and doesn't create its complex multilevel forms described in the Adobe
 * document
 * <a href="https://www.adobe.com/content/dam/acom/en/devnet/acrobat/pdfs/PPKAppearances.pdf">Digital
 * Signature Appearances</a>, because this isn't required by the PDF specification. See the
 * discussion in December 2017 in PDFBOX-3198.
 *
 * @author Vakhtang Koroghlishvili
 * @author Tilman Hausherr
 */

public class CreateVisibleSignatureMem extends CreateSignatureBase
{
    private SignatureOptions signatureOptions;
    private byte[] imageBytes;

    public String signatureName = "AUTO SIGNATURE";
    public String signatureLocation = "PIRAEUS, GREECE";
    public String signatureReason = "IDENTICAL COPY";
    public String visibleLine1 = "DIGITALLY SIGNED";
    public String visibleLine2 = "Ministry of Maritime and Insular Policy";
    public String uuid = "123e4567-e89b-12d3-a456-426614174000";
    public String qrcode = "http://docs.hcg.gr/validate/123e4567-e89b-12d3-a456-426614174000";

    private Calendar signDate = null;

    /**
     * Initialize the signature creator with a keystore (pkcs12) and pin that
     * should be used for the signature.
     *
     * @param keystore is a pkcs12 keystore.
     * @param pin is the pin for the keystore / private key
     * @throws KeyStoreException if the keystore has not been initialized (loaded)
     * @throws NoSuchAlgorithmException if the algorithm for recovering the key cannot be found
     * @throws UnrecoverableKeyException if the given password is wrong
     * @throws UnrecoverableKeyException if the given password is wrong
     * @throws CertificateException if the certificate is not valid as signing time
     * @throws IOException if no certificate could be found
     */
    public CreateVisibleSignatureMem(KeyStore keystore, char[] pin)
            throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, CertificateException
    {
        super(keystore, pin);
    }

    public byte[] getImageBytes()
    {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes)
    {
        this.imageBytes = imageBytes;
    }


    /**
     * Sign pdf file and create new file that ends with "_signed.pdf".
     *
     * @param inputStream The source pdf document file.
     * @param signedStream The file to be signed.
     * @param tsaUrl optional TSA url
     * @param signatureFieldName optional name of an existing (unsigned) signature field
     * @throws IOException
     */
    public void signPDF(InputStream inputStream, OutputStream signedStream, String tsaUrl, String signatureFieldName) throws IOException
    {
        setTsaUrl(tsaUrl);

        try (PDDocument doc = PDDocument.load(inputStream))
        {
            int accessPermissions = SigUtils.getMDPPermission(doc);
            if (accessPermissions == 1)
            {
                throw new IllegalStateException("No changes to the document are permitted due to DocMDP transform parameters dictionary");
            }
            // Note that PDFBox has a bug that visual signing on certified files with permission 2
            // doesn't work properly, see PDFBOX-3699. As long as this issue is open, you may want to
            // be careful with such files.

            PDSignature signature = null;
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            PDRectangle rect = null;

            PDShrink.shrinkFirstpage(doc);

            // sign a PDF with an existing empty signature, as created by the CreateEmptySignatureForm example.
            if (acroForm != null)
            {
                signature = findExistingSignature(acroForm, signatureFieldName);
                if (signature != null)
                {
                    rect = acroForm.getField(signatureFieldName).getWidgets().get(0).getRectangle();
                }
            }

            if (signature == null)
            {
                // create signature dictionary
                signature = new PDSignature();
            }

            if (rect == null)
            {
                float width = doc.getPage(0).getMediaBox().getWidth();
                Rectangle2D humanRect = new Rectangle2D.Float(0, 0, width, 120);
                rect = createSignatureRectangle(doc, humanRect);
            }

            // Optional: certify
            // can be done only if version is at least 1.5 and if not already set
            // doing this on a PDF/A-1b file fails validation by Adobe preflight (PDFBOX-3821)
            // PDF/A-1b requires PDF version 1.4 max, so don't increase the version on such files.
            if (doc.getVersion() >= 1.5f && accessPermissions == 0)
            {
                SigUtils.setMDPPermission(doc, signature, 2);
            }

            if (acroForm != null && acroForm.getNeedAppearances())
            {
                // PDFBOX-3738 NeedAppearances true results in visible signature becoming invisible
                // with Adobe Reader
                if (acroForm.getFields().isEmpty())
                {
                    // we can safely delete it if there are no fields
                    acroForm.getCOSObject().removeItem(COSName.NEED_APPEARANCES);
                    // note that if you've set MDP permissions, the removal of this item
                    // may result in Adobe Reader claiming that the document has been changed.
                    // and/or that field content won't be displayed properly.
                    // ==> decide what you prefer and adjust your code accordingly.
                }
                else
                {
                    System.out.println("/NeedAppearances is set, signature may be ignored by Adobe Reader");
                }
            }

            // default filter
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);

            // subfilter for basic and PAdES Part 2 signatures
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);

            signature.setName(this.signatureName);
            signature.setLocation(this.signatureLocation);
            signature.setReason(this.signatureReason);

            this.signDate = Calendar.getInstance();
            // the signing date, needed for valid signature
            signature.setSignDate(this.signDate);

            // do not set SignatureInterface instance, if external signing used
            SignatureInterface signatureInterface = isExternalSigning() ? null : this;

            // register signature dictionary and sign interface
            signatureOptions = new SignatureOptions();
            signatureOptions.setPreferredSignatureSize(8192*2);
            signatureOptions.setVisualSignature(createVisualSignatureTemplate(doc, 0, rect));
            signatureOptions.setPage(0);
            doc.addSignature(signature, signatureInterface, signatureOptions);

            // write incremental (only for signing purpose)
            doc.saveIncremental(signedStream);

        }

        // Do not close signatureOptions before saving, because some COSStream objects within
        // are transferred to the signed document.
        // Do not allow signatureOptions get out of scope before saving, because then the COSDocument
        // in signature options might by closed by gc, which would close COSStream objects prematurely.
        // See https://issues.apache.org/jira/browse/PDFBOX-3743
        IOUtils.closeQuietly(signatureOptions);
    }

    private PDRectangle createSignatureRectangle(PDDocument doc, Rectangle2D humanRect)
    {
        float x = (float) humanRect.getX();
        float y = (float) humanRect.getY();
        float width = (float) humanRect.getWidth();
        float height = (float) humanRect.getHeight();
        PDPage page = doc.getPage(0);
        PDRectangle pageRect = page.getCropBox();
        PDRectangle rect = new PDRectangle();
        // signing should be at the same position regardless of page rotation.
        switch (page.getRotation())
        {
            case 90:
                rect.setLowerLeftY(x);
                rect.setUpperRightY(x + width);
                rect.setLowerLeftX(y);
                rect.setUpperRightX(y + height);
                break;
            case 180:
                rect.setUpperRightX(pageRect.getWidth() - x);
                rect.setLowerLeftX(pageRect.getWidth() - x - width);
                rect.setLowerLeftY(y);
                rect.setUpperRightY(y + height);
                break;
            case 270:
                rect.setLowerLeftY(pageRect.getHeight() - x - width);
                rect.setUpperRightY(pageRect.getHeight() - x);
                rect.setLowerLeftX(pageRect.getWidth() - y - height);
                rect.setUpperRightX(pageRect.getWidth() - y);
                break;
            case 0:
            default:
                rect.setLowerLeftX(x);
                rect.setUpperRightX(x + width);
                //rect.setLowerLeftY(pageRect.getHeight() - y - height);
                //rect.setUpperRightY(pageRect.getHeight() - y);
                rect.setLowerLeftY(y);
                rect.setUpperRightY(y + height);
                break;
        }
        return rect;
    }

    public static byte[] generateQRcode(String data, int h, int w) throws IOException, WriterException {
//the BitMatrix class represents the 2D matrix of bits
//MultiFormatWriter is a factory class that finds the appropriate Writer subclass for the BarcodeFormat requested and encodes the barcode with the supplied contents.
        BitMatrix matrix = new MultiFormatWriter().encode(new String(data.getBytes("utf-8"), "utf-8"), BarcodeFormat.QR_CODE, w, h);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "png", bos);
        //MatrixToImageWriter.writeToFile(matrix, path.substring(path.lastIndexOf('.') + 1), new File(path));
        return bos.toByteArray();
    }

    // create a template PDF document with empty signature and return it as a stream.
    private InputStream createVisualSignatureTemplate(PDDocument srcDoc, int pageNum, PDRectangle rect) throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage(srcDoc.getPage(pageNum).getMediaBox());
            doc.addPage(page);
            PDAcroForm acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            List<PDField> acroFormFields = acroForm.getFields();
            acroForm.setSignaturesExist(true);
            acroForm.setAppendOnly(true);
            acroForm.getCOSObject().setDirect(true);
            acroFormFields.add(signatureField);

            widget.setRectangle(rect);

            // from PDVisualSigBuilder.createHolderForm()
            PDStream stream = new PDStream(doc);
            PDFormXObject form = new PDFormXObject(stream);
            PDResources res = new PDResources();
            form.setResources(res);
            form.setFormType(1);
            PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
            float height = bbox.getHeight();
            Matrix initialScale = null;
            switch (srcDoc.getPage(pageNum).getRotation())
            {
                case 90:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(1));
                    initialScale = Matrix.getScaleInstance(bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
                    height = bbox.getWidth();
                    break;
                case 180:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(2));
                    break;
                case 270:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(3));
                    initialScale = Matrix.getScaleInstance(bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
                    height = bbox.getWidth();
                    break;
                case 0:
                default:
                    break;
            }
            form.setBBox(bbox);
            //PDFont font = PDType1Font.HELVETICA_BOLD;

            InputStream fontInputStream = CreateVisibleSignatureMem.class.getClassLoader().getResourceAsStream("calibri.ttf");
            PDFont font = PDType0Font.load(doc, fontInputStream);

            // from PDVisualSigBuilder.createAppearanceDictionary()
            PDAppearanceDictionary appearance = new PDAppearanceDictionary();
            appearance.getCOSObject().setDirect(true);
            PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
            appearance.setNormalAppearance(appearanceStream);
            widget.setAppearance(appearance);
            float w = appearanceStream.getBBox().getWidth();
            float h = appearanceStream.getBBox().getHeight();
            try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream))
            {
                // for 90Â° and 270Â° scale ratio of width / height
                // not really sure about this
                // why does scale have no effect when done in the form matrix???
                if (initialScale != null)
                {
                    cs.transform(initialScale);
                }

                // show background (just for debugging, to see the rect size + position)
                //cs.setNonStrokingColor(new Color(.95f,.95f,.95f));
                //cs.addRect(-5000, -5000, 10000, 10000);
                //cs.fill();
                addHeader(cs, w, h, font);
                cs.saveGraphicsState();

                addFooter(cs, w, h, srcDoc);
                addLeftPart(doc, qrcode, uuid, cs, font,  w, h);
                addCenterPart(cs, w, h, font, this.signDate);

                addRightPart(cs, font, w, h, this.signDate, this.visibleLine1, this.visibleLine2);
                addCenterOverlay(cs, w, h, doc, imageBytes);

            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }

    private static void addHeader(PDPageContentStream cs, float w, float h, PDFont font) throws IOException {
        cs.setNonStrokingColor(Color.BLACK);

        cs.addRect(10, h-8, w/3, 5);
        cs.fill();

        float fontSize = 10;

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(Color.black);

        cs.newLineAtOffset(w/3 + 40, h-8);
        cs.showText("Ψηφιακή βεβαίωση εγγράφου");
        cs.endText();
        cs.addRect(2*w/3, h-8, w/3-10, 5);
        cs.fill();
    }

    private static void addFooter(PDPageContentStream cs, float w, float h, PDDocument srcDoc) throws IOException {

        cs.beginText();
        cs.newLineAtOffset(w/2 - 30, 20);
        cs.showText("Σελίδα 1 από " + srcDoc.getNumberOfPages());
        cs.endText();
    }

    private static void addCenterPart(PDPageContentStream cs, float w, float h, PDFont font, Calendar signDate) throws IOException {

        cs.beginText();
        cs.setFont(font, 7);
        cs.newLineAtOffset(w/2-40, h-40);
        cs.showText("Επιβεβαιώνεται το γνήσιο. Υπουργείο Ναυτιλίας");
        cs.newLine();
        cs.showText("και Νησιωτικής Πολιτικής / Verified by the ");
        cs.newLine();
        cs.showText("Ministry of Maritime Affairs and Insular Policy");
        cs.newLine();
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyMMddHHmmssSZ");
        cs.showText(sdf2.format(signDate.getTime()));
        cs.endText();

    }

    private static void addCenterOverlay(PDPageContentStream cs, float w, float h, PDDocument doc, byte[] imageBytes) throws IOException {

        float alpha = 0.2f;
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setStrokingAlphaConstant(alpha);
        graphicsState.setNonStrokingAlphaConstant(alpha);
        cs.setGraphicsStateParameters(graphicsState);

        PDImageXObject img = PDImageXObject.createFromByteArray(doc, imageBytes, null);
        img.setHeight(30);
        img.setWidth(35);
        cs.drawImage(img, w/2, h/2);
        cs.restoreGraphicsState();
    }

    private static void addLeftPart(PDDocument doc, String qrcode, String uuid, PDPageContentStream cs, PDFont font, float w, float h) throws IOException {

        cs.transform(Matrix.getScaleInstance(0.5f, 0.5f));
        try {
            byte[] qrbytes = generateQRcode(qrcode, 150, 150);
            PDImageXObject qrimg = PDImageXObject.createFromByteArray(doc, qrbytes, null);
            cs.drawImage(qrimg, 2*w/3 - 50, h / 2);
        } catch(WriterException e) {
            e.printStackTrace();
        }

        cs.restoreGraphicsState();

        cs.beginText();
        cs.setLeading(10);
        cs.setFont(font, 8);
        cs.newLineAtOffset(10, h-30);
        cs.showText("Μπορείτε να ελέγξετε την ισχύ του εγγράφου");
        cs.newLine();
        cs.showText("σκανάροντας το QR code ή εισάγοντας τον κωδικό");
        cs.newLine();
        cs.showText("στο docs.hcg.gr/validate");
        cs.newLine();
        cs.newLine();

        cs.setFont(font, 9);
        cs.showText("Κωδικός εγγράφου:");
        cs.newLine();
        cs.showText(uuid);
        cs.endText();

    }

    private static void addRightPart(PDPageContentStream cs, PDFont font, float w, float h, Calendar signDate, String visibleLine1, String visibleLine2) throws IOException {
        float fontSize = 9f;
        cs.setFont(font, fontSize);
        showTextRight(cs, font, "Υπογραφή από:", w, h-40, fontSize);
        showTextRight(cs, font, visibleLine1, w, h-50, fontSize);
        showTextRight(cs, font, visibleLine2, w, h-60, fontSize);
        showTextRight(cs, font, "Ημερομηνία υπογραφής:", w, h-70, fontSize);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        showTextRight(cs, font, sdf.format(signDate.getTime()), w, h-80, fontSize);
    }

    private static void showTextRight(PDPageContentStream cs, PDFont font, String text, float w, float y, float fontSize ) throws IOException {
        cs.beginText();
        float xoffset = w - font.getStringWidth(text) / 1000 * fontSize - 15;
        cs.newLineAtOffset(xoffset, y);
        cs.setNonStrokingColor(Color.black);
        cs.showText(text);
        cs.endText();
    }

    // Find an existing signature (assumed to be empty). You will usually not need this.
    private PDSignature findExistingSignature(PDAcroForm acroForm, String sigFieldName)
    {
        PDSignature signature = null;
        PDSignatureField signatureField;
        if (acroForm != null)
        {
            signatureField = (PDSignatureField) acroForm.getField(sigFieldName);
            if (signatureField != null)
            {
                // retrieve signature dictionary
                signature = signatureField.getSignature();
                if (signature == null)
                {
                    signature = new PDSignature();
                    // after solving PDFBOX-3524
                    // signatureField.setValue(signature)
                    // until then:
                    signatureField.getCOSObject().setItem(COSName.V, signature);
                }
                else
                {
                    throw new IllegalStateException("The signature field " + sigFieldName + " is already signed.");
                }
            }
        }
        return signature;
    }

}