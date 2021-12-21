package gr.hcg;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.naming.InvalidNameException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.List;

@Controller
public class SignController {

    @Value("${check.config}")
    private String checkConfig;

    @GetMapping("/sign")
    public ModelAndView home(Model model) {
        model.addAttribute("message", "Please upload a pdf file to sign");
        model.addAttribute("config", checkConfig);

        return new ModelAndView("sign", model.asMap());

    }

    @PostMapping("/sign")
    public Object singleFileUpload(Model model,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "apikey") String apikey,
                                   HttpServletResponse response ) {

        if (file.isEmpty()) {
            model.addAttribute("message", "Empty file");
            return "sign";
        }

        if(!apikey.equals("123")) {
            model.addAttribute("message", "Wrong api key");
            return "sign";
        }

        try {

            byte[] bytes = file.getBytes();
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            return  new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            model.addAttribute("message", "Error!");
            e.printStackTrace();
            return "sign";
        }


        //return new ModelAndView("sign", model.asMap());

        //return "home";
    }




}