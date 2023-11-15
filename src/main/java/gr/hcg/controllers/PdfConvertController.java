package gr.hcg.controllers;

import gr.hcg.check.PDFSignatureInfo;
import gr.hcg.check.PDFSignatureInfoParser;
import gr.hcg.views.JsonView;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.bouncycastle.tsp.TSPException;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.core.util.FileUtils;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.naming.InvalidNameException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Logger;

@Controller
public class PdfConvertController {
    Logger logger = Logger.getLogger(PdfConvertController.class.getName());
    private final OfficeManager officeManager;
    private final DocumentConverter documentConverter;


    @Value("${check.config}")
    private String checkConfig;

    public PdfConvertController() {
        logger.info("PDF CONVERT INIT");

        final LocalOfficeManager.Builder builder = LocalOfficeManager.builder();

        builder.portNumbers(Integer.parseInt("2002"));

        final String officeHomeParam = "";
        builder.officeHome(officeHomeParam);
        final String officeProfileParam = "";
        builder.templateProfileDir(officeProfileParam);

        officeManager = builder.build();
        try {
            officeManager.start();
        } catch (OfficeException e) {
            throw new RuntimeException(e);
        }
        documentConverter = LocalConverter.make(officeManager);
    }

    @GetMapping("/pdfconverter")
    public ModelAndView home(Model model) {
        model.addAttribute("message", "Please upload a file");
        model.addAttribute("config", checkConfig);

        logger.info("Total memory: " + Runtime.getRuntime().totalMemory() / 1024 / 1024);
        logger.info("Max memory: " + Runtime.getRuntime().maxMemory() / 1024 / 1024);
        logger.info("Free memory: " + Runtime.getRuntime().freeMemory() / 1024 / 1024);

        return new ModelAndView("pdfconvert", model.asMap());

    }

    @PostMapping("/pdfconverter")
    public Object fileUpload(
            Model model,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "outputFormat", required = false) final String outputFormat,
            HttpServletResponse response) {

        String outputFormatok = "pdf";
        if (file.isEmpty()) {
            model.addAttribute("message", "Empty file");
            return "pdfconverter";
        }

        if(outputFormat!=null && outputFormat.isEmpty()) {

            outputFormatok = outputFormat;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            final DocumentFormat targetFormat = DefaultDocumentFormatRegistry.getFormatByExtension(outputFormatok);
            documentConverter.convert(file.getInputStream()).to(baos).as(targetFormat).execute();

            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(targetFormat.getMediaType()));
            headers.add(
                    "Content-Disposition",
                    "attachment; filename="
                            + FileUtils.getBaseName(file.getOriginalFilename())
                            + "."
                            + targetFormat.getExtension());
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);

        } catch (OfficeException | IOException e) {
            model.addAttribute("message",                     "Could not convert the file "
                    + file.getOriginalFilename()
                    + ". Cause: "
                    + e.getMessage());
            return "pdfconverter";
        }




    }


}