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

    public static final String TUNNEL_FLAG =  "tunnel://";

    //Implicit tunnel route use port 00
    //you can use template like this 'http://www.{}.com:00/'
    //use ':00' for tunnel pattern and empty replacement
    private String locationTemplate = TUNNEL_FLAG + "{id}";

    private String locationPattern = TUNNEL_FLAG;
    private String locationReplacement = "http://";

    private Map<String, TunnelSocket> sockets = new HashMap<String, TunnelSocket>();


    @PostConstruct
    public void init() {
        for (Map.Entry<String, TunnelSocket> entry : sockets.entrySet()) {
            if (entry.getValue().getId() == null) {
                entry.getValue().setId(entry.getKey());
            }
        }
    }

    public String getLocationTemplate() {
        return locationTemplate;
    }

    /**
     * location prefix for dynamic addition
     * @see com.xy.spring.cloud.zuul.tunnel.actuate.TunnelMvcEndpoint
     * @param locationTemplate
     */
    public void setLocationTemplate(String locationTemplate) {
        this.locationTemplate = locationTemplate;
    }

    public String getLocationPattern() {
        return locationPattern;
    }

    /**
     * location pattern for 'Matched' tunnel route
     * @see com.xy.spring.cloud.zuul.tunnel.zuul.PreDecorationTunnelFilter
     * @param locationPattern
     */
    public void setLocationPattern(String locationPattern) {
        this.locationPattern = locationPattern;
    }

    public String getLocationReplacement() {
        return locationReplacement;
    }

    /**
     * location replacement for 'Matched' tunnel route
     * @see com.xy.spring.cloud.zuul.tunnel.zuul.PreDecorationTunnelFilter
     * @param locationReplacement
     */
    public void setLocationReplacement(String locationReplacement) {
        this.locationReplacement = locationReplacement;
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
