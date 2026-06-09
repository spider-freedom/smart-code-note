package com.itheima.smartcodenote;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.itheima.smartcodenote.mapper")
@SpringBootApplication
public class SmartCodeNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCodeNoteApplication.class, args);
    }

}
