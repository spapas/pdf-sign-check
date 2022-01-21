package gr.hcg.sign;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.util.Matrix;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class PDShrink {
    public static void shrinkFirstpage(PDDocument pdf) throws IOException {
        PDPageTree tree = pdf.getDocumentCatalog().getPages();
        PDPage page = tree.get(0);

        PDPageContentStream contentStream = new PDPageContentStream(pdf, page, PDPageContentStream.AppendMode.PREPEND, false);

        contentStream.transform(new Matrix(0.9f, 0, 0, 0.9f, 25,75));
        contentStream.close();

    }

    public static void shrinkAllPages(PDDocument pdf) throws IOException {
        PDPageTree tree = pdf.getDocumentCatalog().getPages();

        Iterator<PDPage> iterator = tree.iterator();
        while (iterator.hasNext()) {
            PDPage page = iterator.next();
            PDPageContentStream contentStream = new PDPageContentStream(pdf, page, PDPageContentStream.AppendMode.PREPEND, false);
            contentStream.transform(new Matrix(0.9f, 0, 0, 0.9f, 25, 75));
            contentStream.close();
        }
    }


    public static void main(String[] args) {
        try (PDDocument pdf = PDDocument.load(new File("foo.pdf"))) {
            shrinkAllPages(pdf);
            pdf.save("foo2.pdf");

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}