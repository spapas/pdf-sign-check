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
import java.util.Optional;


@Controller
public class SignController {

    @Value("${check.config}")
    private String checkConfig;

    @Value("${signer.apikey}")
    private String signerapikey;

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
                                   @RequestParam(value = "signName") Optional<String> signName,
                                   @RequestParam(value = "signReason") Optional<String> signReason,
                                   @RequestParam(value = "signLocation") Optional<String> signLocation,
                                   @RequestParam(value = "visibleLine1") Optional<String> visibleLine1,
                                   @RequestParam(value = "visibleLine2") Optional<String> visibleLine2,
                                   @RequestParam(value = "qrcode") Optional<String> qrcode,
                                   HttpServletResponse response ) {

        if (file.isEmpty()) {
            model.addAttribute("message", "Empty file");
            return "sign";
        }

        if(!apikey.equals(signerapikey)) {
            model.addAttribute("message", "Wrong api key");
            return "sign";
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            signer.sign(file.getInputStream(), bos, signName.orElse(null), signLocation.orElse(null), signReason.orElse(null), visibleLine1.orElse(null), visibleLine2.orElse(null), qrcode.orElse(null));

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