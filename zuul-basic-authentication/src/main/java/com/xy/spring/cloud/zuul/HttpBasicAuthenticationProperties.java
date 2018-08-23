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

    private Map<String, HttpBasicAuthenticationProvider.RouteUsernamePassword> basics = new HashMap<String, HttpBasicAuthenticationProvider.RouteUsernamePassword>();

    public Map<String, HttpBasicAuthenticationProvider.RouteUsernamePassword> getBasics() {
        return basics;
    }

    public void setBasics(Map<String, HttpBasicAuthenticationProvider.RouteUsernamePassword> basics) {
        this.basics = basics;
    }

}
