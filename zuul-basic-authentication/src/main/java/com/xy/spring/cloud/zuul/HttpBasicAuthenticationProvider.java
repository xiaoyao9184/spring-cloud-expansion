package com.xy.spring.cloud.zuul;

import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Created by xiaoyao9184 on 2018/8/23.
 */
public interface HttpBasicAuthenticationProvider {

    void configure();

    boolean canProvide(String name);

    Map<String,RouteUsernamePassword> provide();

    RouteUsernamePassword provide(String name);

    default RouteUsernamePassword provideOrDefault(String name, RouteUsernamePassword defaultRouteUsernamePassword) {
        RouteUsernamePassword option = provide(name);
        if(option == null){
            option = defaultRouteUsernamePassword;
        }
        return option;
    };

    default String provideToken(String name) {
        RouteUsernamePassword rup = provide(name);
        String username = rup.getUsername();
        String password = rup.getPassword();
        if(StringUtils.isEmpty(username)
                || StringUtils.isEmpty(password)){
            return null;
        }
        String temp = username + ":" + password;
        byte[] bytes = temp.getBytes();
        return "Basic " + Base64Utils.encodeToString(bytes);
    };

    default String provideTokenOrDefault(String name, String token) {
        String option = provideToken(name);
        if(option == null){
            option = token;
        }
        return option;
    };


    class RouteUsernamePassword {

        private String id;
        private String username;
        private String password;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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
