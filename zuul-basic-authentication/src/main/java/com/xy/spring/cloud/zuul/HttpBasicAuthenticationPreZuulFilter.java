package com.xy.spring.cloud.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.security.oauth2.proxy.ProxyAuthenticationProperties;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
public class HttpBasicAuthenticationPreZuulFilter extends ZuulFilter {
    public static final String BASIC = "basic";

    private Map<String, ProxyAuthenticationProperties.Route> routes = new HashMap<String, ProxyAuthenticationProperties.Route>();
    private ProxyRequestHelper helper;
    private Map<String,String> tokens;

    public HttpBasicAuthenticationPreZuulFilter(
            ProxyRequestHelper helper,
            ProxyAuthenticationProperties properties,
            HttpBasicAuthenticationProperties basicAuthenticationProperties){
        this.helper = helper;
        this.routes = properties.getRoutes();
        this.tokens = basicAuthenticationProperties.getBasics().entrySet().stream()
                .map(kv -> {
                    if(kv.getValue() == null){
                        return null;
                    }
                    String username = kv.getValue().getUsername();
                    String password = kv.getValue().getPassword();
                    if(StringUtils.isEmpty(username)
                            || StringUtils.isEmpty(password)){
                        return null;
                    }
                    String temp = username + ":" + password;
                    byte[] bytes = temp.getBytes();
                    String basic = "Basic " + Base64Utils.encodeToString(bytes);
                    return new AbstractMap.SimpleEntry<>(kv.getKey(),basic);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    public String filterType() {
        return "pre";
    }

    public int filterOrder() {
        return 6;
    }

    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        if (ctx.containsKey("proxy")) {
            String id = (String) ctx.get("proxy");
            if (routes.containsKey(id)
                    && BASIC.equals(routes.get(id).getScheme())
                    && tokens.containsKey(id)) {
                return true;
            }
        }
        return false;
    }

    public Object run() {
        //auto ignored other authorization header
        helper.addIgnoredHeaders("authorization");
        RequestContext ctx = RequestContext.getCurrentContext();
        String id = (String) ctx.get("proxy");
        ctx.addZuulRequestHeader("Authorization", tokens.get(id));
        return null;
    }
}