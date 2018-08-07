package com.xy.sample.cloud.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by xiaoyao9184 on 2018/8/6.
 */
@SpringBootApplication
public class ZuulApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ZuulApplication.class);
        app.run(args);
    }

}
