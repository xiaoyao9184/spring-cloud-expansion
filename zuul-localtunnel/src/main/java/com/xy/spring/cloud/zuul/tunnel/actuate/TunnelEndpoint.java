package com.xy.spring.cloud.zuul.tunnel.actuate;

import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.Info;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Created by xiaoyao9184 on 2018/8/21.
 */
@ManagedResource(description = "Can be used to list the tunnel statistics")
@ConfigurationProperties(prefix = "endpoints.tunnels")
public class TunnelEndpoint extends AbstractEndpoint<ClientManager.Statistics> {

    private ClientManager clientManager;

    public TunnelEndpoint(
            ZuulProperties zuulProperties,
            ClientManager clientManager) {
        super("tunnels", false);
        this.clientManager = clientManager;
    }

    /**
     * endpoint of statistics
     * @return statistics
     */
    @Override
    public ClientManager.Statistics invoke() {
        return clientManager.getStats();
    }

    /**
     * Init or get client
     * @param id ID of tunnel route
     * @param proxyPath proxy path for request
     * @return Tunnel Info
     */
    public Info getOrInitClient(String id, String proxyPath){
        return clientManager.getOrNewClient(id,proxyPath);
    }
}
