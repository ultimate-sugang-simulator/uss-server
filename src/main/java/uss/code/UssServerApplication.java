package uss.code;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UssServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UssServerApplication.class, args);
    }

}
