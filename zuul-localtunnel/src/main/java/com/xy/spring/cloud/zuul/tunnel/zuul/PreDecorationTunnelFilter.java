package com.xy.spring.cloud.zuul.tunnel.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Created by xiaoyao9184 on 2018/8/8.
 */
public class PreDecorationTunnelFilter extends ZuulFilter {

    private static Logger logger = LoggerFactory.getLogger(PreDecorationTunnelFilter.class);

    public static final String TUNNEL_KEY = "tunnel";

    private String matchHost;
    private URL replaceRouteHost;
    private Collection<String> enableServiceIds;

    public void setMatchHost(String matchHost) {
        this.matchHost = matchHost;
    }

    public void setReplaceRouteHost(URL replaceRouteHost) {
        this.replaceRouteHost = replaceRouteHost;
    }

    public void setEnableServiceIds(Collection<String> enableServiceIds) {
        this.enableServiceIds = enableServiceIds;
    }

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        if(enableServiceIds.contains(
                ctx.getOrDefault(PROXY_KEY,"").toString())){
            logger.debug("The zuul route '{}' is specified to use tunnel!",
                    ctx.getOrDefault(PROXY_KEY,"").toString());
            return true;
        }

        if(ctx.containsKey(SERVICE_ID_KEY)
                && ctx.getRouteHost() == null){
            if(ctx.get(SERVICE_ID_KEY).equals(matchHost)){
                logger.debug("The zuul route '{}' service id is match to use tunnel!",
                        ctx.getOrDefault(PROXY_KEY,"").toString());
                return true;
            }
        }
        return false;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(TUNNEL_KEY,true);

        if(ctx.containsKey(SERVICE_ID_KEY)){
            ctx.remove(SERVICE_ID_KEY);
            ctx.setRouteHost(replaceRouteHost);
            logger.debug("Tunnel route '{}' use '{}' to generate an HTTP request packet!",
                    ctx.getOrDefault(PROXY_KEY,"").toString(),
                    replaceRouteHost);
        }
        return null;
    }
}
