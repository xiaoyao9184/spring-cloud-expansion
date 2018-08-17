package com.xy.spring.cloud.zuul.redirect;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@ConfigurationProperties(
        prefix = "proxy.redirect"
)
public class ZuulRedirectProperties {

    private boolean allowToOtherRoute = true;
    private boolean passThroughOtherRoute = false;
    private boolean passThroughOuter = true;

    private Map<String, Option> routes = new HashMap<String, Option>();

    public boolean isAllowToOtherRoute() {
        return allowToOtherRoute;
    }

    public void setAllowToOtherRoute(boolean allowToOtherRoute) {
        this.allowToOtherRoute = allowToOtherRoute;
    }

    public boolean isPassThroughOtherRoute() {
        return passThroughOtherRoute;
    }

    public void setPassThroughOtherRoute(boolean passThroughOtherRoute) {
        this.passThroughOtherRoute = passThroughOtherRoute;
    }

    public boolean isPassThroughOuter() {
        return passThroughOuter;
    }

    public void setPassThroughOuter(boolean passThroughOuter) {
        this.passThroughOuter = passThroughOuter;
    }

    public Map<String, Option> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, Option> routes) {
        this.routes = routes;
    }


    public static class Option {
        private boolean allowProxyRedirected = true;

        public boolean isAllowProxyRedirected() {
            return allowProxyRedirected;
        }

        public void setAllowProxyRedirected(boolean allowProxyRedirected) {
            this.allowProxyRedirected = allowProxyRedirected;
        }
    }


    public static Option DEFAULT_OPTION = create_default();

    private static Option create_default(){
        return new Option(){
            {
                setAllowProxyRedirected(true);
            }
        };
    }
}
