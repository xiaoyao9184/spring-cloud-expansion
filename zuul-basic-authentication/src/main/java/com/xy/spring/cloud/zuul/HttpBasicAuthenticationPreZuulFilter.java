package com.xy.spring.cloud.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.security.oauth2.proxy.ProxyAuthenticationProperties;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
public class HttpBasicAuthenticationPreZuulFilter extends ZuulFilter {
    public static final String BASIC = "basic";

    private Map<String, ProxyAuthenticationProperties.Route> routes = new HashMap<String, ProxyAuthenticationProperties.Route>();
    private ProxyRequestHelper helper;
    private HttpBasicAuthenticationProvider provider;

    public HttpBasicAuthenticationPreZuulFilter(
            ProxyRequestHelper helper,
            ProxyAuthenticationProperties properties,
            HttpBasicAuthenticationProvider provider){
        this.helper = helper;
        this.routes = properties.getRoutes();
        this.provider = provider;
    }

    public String filterType() {
        return PRE_TYPE;
    }

    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 3;
    }

    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        if (ctx.containsKey(PROXY_KEY)) {
            String id = ctx.getOrDefault(PROXY_KEY,null).toString();
            if (routes.containsKey(id)
                    && BASIC.equals(routes.get(id).getScheme())
                    && provider.canProvide(id)){
                return true;
            }
        }
        return false;
    }

    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String id = ctx.getOrDefault(PROXY_KEY,null).toString();
        String token = provider.provideToken(id);
        //auto ignored other authorization header
        helper.addIgnoredHeaders("authorization");
        ctx.addZuulRequestHeader("Authorization", token);
        return null;
    }
}