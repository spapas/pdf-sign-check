package gr.hcg;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.apache.pdfbox.util.Matrix;

import java.awt.print.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class PDPrn {
    public static void main(String[] args) {
        try (PDDocument pdf = PDDocument.load(new File("foo.pdf"))) {
            PDPageTree tree = pdf.getDocumentCatalog().getPages();
            Iterator<PDPage> iterator = tree.iterator();
            while (iterator.hasNext()) {
                PDPage page = iterator.next();

                PDPageContentStream contentStream = new PDPageContentStream(pdf, page,
                        PDPageContentStream.AppendMode.PREPEND, false);
                //contentStream.transform(Matrix.getScaleInstance(0.8f, 0.8f));
                contentStream.transform(new Matrix(0.9f, 0, 0, 0.9f, 25,75));
                contentStream.close();
                //page.setMediaBox(PDRectangle.LETTER);
                //page.setMediaBox(PDRectangle.A4);


            }
            pdf.save("foo2.pdf");

            /*
            Paper paper = new Paper();
            paper.setSize(612, 792);
            paper.setImageableArea(0, 0, 612, 792);
            PageFormat pageFormat = new PageFormat();
            pageFormat.setPaper(paper);
            Book book = new Book();
            book.append(new PDFPrintable(pdf, Scaling.SHRINK_TO_FIT), pageFormat, pdf.getNumberOfPages());
            job.setPageable(book);
            try {
                job.print();
            } catch (PrinterException pe) {
                pe.printStackTrace();
            }*/

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}