package com.sheji;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sheji.mapper")
public class ShejiDiaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShejiDiaryApplication.class, args);
    }

}
