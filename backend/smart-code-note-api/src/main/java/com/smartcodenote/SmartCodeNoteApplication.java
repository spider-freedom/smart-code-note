package com.smartcodenote;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.smartcodenote.mapper")
@SpringBootApplication(scanBasePackages = "com.smartcodenote")
public class SmartCodeNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCodeNoteApplication.class, args);
    }

}
