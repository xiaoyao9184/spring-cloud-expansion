package com.xy.sample.cloud.zuul.redirect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * Created by xiaoyao9184 on 2018/4/25.
 */
@SpringBootApplication
@EnableZuulProxy
public class RedirectBootApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(RedirectBootApplication.class);
        app.run(args);
    }


}
