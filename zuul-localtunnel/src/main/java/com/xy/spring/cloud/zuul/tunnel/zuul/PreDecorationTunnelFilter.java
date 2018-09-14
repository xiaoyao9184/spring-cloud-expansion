package com.xy.spring.cloud.zuul.tunnel.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.xy.spring.cloud.zuul.tunnel.ZuulTunnelProperties.TUNNEL_FLAG;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Specified route is tunnel, replacement tunnel location to http location
 * Created by xiaoyao9184 on 2018/8/8.
 */
public class PreDecorationTunnelFilter extends ZuulFilter {

    private static Logger logger = LoggerFactory.getLogger(PreDecorationTunnelFilter.class);

    public static final String TUNNEL_KEY = "tunnel";
    public static final String TUNNEL_LOCATION = "tunnel.location";

    private TunnelRouteLocator tunnelRouteLocator;

    private String tunnelLocationPattern = TUNNEL_FLAG;
    private Pattern pattern = Pattern.compile(tunnelLocationPattern);
    private String tunnelLocationReplacement;


    public PreDecorationTunnelFilter(TunnelRouteLocator tunnelRouteLocator){
        this.tunnelRouteLocator = tunnelRouteLocator;
    }

    public void setTunnelLocationPattern(String tunnelLocationPattern) {
        this.tunnelLocationPattern = tunnelLocationPattern;
        if(tunnelLocationPattern != null){
            this.pattern = Pattern.compile(tunnelLocationPattern);
        }else{
            this.pattern = null;
        }
    }

    public void setTunnelLocationReplacement(String tunnelLocationReplaced) {
        this.tunnelLocationReplacement = tunnelLocationReplaced;
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

        boolean result = false;

        //Specified
        if(tunnelRouteLocator.isTunnelRoute(proxy)){
            logger.debug("The zuul route '{}' is specified to use tunnel!", proxy);
            result = true;
        }

        //Matched
        if(pattern != null){
            String location = getTunnelLocation(ctx,proxy);
            Matcher matcher = pattern.matcher(location);
            if(matcher.find()){
                logger.debug("The zuul route '{}' is match to use tunnel!", proxy);
                ctx.set(TUNNEL_LOCATION, location);
                result = true;
            }
        }

        return result;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        //flag it is tunnel route
        ctx.set(TUNNEL_KEY,true);


        //Matched tunnel location
        String location = (String) ctx.get(TUNNEL_LOCATION);
        if(location != null
                && tunnelLocationReplacement != null){
            //tunnel route not support service mode
            ctx.remove(SERVICE_ID_KEY);

            location = replacement(location);

            try {
                ctx.setRouteHost(new URL(location));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            logger.debug("Tunnel route '{}' use '{}' to generate an HTTP request packet!",
                    ctx.getOrDefault(PROXY_KEY,"").toString(),
                    location);
        }
        return null;
    }

    private String getTunnelLocation(RequestContext ctx, String proxy){
        String location = null;
        if(ctx.containsKey(SERVICE_ID_KEY)
                && ctx.getRouteHost() == null){
            //when url not http or https the route host is empty
            location = (String) ctx.get(SERVICE_ID_KEY);
            logger.debug("The zuul route '{}' location from service id !", proxy);
        }else if(!ctx.containsKey(SERVICE_ID_KEY)
                && ctx.getRouteHost() != null){
            location = ctx.getRouteHost().toString();
            logger.debug("The zuul route '{}' location from host!", proxy);
        }
        return location;
    }

    public String replacement(String location) {
        Matcher matcher = pattern.matcher(location);
        return matcher.replaceAll(tunnelLocationReplacement);
    }
}
