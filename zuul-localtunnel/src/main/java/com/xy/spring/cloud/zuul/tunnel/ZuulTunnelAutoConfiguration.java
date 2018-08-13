package com.xy.spring.cloud.zuul.tunnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.spring.cloud.zuul.tunnel.apachel.TunnelApacheHttpClientConnectionManagerFactory;
import com.xy.spring.cloud.zuul.tunnel.apachel.TunnelPlainConnectionSocketFactory;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import com.xy.spring.cloud.zuul.tunnel.zuul.InitTunnelFilter;
import com.xy.spring.cloud.zuul.tunnel.zuul.PreDecorationTunnelFilter;
import com.xy.spring.cloud.zuul.tunnel.zuul.ZuulRequestContextPassThroughHttpClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@Configuration
@ConditionalOnClass(EnableZuulProxy.class)
@EnableConfigurationProperties(ZuulTunnelProperties.class)
public class ZuulTunnelAutoConfiguration {


    public Map<String,ClientManager.Option> mapOptions (Map<String, ZuulTunnelProperties.Route> routeMap){
        return routeMap.values().stream()
                .map(route -> {
                    ClientManager.Option option = new ClientManager.Option();
                    option.setPort(route.getPort());
                    option.setMaxTcpSockets(route.getMaxConnCount());
                    option.setAcceptRepeat(true);
                    return new AbstractMap.SimpleEntry<>(route.getId(),option);
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @Bean
    public ClientManager clientManager(
            @Autowired ZuulTunnelProperties zuulTunnelProperties
    ){
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
        private ClientManager clientManager;

        @Autowired
        private ObjectMapper objectMapper;

        @Bean
        public PreDecorationTunnelFilter preTunnelFilter() throws MalformedURLException {
            PreDecorationTunnelFilter filter = new PreDecorationTunnelFilter();
            filter.setMatchHost(zuulTunnelProperties.getMatchServiceId());
            filter.setReplaceRouteHost(new URL(zuulTunnelProperties.getReplaceRouteHost()));
            filter.setEnableServiceIds(zuulTunnelProperties.getSockets().keySet());
            return filter;
        }


        @Bean
        public InitTunnelFilter initTunnelFilter() throws MalformedURLException {
            InitTunnelFilter filter = new InitTunnelFilter();
            filter.setZuulProperties(zuulProperties);
            filter.setZuulTunnelProperties(zuulTunnelProperties);
            filter.setClientManager(clientManager);
            filter.setObjectMapper(objectMapper);
            return filter;
        }
    }


}
