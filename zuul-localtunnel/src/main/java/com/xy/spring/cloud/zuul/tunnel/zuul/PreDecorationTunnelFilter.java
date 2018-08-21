package com.xy.spring.cloud.zuul.tunnel.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

import static com.xy.spring.cloud.zuul.tunnel.ZuulTunnelProperties.NO_EXIST_HOST;
import static com.xy.spring.cloud.zuul.tunnel.ZuulTunnelProperties.TUNNEL_SERVICE_ID;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Created by xiaoyao9184 on 2018/8/8.
 */
public class PreDecorationTunnelFilter extends ZuulFilter {

    private static Logger logger = LoggerFactory.getLogger(PreDecorationTunnelFilter.class);

    public static final String TUNNEL_KEY = "tunnel";

    private TunnelRouteLocator tunnelRouteLocator;
    private String tunnelServiceId = TUNNEL_SERVICE_ID;
    private String tunnelRouteHost = NO_EXIST_HOST;


    public PreDecorationTunnelFilter(TunnelRouteLocator tunnelRouteLocator){
        this.tunnelRouteLocator = tunnelRouteLocator;
    }

    public void setTunnelServiceId(String tunnelServiceId) {
        this.tunnelServiceId = tunnelServiceId;
    }

    public void setTunnelRouteHost(String tunnelRouteHost) {
        this.tunnelRouteHost = tunnelRouteHost;
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
        String proxy = ctx.getOrDefault(PROXY_KEY,"").toString();

        if(tunnelRouteLocator.isTunnelRoute(proxy)){
            logger.debug("The zuul route '{}' is specified to use tunnel!", proxy);
            return true;
        }

        if(ctx.containsKey(SERVICE_ID_KEY)
                && ctx.getRouteHost() == null){
            if(ctx.get(SERVICE_ID_KEY).equals(tunnelServiceId)){
                logger.debug("The zuul route '{}' service id is match to use tunnel!", proxy);
                return true;
            }
        }else if(!ctx.containsKey(SERVICE_ID_KEY)
                && ctx.getRouteHost() != null){
            if(tunnelRouteHost.contains(ctx.getRouteHost().getHost())){
                logger.debug("The zuul route '{}' host is match to use tunnel!", proxy);
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
            try {
                ctx.setRouteHost(new URL(tunnelRouteHost));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            logger.debug("Tunnel route '{}' use '{}' to generate an HTTP request packet!",
                    ctx.getOrDefault(PROXY_KEY,"").toString(),
                    tunnelRouteHost);
        }
        return null;
    }
}
