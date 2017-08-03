package gr.hcg;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.InvalidNameException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.List;

@Controller
public class UploadController {

    @Value("${check.config}")
    private String checkConfig;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Please upload a pdf file");
        model.addAttribute("config", checkConfig);
        return "home";
    }

    @PostMapping("/")
    public String singleFileUpload(Model model, @RequestParam("file") MultipartFile file ) {

        if (file.isEmpty()) {
            model.addAttribute("message", "Empty file");
            return "home";
        }

        try {

            byte[] bytes = file.getBytes();
            List<PDFSignatureInfo> info = PDFSignatureInfoParser.getPDFSignatureInfo(bytes);

            model.addAttribute("message", "OK");
            model.addAttribute("pdfSignatureInfo", info);

        } catch (IOException | InvalidNameException | CertificateException| NoSuchAlgorithmException | InvalidKeyException |SignatureException | NoSuchProviderException e) {
            model.addAttribute("message", "Cannot open file: " + e.getMessage());
            e.printStackTrace();
        }

        return "home";
    }




}