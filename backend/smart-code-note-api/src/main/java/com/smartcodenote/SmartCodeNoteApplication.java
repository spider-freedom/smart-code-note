package com.smartcodenote;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@MapperScan("com.smartcodenote.mapper")
@SpringBootApplication(scanBasePackages = "com.smartcodenote")
@EnableCaching
public class SmartCodeNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCodeNoteApplication.class, args);
    }

}
