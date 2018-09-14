package com.xy.spring.cloud.zuul.tunnel.apache;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
public class TunnelApacheHttpClientConnectionManagerFactory
        extends DefaultApacheHttpClientConnectionManagerFactory {

    private TunnelPlainConnectionSocketFactory tunnelPlainConnectionSocketFactory;

    public void setTunnelPlainConnectionSocketFactory(TunnelPlainConnectionSocketFactory tunnelPlainConnectionSocketFactory) {
        this.tunnelPlainConnectionSocketFactory = tunnelPlainConnectionSocketFactory;
    }

    @Override
    public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation,
                                                            int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive,
                                                            TimeUnit timeUnit, RegistryBuilder registryBuilder) {

        if(registryBuilder == null){
            registryBuilder = RegistryBuilder.<ConnectionSocketFactory> create();
        }
        //noinspection unchecked
        registryBuilder.register(HTTP_SCHEME, tunnelPlainConnectionSocketFactory);

        return super.newConnectionManager(disableSslValidation, maxTotalConnections, maxConnectionsPerRoute, timeToLive, timeUnit, registryBuilder);
    }
}
