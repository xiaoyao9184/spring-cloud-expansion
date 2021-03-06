package com.xy.spring.cloud.zuul.tunnel.zuul;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.xy.spring.cloud.zuul.tunnel.ZuulTunnelProperties;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.Client;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;

import static com.xy.spring.cloud.zuul.tunnel.zuul.PreDecorationTunnelFilter.TUNNEL_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Created by xiaoyao9184 on 2018/8/8.
 */
public class InitTunnelFilter extends ZuulFilter {

    private static Logger logger = LoggerFactory.getLogger(InitTunnelFilter.class);


    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    private RouteLocator routeLocator;
    private ClientManager clientManager;
    private ObjectMapper objectMapper;

    public void setZuulProperties(ZuulProperties zuulProperties) {
        this.urlPathHelper.setRemoveSemicolonContent(zuulProperties.isRemoveSemicolonContent());
    }

    public void setZuulTunnelProperties(ZuulTunnelProperties zuulTunnelProperties) {
    }

    public void setRouteLocator(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    public void setClientManager(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 2;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String proxy = ctx.getOrDefault(PROXY_KEY,null).toString();

        //tunnel proxy
        //root path
        //can init
        if(isUseTunnel(ctx)
                && isRequestRootPath(ctx)
                && clientManager.canInitClient(proxy)){
            logger.debug("Tunnel route '{}' will be initialized!",
                    ctx.getOrDefault(PROXY_KEY,"**NO PROXY_KEY**").toString());
            return true;
        }
        return false;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String proxy = ctx.getOrDefault(PROXY_KEY,null).toString();
        String path = ctx.getRequest().getRequestURL().toString();

        Info info = clientManager.getOrNewClient(proxy, path);

        try {
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(200);
            ctx.addZuulResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            ctx.setResponseBody(objectMapper.writeValueAsString(info));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Tunnel route info write error!",e);
        }

        return null;
    }


    /**
     * Check route is using tunnel
     * @param ctx RequestContext
     * @return true/false
     */
    private boolean isUseTunnel(RequestContext ctx){
        return (boolean) ctx.getOrDefault(TUNNEL_KEY,false);
    }

    /**
     * Check request is for root path of route
     * @param ctx RequestContext
     * @return true/false
     */
    private boolean isRequestRootPath(RequestContext ctx){
        //Don't just use this because you can't be sure the root path when not strip prefix
        String targetPath = ctx.getOrDefault(REQUEST_URI_KEY,"**NO REQUEST_URI_KEY**").toString();
        if(targetPath.isEmpty()){
            return true;
        }
        //not strip prefix need to check the request path
        String id = ctx.getOrDefault(PROXY_KEY,null).toString();
        String routeRootPath = routeLocator.getRoutes().stream()
                .filter(r -> r.getId().equals(id))
                .map(r -> {
                    int index = r.getFullPath().indexOf("*") - 1;
                    if (index > 0) {
                        String rootPath = r.getFullPath().substring(0, index);
                        return rootPath;
                    }
                    return r.getFullPath();
                })
                .findFirst()
                .orElse(null);
        String requestPath = urlPathHelper.getPathWithinApplication(ctx.getRequest());
        return requestPath.equals(routeRootPath);
    }
}
