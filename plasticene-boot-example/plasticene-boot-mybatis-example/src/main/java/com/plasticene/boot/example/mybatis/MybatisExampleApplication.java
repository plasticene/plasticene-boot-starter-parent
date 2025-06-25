package com.plasticene.boot.example.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author shepherd
 */
@SpringBootApplication
@MapperScan(basePackages = "com.plasticene.boot.example.mybatis.dao")
public class MybatisExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisExampleApplication.class, args);
    }

}
