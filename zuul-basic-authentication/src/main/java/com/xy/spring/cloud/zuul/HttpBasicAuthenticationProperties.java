package com.xy.spring.cloud.zuul;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@ConfigurationProperties(
        prefix = "proxy.auth"
)
public class HttpBasicAuthenticationProperties {

    public static final String PROPERTIE_MAIN = "proxy.auth.basics";

    private Map<String, Item> basics = new HashMap<String, Item>();

    public Map<String, Item> getBasics() {
        return basics;
    }

    public void setBasics(Map<String, Item> basics) {
        this.basics = basics;
    }


    public static class Item {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }


}
