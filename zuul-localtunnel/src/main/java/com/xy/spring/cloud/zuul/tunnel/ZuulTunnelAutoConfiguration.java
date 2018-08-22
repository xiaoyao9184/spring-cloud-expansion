package com.xy.spring.cloud.zuul.tunnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.spring.cloud.zuul.tunnel.actuate.TunnelEndpoint;
import com.xy.spring.cloud.zuul.tunnel.actuate.TunnelMvcEndpoint;
import com.xy.spring.cloud.zuul.tunnel.apachel.TunnelApacheHttpClientConnectionManagerFactory;
import com.xy.spring.cloud.zuul.tunnel.apachel.TunnelPlainConnectionSocketFactory;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import com.xy.spring.cloud.zuul.tunnel.zuul.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager.DEFAULT_OPTION;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@Configuration
@ConditionalOnClass(EnableZuulProxy.class)
@EnableConfigurationProperties(ZuulTunnelProperties.class)
public class ZuulTunnelAutoConfiguration {


    public Map<String,ClientManager.Option> mapOptions (Map<String, ZuulTunnelProperties.TunnelSocket> routeMap){
        return routeMap.values().stream()
                .map(tunnelSocket -> {
                    ClientManager.Option option = new ClientManager.Option();
                    option.setPort(tunnelSocket.getPort());
                    option.setMaxTcpSockets(tunnelSocket.getMaxConnCount() == null ?
                            DEFAULT_OPTION.getMaxTcpSockets() : tunnelSocket.getMaxConnCount());
                    option.setInitTcpSockets(tunnelSocket.getConnCount() == null ?
                            DEFAULT_OPTION.getInitTcpSockets() : tunnelSocket.getConnCount());
                    option.setAcceptRepeat(tunnelSocket.getAcceptRepeat() == null ?
                            DEFAULT_OPTION.getAcceptRepeat() : tunnelSocket.getAcceptRepeat());
                    return new AbstractMap.SimpleEntry<>(tunnelSocket.getId(),option);
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @Bean
    public ClientManager clientManager(
            @Autowired ZuulTunnelProperties zuulTunnelProperties
    ) throws IOException {
        return new ClientManager(mapOptions(zuulTunnelProperties.getSockets()));
    }


    @Configuration
    public static class HttpClientConfiguration {

        @Autowired
        private ClientManager clientManager;

        @Bean
        public TunnelPlainConnectionSocketFactory tunnelPlainConnectionSocketFactory(){
            TunnelPlainConnectionSocketFactory connectionSocketFactory = new TunnelPlainConnectionSocketFactory();
            connectionSocketFactory.setClientManager(clientManager);
            return connectionSocketFactory;
        }


        /**
         * @see org.springframework.cloud.commons.httpclient.HttpClientConfiguration
         */
        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        @Configuration
        @ConditionalOnProperty(name = "spring.cloud.httpclientfactories.apache.enabled", matchIfMissing = true)
        static class ApacheHttpClientConfiguration {

            @Bean
            public TunnelApacheHttpClientConnectionManagerFactory connManFactory(
                    @Autowired TunnelPlainConnectionSocketFactory tunnelPlainConnectionSocketFactory
            ) {
                TunnelApacheHttpClientConnectionManagerFactory clientConnectionManagerFactory = new TunnelApacheHttpClientConnectionManagerFactory();
                clientConnectionManagerFactory.setTunnelPlainConnectionSocketFactory(tunnelPlainConnectionSocketFactory);
                return clientConnectionManagerFactory;
            }


            @Bean
            public ApacheHttpClientFactory apacheHttpClientFactory() {
                return new DefaultApacheHttpClientFactory();
            }

        }
    }


    @Configuration
    @Import(ZuulRequestContextPassThroughHttpClientConfiguration.class)
    public static class ZuulConfiguration {

        @Autowired
        private ZuulProperties zuulProperties;

        @Autowired
        private ZuulTunnelProperties zuulTunnelProperties;

        @Autowired
        private RouteLocator routeLocator;

        @Autowired
        private ClientManager clientManager;

        @Autowired
        private ObjectMapper objectMapper;


        @Bean
        public TunnelRouteLocator tunnelRouteLocator(
                @Autowired ServerProperties server,
                @Autowired ClientManager clientManager
        ){
            TunnelRouteLocator locator = new TunnelRouteLocator(
                    server.getServletPrefix(),
                    zuulProperties,
                    zuulTunnelProperties,
                    clientManager
            );
            return locator;
        }

        @Bean
        public PreDecorationTunnelFilter preTunnelFilter(TunnelRouteLocator tunnelRouteLocator) {
            PreDecorationTunnelFilter filter = new PreDecorationTunnelFilter(tunnelRouteLocator);
            filter.setTunnelServiceId(zuulTunnelProperties.getServiceId());
            filter.setTunnelRouteHost(zuulTunnelProperties.getRouteHost());
            return filter;
        }

        @Bean
        public InitTunnelFilter initTunnelFilter() throws MalformedURLException {
            InitTunnelFilter filter = new InitTunnelFilter();
            filter.setZuulProperties(zuulProperties);
            filter.setZuulTunnelProperties(zuulTunnelProperties);
            filter.setRouteLocator(routeLocator);
            filter.setClientManager(clientManager);
            filter.setObjectMapper(objectMapper);
            return filter;
        }
    }


    @Configuration
    @ConditionalOnClass(Endpoint.class)
    public static class ActuateConfiguration {

        @ConditionalOnEnabledEndpoint("tunnels")
        @Bean
        public TunnelEndpoint tunnelEndpoint(ZuulProperties zuulProperties,
                                             ClientManager clientManager) {
            return new TunnelEndpoint(zuulProperties,clientManager);
        }

        @ConditionalOnEnabledEndpoint("tunnels")
        @Bean
        public TunnelMvcEndpoint tunnelMvcEndpoint(
                TunnelEndpoint tunnelEndpoint,
                @Autowired @Lazy TunnelRouteLocator tunnelRouteLocator,
                ZuulProperties zuulProperties,
                ZuulTunnelProperties zuulTunnelProperties) {
            return new TunnelMvcEndpoint(
                    tunnelEndpoint,
                    tunnelRouteLocator,
                    zuulTunnelProperties.getServiceId(),
                    zuulProperties.getPrefix());
        }
    }

}
