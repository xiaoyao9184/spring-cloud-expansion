package com.xy.spring.cloud.zuul.tunnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.spring.cloud.zuul.tunnel.actuate.TunnelEndpoint;
import com.xy.spring.cloud.zuul.tunnel.actuate.TunnelMvcEndpoint;
import com.xy.spring.cloud.zuul.tunnel.apache.AutoReleaseConnectionSocketApplicationListener;
import com.xy.spring.cloud.zuul.tunnel.apache.TunnelApacheHttpClientConnectionManagerFactory;
import com.xy.spring.cloud.zuul.tunnel.apache.TunnelPlainConnectionSocketFactory;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.DefaultEventHandler;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.DefaultOptionProvider;
import com.xy.spring.cloud.zuul.tunnel.zuul.InitTunnelFilter;
import com.xy.spring.cloud.zuul.tunnel.zuul.PreDecorationTunnelFilter;
import com.xy.spring.cloud.zuul.tunnel.zuul.TunnelRouteLocator;
import com.xy.spring.cloud.zuul.tunnel.zuul.ZuulRequestContextPassThroughHttpClientConfiguration;
import org.apache.http.HttpHost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
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
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;

import static com.xy.spring.cloud.zuul.tunnel.ZuulTunnelProperties.PROPERTIE_AUTO_RELEASE_SOCKET;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@Configuration
@ConditionalOnClass(EnableZuulProxy.class)
@EnableConfigurationProperties(ZuulTunnelProperties.class)
public class ZuulTunnelAutoConfiguration {


    @Bean
    public ClientManager clientManager(
            @Autowired ZuulTunnelProperties zuulTunnelProperties,
            @Autowired ApplicationContext applicationContext
    ) throws IOException {
        DefaultOptionProvider optionProvider =  new DefaultOptionProvider();
        optionProvider.setConfigurator((p) -> p.init(zuulTunnelProperties.getSockets()));
        //if will lazy configure when refresh route locator

        //publish tunnel event to application
        DefaultEventHandler eventHandler = new DefaultEventHandler((e) -> {
            applicationContext.publishEvent(AutoReleaseConnectionSocketApplicationListener.ApplicationTunnelEvent.to(e));
        });
        return new ClientManager(eventHandler,optionProvider);
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


        /**
         * location apache http route by zuul proxy route
         * @param preDecorationTunnelFilter use this for replacement route location
         * @param tunnelRouteLocator
         * @return
         */
        @Bean
        @ConditionalOnProperty(name = PROPERTIE_AUTO_RELEASE_SOCKET, havingValue = "true", matchIfMissing = true)
        public AutoReleaseConnectionSocketApplicationListener.HttpRouteLocator httpRouteLocator(
                PreDecorationTunnelFilter preDecorationTunnelFilter,
                TunnelRouteLocator tunnelRouteLocator
        ){
            return id -> tunnelRouteLocator.getRoutes().stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .map(Route::getLocation)
                    .map(preDecorationTunnelFilter::replacement)
                    .map(HttpHost::create)
                    .map(HttpRoute::new)
                    .orElse(null);
        }

        /**
         * tunnel event tigger auto release
         * @param simpleHostRoutingFilter
         * @param locator
         * @return
         */
        @Bean
        @ConditionalOnProperty(name = PROPERTIE_AUTO_RELEASE_SOCKET, havingValue = "true", matchIfMissing = true)
        public AutoReleaseConnectionSocketApplicationListener autoReleaseConnectionSocketApplicationListener(
                SimpleHostRoutingFilter simpleHostRoutingFilter,
                AutoReleaseConnectionSocketApplicationListener.HttpRouteLocator locator
        ){
            Field field = ReflectionUtils
                    .findField(SimpleHostRoutingFilter.class, "connectionManager");
            ReflectionUtils.makeAccessible(field);
            Object connectionManager = ReflectionUtils.getField(field,simpleHostRoutingFilter);
            HttpClientConnectionManager manager = (HttpClientConnectionManager) connectionManager;

            return new AutoReleaseConnectionSocketApplicationListener(
                    manager,
                    locator
            );
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
