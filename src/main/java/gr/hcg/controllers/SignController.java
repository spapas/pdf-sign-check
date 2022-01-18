package gr.hcg.controllers;

import com.google.gson.JsonObject;
import gr.hcg.services.UploadDocumentService;
import gr.hcg.sign.Signer;
import org.eclipse.jetty.util.ajax.JSON;
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
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.UUID;


@Controller
public class SignController {

    @Value("${check.config}")
    private String checkConfig;

    @Value("${signer.apikey}")
    private String signerapikey;

    @Value("${signer.apikey}")
    private String docsUrlPrefix;

    @Autowired
    Signer signer;

    @Autowired
    UploadDocumentService uploadDocumentService;

    @GetMapping("/sign")
    public ModelAndView home(Model model) {
        model.addAttribute("message", "Please upload a pdf file to sign");
        model.addAttribute("config", checkConfig);
        return new ModelAndView("sign", model.asMap());

    }

    @PostMapping("/sign")
    public Object singleFileUpload(Model model,
                                   @RequestParam(value = "file") MultipartFile file,
                                   @RequestParam(value = "year") String year,
                                   @RequestParam(value = "protocol") String protocol,
                                   @RequestParam(value = "folder") String folder,
                                   @RequestParam(value = "apikey") String apikey,
                                   @RequestParam(value = "signName") Optional<String> signName,
                                   @RequestParam(value = "signReason") Optional<String> signReason,
                                   @RequestParam(value = "signLocation") Optional<String> signLocation,
                                   @RequestParam(value = "visibleLine1") Optional<String> visibleLine1,
                                   @RequestParam(value = "visibleLine2") Optional<String> visibleLine2,
                                   // @RequestParam(value = "qrcode") Optional<String> qrcode,
                                   HttpServletResponse response ) {

        if (file.isEmpty()) {
            model.addAttribute("message", "Empty file");
            return "sign";
        }

        if(!apikey.equals(signerapikey)) {
            model.addAttribute("message", "Wrong api key");
            return "sign";
        }
        if(year==null || protocol == null || folder == null || year.equals("") || protocol.equals("") || folder.equals("")) {
            model.addAttribute("message", "Fill year, protocol and folder");
            return "sign";
        }

        final String uuid = UUID.randomUUID().toString();

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String qrcode = docsUrlPrefix + uuid;
            signer.sign(file.getInputStream(), bos, signName.orElse(null), signLocation.orElse(null), signReason.orElse(null), visibleLine1.orElse(null), visibleLine2.orElse(null), qrcode);

            String path = uploadDocumentService.handleUpload(year, folder, protocol, uuid, bos.toByteArray());
            //model.addAttribute("message", path);
            //return "sign";

            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            JsonObject jo = new JsonObject();
            jo.addProperty("uuid", uuid);
            jo.addProperty("path", path);

            return  new ResponseEntity<>(jo.toString().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
            //final HttpHeaders headers = new HttpHeaders();
            //headers.setContentType(MediaType.APPLICATION_PDF);
            //return  new ResponseEntity<>(bos.toByteArray(), headers, HttpStatus.OK);

        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            model.addAttribute("message", "General error!");
            e.printStackTrace();
            return "sign";
        }


        //return new ModelAndView("sign", model.asMap());

        //return "home";
    }




}