package gr.hcg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.useSystemProxies", "true");
        SpringApplication.run(Application.class, args);
    }
}
