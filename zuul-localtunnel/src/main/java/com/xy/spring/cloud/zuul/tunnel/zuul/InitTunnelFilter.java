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
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;

import static com.xy.spring.cloud.zuul.tunnel.zuul.PreDecorationTunnelFilter.TUNNEL_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Created by xiaoyao9184 on 2018/8/8.
 */
public class InitTunnelFilter extends ZuulFilter {

    private static Logger logger = LoggerFactory.getLogger(InitTunnelFilter.class);

    private ZuulProperties zuulProperties;
    private ZuulTunnelProperties zuulTunnelProperties;
    private ClientManager clientManager;
    private ObjectMapper objectMapper;

    public void setZuulProperties(ZuulProperties zuulProperties) {
        this.zuulProperties = zuulProperties;
    }

    public void setZuulTunnelProperties(ZuulTunnelProperties zuulTunnelProperties) {
        this.zuulTunnelProperties = zuulTunnelProperties;
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
        String proxy = ctx.getOrDefault(PROXY_KEY,"").toString();

        //tunnel proxy
        //root path
        //can init
        if(isUseTunnel(ctx)
                && isRequestRootPath(ctx)
                && clientManager.canInitClient(proxy)){
            logger.debug("Tunnel route '{}' will be initialized!",
                    ctx.getOrDefault(PROXY_KEY,"").toString());
            return true;
        }
        return false;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String proxy = ctx.getOrDefault(PROXY_KEY,"").toString();

        Client client = clientManager.getClient(proxy);
        Info info;
        if(client == null){
            try {
                logger.debug("Tunnel route making new client with id '{}'!", proxy);
                info = clientManager.newClient(proxy);
            } catch (IOException e) {
                logger.error("Tunnel route making new client error!",e);
                throw new RuntimeException("Tunnel route making new client error!",e);
            }
        }else{
            logger.debug("Tunnel route use already client with id '{}'!", proxy);
            info = client.getInfo();
        }

        info.setUrl(ctx.getRequest().getRequestURL().toString());
        logger.debug("Tunnel client '{}', port:{}, conn count:{}, url: {}.",
                info.getId(),
                info.getPort(), info.getMaxConnCount(), info.getUrl());

        try {
            ctx.addZuulResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(200);
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
        String path = ctx.getRequest().getRequestURI();
        //replace zuul servlet path
        path = path.replace(zuulProperties.getServletPath(),"");
        String[] parts = path.split("/");

        return parts.length == 2
                && !path.endsWith("/");
    }
}
