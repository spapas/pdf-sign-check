package gr.hcg.sign;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Logger;

//@SpringBootApplication
public class Runner implements CommandLineRunner {


    @Autowired
    Signer signer;

    public static void main(String[] args) {
        System.out.println("STARTING THE APPLICATION");

        SpringApplication.run(Runner.class, args);
        System.out.println("APPLICATION FINISHED");
    }


    @Override
    public void run(String... args) throws Exception {
        System.out.println("OK");
        File documentFile = new File("sample3.pdf");
        String name = documentFile.getName();
        String substring = name.substring(0, name.lastIndexOf('.'));
        File signedDocumentFile = new File(documentFile.getParent(), substring + "_signed.pdf");
        FileInputStream fis = new FileInputStream(documentFile);
        FileOutputStream fos = new FileOutputStream(signedDocumentFile);
        signer.sign(fis, fos, null, null, null, null, null, null, null);
    }
}
