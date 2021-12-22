package gr.hcg;

import gr.hcg.sign.Signer;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;


@Controller
public class SignController {

    @Value("${check.config}")
    private String checkConfig;

    @Autowired
    Signer signer;

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
                                   @RequestParam(value = "signName") String signName,
                                   @RequestParam(value = "signReason") String signReason,
                                   @RequestParam(value = "signLocation") String signLocation,
                                   @RequestParam(value = "visibleLine1") String visibleLine1,
                                   @RequestParam(value = "visibleLine2") String visibleLine2,
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
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            signer.sign(file.getInputStream(), bos, signName, signLocation,signReason,visibleLine1,visibleLine2);

            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);


            return  new ResponseEntity<>(bos.toByteArray(), headers, HttpStatus.OK);

        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            model.addAttribute("message", "General error!");
            e.printStackTrace();
            return "sign";
        }


        //return new ModelAndView("sign", model.asMap());

        //return "home";
    }




}