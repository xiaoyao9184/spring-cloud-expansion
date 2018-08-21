package com.xy.spring.cloud.zuul.tunnel;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@ConfigurationProperties(
        prefix = "proxy.tunnel"
)
public class ZuulTunnelProperties {

    public static final String TUNNEL_SERVICE_ID =  "0.0.0.0";

    public static final String NO_EXIST_HOST = "http://127.0.0.0";

    private String serviceId = TUNNEL_SERVICE_ID;
    private String routeHost = NO_EXIST_HOST;
    private Map<String, TunnelSocket> sockets = new HashMap<String, TunnelSocket>();


    @PostConstruct
    public void init() {
        for (Map.Entry<String, TunnelSocket> entry : sockets.entrySet()) {
            if (entry.getValue().getId() == null) {
                entry.getValue().setId(entry.getKey());
            }
        }
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getRouteHost() {
        return routeHost;
    }

    public void setRouteHost(String routeHost) {
        this.routeHost = routeHost;
    }

    public Map<String, TunnelSocket> getSockets() {
        return sockets;
    }

    public void setSockets(Map<String, TunnelSocket> sockets) {
        this.sockets = sockets;
    }


    public static class TunnelSocket {

        private String id;
        private Integer maxConnCount;
        private Integer connCount;
        private Integer port;
        private Boolean acceptRepeat;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getMaxConnCount() {
            return maxConnCount;
        }

        public void setMaxConnCount(Integer maxConnCount) {
            this.maxConnCount = maxConnCount;
        }

        public Integer getConnCount() {
            return connCount;
        }

        public void setConnCount(Integer connCount) {
            this.connCount = connCount;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Boolean getAcceptRepeat() {
            return acceptRepeat;
        }

        public void setAcceptRepeat(Boolean acceptRepeat) {
            this.acceptRepeat = acceptRepeat;
        }
    }



}
