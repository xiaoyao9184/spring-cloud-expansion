package com.xy.sample.cloud.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * Created by xiaoyao9184 on 2018/4/25.
 */
@SpringBootApplication
@EnableZuulProxy
public class LocalTunnelBootApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(LocalTunnelBootApplication.class);
        app.run(args);
    }

}
