package com.xy.spring.cloud.zuul;

import java.util.Map;

/**
 * Created by xiaoyao9184 on 2018/8/23.
 */
public interface HttpBasicAuthenticationProvider {

    void configure();

    Map<String,RouteUsernamePassword> provide();

    RouteUsernamePassword provide(String name);

    default RouteUsernamePassword provideOrDefault(String name, RouteUsernamePassword defaultRouteUsernamePassword) {
        RouteUsernamePassword option = provide(name);
        if(option == null){
            option = defaultRouteUsernamePassword;
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
