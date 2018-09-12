package com.xy.spring.cloud.zuul.tunnel.actuate;

import com.xy.spring.cloud.zuul.tunnel.zuul.TunnelRouteLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.cloud.netflix.zuul.RoutesRefreshedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by xiaoyao9184 on 2018/8/21.
 */
@ManagedResource(description = "Can be used to init the tunnel proxy routes")
public class TunnelMvcEndpoint extends EndpointMvcAdapter implements ApplicationEventPublisherAware {

    private static Logger logger = LoggerFactory.getLogger(TunnelMvcEndpoint.class);

    private final TunnelEndpoint delegate;

    private String routePrefix;

    private TunnelRouteLocator tunnelRouteLocator;

    private String routeLocationPrefix;

    private ApplicationEventPublisher publisher;

    public TunnelMvcEndpoint(TunnelEndpoint delegate,
                             TunnelRouteLocator tunnelRouteLocator,
                             String routeLocationPrefix,
                             String routePrefix) {
        super(delegate);
        this.delegate = delegate;
        this.tunnelRouteLocator = tunnelRouteLocator;
        this.routeLocationPrefix = routeLocationPrefix;
        this.routePrefix = routePrefix;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }


    @GetMapping("/{name:.*}")
    @ResponseBody
    @ManagedOperation
    public ResponseEntity<?> info(@PathVariable String name, HttpServletRequest request) {
        if (!delegate.isEnabled()) {
            // Shouldn't happen - MVC endpoint shouldn't be registered when delegate's
            // disabled
            return getDisabledResponse();
        }

        //Check route
        if(tunnelRouteLocator.getRoutes()
                .stream()
                .noneMatch(r -> r.getId().equals(name))) {
            //
            logger.debug("Tunnel zuul route '{}' not find need create!", name);
            //Add route
            tunnelRouteLocator.addTunnelRoute(name, "/" + name + "/**", routeLocationPrefix + name);
            //Refreshed event
            publisher.publishEvent(new RoutesRefreshedEvent(tunnelRouteLocator));
        }else{
            logger.debug("Tunnel zuul route '{}' already exist!", name);
        }

        //Init or get client info
        String routePath = ServletUriComponentsBuilder.fromContextPath(request)
                .path(routePrefix)
                .pathSegment(name)
                .build()
                .toString();
        return ResponseEntity.ok(delegate.getOrInitClient(name, routePath));
    }
}
