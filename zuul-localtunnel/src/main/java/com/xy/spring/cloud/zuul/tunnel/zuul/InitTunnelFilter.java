package com.xy.spring.cloud.zuul.tunnel.zuul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.xy.spring.cloud.zuul.tunnel.ZuulTunnelProperties;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
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
        boolean useTunnel = (boolean) ctx.getOrDefault(TUNNEL_KEY,false);
        if(useTunnel &&
                clientManager.getClient(proxy) == null){
            logger.debug("Tunnel route '{}' will be initialized!",
                    ctx.getOrDefault(PROXY_KEY,"").toString());
            return true;
        }
        return false;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest req = ctx.getRequest();
        String path = req.getRequestURI();

        //replace zuul servlet path
        path = path.replace(zuulProperties.getServletPath(),"");
        String[] parts = path.split("/");

        if (parts.length != 2) {
            throw new UnsupportedOperationException("Initialization tunnel zuul must not contain any other paths!");
        }

        String reqId = parts[1];

        try {
            logger.debug("Tunnel route making new client with id '{}'!", reqId);
            Info info = clientManager.newClient(reqId);
            info.setUrl(req.getRequestURL().toString());
            logger.debug("Tunnel client '{}', port:{}, conn count:{}, url: {}.",
                    info.getId(),
                    info.getPort(), info.getMaxConnCount(), info.getUrl());

            ctx.addZuulResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(200);
            ctx.setResponseBody(objectMapper.writeValueAsString(info));
        } catch (IOException e) {
            logger.error("Tunnel route making new client error!",e);
        }
        return null;
    }
}
