package com.xy.sample.cloud.zuul.redirect;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by xiaoyao9184 on 2018/7/23.
 */
@Configuration
public class MvcConfig extends WebMvcConfigurerAdapter {

    @Controller
    public class RedirectController {

        @RequestMapping(value = "/jump/this",method = RequestMethod.GET)
        @ResponseBody
        public ResponseEntity thisJump() {
            return ResponseEntity.status(302)
                    .header("Location","/info")
                    .build();
        }

        @RequestMapping(value = "/jump/self",method = RequestMethod.GET)
        @ResponseBody
        public ResponseEntity selfJump() {
            return ResponseEntity.status(302)
                    .header("Location","http://localhost:8080/info")
                    .build();
        }

        @RequestMapping(value = "/jump/api",method = RequestMethod.GET)
        @ResponseBody
        public ResponseEntity apiJump() {
            return ResponseEntity.status(302)
                    .header("Location","http://localhost:8080/api/api-info")
                    .build();
        }

        @RequestMapping(value = "/jump/bin",method = RequestMethod.GET)
        @ResponseBody
        public ResponseEntity binJump() {
            return ResponseEntity.status(302)
                    .header("Location","http://httpbin.org")
                    .build();
        }

        @RequestMapping(value = "/jump/hub",method = RequestMethod.GET)
        @ResponseBody
        public ResponseEntity hubJump() {
            return ResponseEntity.status(302)
                    .header("Location","http://www.gayhub.com")
                    .build();
        }


        @RequestMapping(value = "/info",method = RequestMethod.GET)
        @ResponseBody
        public String selfInfo() {
            return "this is self";
        }

    }

    @Controller
    public class ApiController {

        @RequestMapping(value = "/api-info",method = RequestMethod.GET)
        @ResponseBody
        public String apiInfo() {
            return "this is api";
        }
    }

}
