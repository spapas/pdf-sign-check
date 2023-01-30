package gr.hcg.controllers;

import gr.hcg.check.PDFSignatureInfo;
import gr.hcg.check.PDFSignatureInfoParser;
import gr.hcg.views.JsonView;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.TextContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.bouncycastle.tsp.TSPException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.naming.InvalidNameException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@Controller
public class CheckSignatureController {
    Logger logger = Logger.getLogger(CheckSignatureController.class.getName());

    @Value("${check.config}")
    private String checkConfig;

    @GetMapping("/")
    public ModelAndView home(Model model) {
        model.addAttribute("message", "Please upload a pdf file");
        model.addAttribute("config", checkConfig);

        logger.info("Total memory: " + Runtime.getRuntime().totalMemory() / 1024 / 1024);
        logger.info("Max memory: " + Runtime.getRuntime().maxMemory() / 1024 / 1024);
        logger.info("Free memory: " + Runtime.getRuntime().freeMemory() / 1024 / 1024);

        return new ModelAndView("home", model.asMap());

        //return "home";
    }

    @PostMapping("/")
    public Object singleFileUpload(
            Model model,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "json", required = false) String json,
            @RequestParam(value = "contents", required = false) String contents,
            HttpServletResponse response) {

        if (file.isEmpty()) {
            model.addAttribute("message", "Empty file");
            return "home";
        }

        try {

            byte[] bytes = file.getBytes();
            List<PDFSignatureInfo> info = PDFSignatureInfoParser.getPDFSignatureInfo(bytes);

            model.addAttribute("message", "OK");
            model.addAttribute("filename", file.getOriginalFilename());
            model.addAttribute("pdfSignatureInfo", info);

            if (contents != null && contents.equals("on")) {
                try {
                    AutoDetectParser parser = new AutoDetectParser();
                    //ContentHandler handler = new ToXMLContentHandler();
                    ContentHandler handler = new BodyContentHandler();
                    Metadata metadata = new Metadata();
                    parser.parse(file.getInputStream(), handler, metadata);
                    String res = handler.toString();
                    model.addAttribute("contents", res);

                } catch (TikaException te) {
                    te.printStackTrace();
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }

            }

        } catch (IOException | InvalidNameException | CertificateException | NoSuchAlgorithmException |
                 InvalidKeyException | SignatureException | NoSuchProviderException | TSPException e) {
            model.addAttribute("message", "Cannot open file: " + e.getMessage());
            e.printStackTrace();
            logger.info(e.getMessage());
        }

        if (json != null && json.equals("on")) {
            return JsonView.Render(model, response);
        }

        return new ModelAndView("home", model.asMap());

        //return "home";
    }


}