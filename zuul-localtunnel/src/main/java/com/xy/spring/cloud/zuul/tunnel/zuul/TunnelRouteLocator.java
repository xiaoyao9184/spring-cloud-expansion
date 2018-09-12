package com.xy.spring.cloud.zuul.tunnel.zuul;

import com.xy.spring.cloud.zuul.tunnel.ZuulTunnelProperties;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.*;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by xiaoyao9184 on 2018/8/20.
 */
public class TunnelRouteLocator extends SimpleRouteLocator
        implements RefreshableRouteLocator {

    private static Logger logger = LoggerFactory.getLogger(TunnelRouteLocator.class);

    private AtomicReference<Set<String>> tunnelRoutes = new AtomicReference<>();


    private ZuulProperties properties;

    private ZuulTunnelProperties zuulTunnelProperties;

    private ClientManager clientManager;

    public TunnelRouteLocator(
            String servletPath,
            ZuulProperties properties,
            ZuulTunnelProperties zuulTunnelProperties,
            ClientManager clientManager) {
        super(servletPath, properties);
        this.properties = properties;
        this.zuulTunnelProperties = zuulTunnelProperties;
        this.clientManager = clientManager;
    }


    @Override
    public void refresh() {
        logger.debug("Refresh routes and tunnels!");
        doRefresh();
        tunnelRoutes.set(new HashSet<>(zuulTunnelProperties.getSockets().keySet()));

        //refresh options by configure it again
        clientManager.getOptionProvider().configure();
    }

    public boolean isTunnelRoute(String id){
        return tunnelRoutes.get().contains(id);
    }

    /**
     * TODO support option from request
     * @param id route id
     * @param path route path
     * @param location route target location
     */
    public void addTunnelRoute(String id, String path, String location) {
        logger.debug("Add route '{}' and flag it to tunnel!", id);
        ZuulProperties.ZuulRoute zuulRoute = new ZuulProperties.ZuulRoute(path, location);
        zuulRoute.setId(id);
        properties.getRoutes().put(path, zuulRoute);

        ZuulTunnelProperties.TunnelSocket tunnelSocket = new ZuulTunnelProperties.TunnelSocket();
        tunnelSocket.setId(id);
        zuulTunnelProperties.getSockets().putIfAbsent(id,tunnelSocket);
    }

    @Override
    protected LinkedHashMap<String, ZuulProperties.ZuulRoute> locateRoutes() {
        LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<String, ZuulProperties.ZuulRoute>();
        routesMap.putAll(super.locateRoutes());
        if (clientManager != null) {
            Map<String, ZuulProperties.ZuulRoute> staticTunnel = new LinkedHashMap<String, ZuulProperties.ZuulRoute>();
            for (ZuulProperties.ZuulRoute route : routesMap.values()) {
                String id = route.getId();
                if (zuulTunnelProperties.getSockets().containsKey(id)) {
                    staticTunnel.put(id, route);
                }
            }
            // Add routes for client manager by default
            Set<String> tunnels = clientManager.getClient().keySet();
            String[] ignored = properties.getIgnoredServices()
                    .toArray(new String[0]);
            for (String clientId : tunnels) {
                // Ignore specifically ignored tunnels and those that were manually
                // configured
                /**
                 * path and location generate same like TunnelMvcEndpoint
                 * @see com.xy.spring.cloud.zuul.tunnel.actuate.TunnelMvcEndpoint
                 */
                String path = "/" + clientId + "/**";
                if (staticTunnel.containsKey(clientId)
                        && staticTunnel.get(clientId).getUrl() == null) {
                    // Explicitly configured with no URL, cannot be ignored
                    // all static routes are already in routesMap
                    // Update location using clientId if location is null
                    ZuulProperties.ZuulRoute staticRoute = staticTunnel.get(clientId);
                    if (!StringUtils.hasText(staticRoute.getLocation())) {
                        String location = UriComponentsBuilder.fromUriString(zuulTunnelProperties.getLocationTemplate())
                                .buildAndExpand(clientId)
                                .toUriString();
                        staticRoute.setLocation(location);
                    }
                }
                if (!PatternMatchUtils.simpleMatch(ignored, clientId)
                        && !routesMap.containsKey(path)) {
                    // Not ignored
                    routesMap.put(path, new ZuulProperties.ZuulRoute(path, clientId));
                }
            }
        }
        LinkedHashMap<String, ZuulProperties.ZuulRoute> values = new LinkedHashMap<>();
        for (Map.Entry<String, ZuulProperties.ZuulRoute> entry : routesMap.entrySet()) {
            String path = entry.getKey();
            // Prepend with slash if not already present.
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (StringUtils.hasText(properties.getPrefix())) {
                path = properties.getPrefix() + path;
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
            }
            values.put(path, entry.getValue());
        }
        return values;
    }
}
