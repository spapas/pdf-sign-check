package gr.hcg;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;

@Controller
@EnableAutoConfiguration
public class UploadController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Please upload a pdf file");
        return "home";
    }

    @PostMapping("/")
    public String singleFileUpload(Model model, @RequestParam("file") MultipartFile file ) {

        if (file.isEmpty()) {
            model.addAttribute("message", "Empty file");
            return "home";
        }

        try {

            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            CertInfo.showSignature(bytes);

            model.addAttribute("message", "OK");

        } catch (IOException | CertificateException| NoSuchAlgorithmException | InvalidKeyException |SignatureException | NoSuchProviderException e) {
            model.addAttribute("message", e.getMessage());
            e.printStackTrace();
        }

        return "home";
    }


    public static void main(String[] args) throws Exception {
        SpringApplication.run(UploadController.class, args);
    }

}