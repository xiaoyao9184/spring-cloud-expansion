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

    public static final String PROPERTIE_SERVLET = "proxy.tunnel.servlet";

    private Boolean servlet = false;
    private String matchServiceId = "0.0.0.0";
    private String replaceRouteHost = "http://127.0.0.0";

    private Map<String, Route> sockets = new HashMap<String, Route>();


    @PostConstruct
    public void init() {
        for (Map.Entry<String, Route> entry : sockets.entrySet()) {
            if (entry.getValue().getId() == null) {
                entry.getValue().setId(entry.getKey());
            }
        }
    }


    public Boolean getServlet() {
        return servlet;
    }

    public void setServlet(Boolean servlet) {
        this.servlet = servlet;
    }

    public String getMatchServiceId() {
        return matchServiceId;
    }

    public void setMatchServiceId(String matchServiceId) {
        this.matchServiceId = matchServiceId;
    }

    public String getReplaceRouteHost() {
        return replaceRouteHost;
    }

    public void setReplaceRouteHost(String replaceRouteHost) {
        this.replaceRouteHost = replaceRouteHost;
    }

    public Map<String, Route> getSockets() {
        return sockets;
    }

    public void setSockets(Map<String, Route> sockets) {
        this.sockets = sockets;
    }

    public static class Route {

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
