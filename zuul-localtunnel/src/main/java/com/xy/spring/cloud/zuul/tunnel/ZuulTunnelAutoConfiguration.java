package com.xy.spring.cloud.zuul.tunnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.spring.cloud.zuul.tunnel.actuate.TunnelEndpoint;
import com.xy.spring.cloud.zuul.tunnel.actuate.TunnelMvcEndpoint;
import com.xy.spring.cloud.zuul.tunnel.apachel.TunnelApacheHttpClientConnectionManagerFactory;
import com.xy.spring.cloud.zuul.tunnel.apachel.TunnelPlainConnectionSocketFactory;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.DefaultOptionProvider;
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

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@Configuration
@ConditionalOnClass(EnableZuulProxy.class)
@EnableConfigurationProperties(ZuulTunnelProperties.class)
public class ZuulTunnelAutoConfiguration {


    @Bean
    public ClientManager clientManager(
            @Autowired ZuulTunnelProperties zuulTunnelProperties
    ) throws IOException {
        DefaultOptionProvider optionProvider =  new DefaultOptionProvider();
        optionProvider.setConfigurator((p) -> p.init(zuulTunnelProperties.getSockets()));
        //if will lazy configure when refresh route locator
        return new ClientManager(optionProvider);
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
        private ClientManager clientManager;

        @Autowired
        private ObjectMapper objectMapper;


        @Bean
        public TunnelRouteLocator tunnelRouteLocator(ServerProperties server){
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
            filter.setTunnelLocationPattern(zuulTunnelProperties.getLocationPattern());
            filter.setTunnelLocationReplacement(zuulTunnelProperties.getLocationReplacement());
            return filter;
        }

        @Bean
        public InitTunnelFilter initTunnelFilter(RouteLocator routeLocator) throws MalformedURLException {
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
                    zuulTunnelProperties.getLocationTemplate(),
                    zuulProperties.getPrefix());
        }
    }

}
