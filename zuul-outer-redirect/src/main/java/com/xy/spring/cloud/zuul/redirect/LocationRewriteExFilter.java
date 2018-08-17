package com.xy.spring.cloud.zuul.redirect;

import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

import java.net.URI;
import java.util.Comparator;
import java.util.Optional;

import static com.xy.spring.cloud.zuul.redirect.ZuulRedirectProperties.DEFAULT_OPTION;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;

/**
 * Copy modify from LocationRewriteFilter
 * Created by xiaoyao9184 on 2018/8/16.
 */
public class LocationRewriteExFilter extends ZuulFilter {

    private static Logger logger = LoggerFactory.getLogger(LocationRewriteExFilter.class);


    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    @Autowired
    private ZuulRedirectProperties zuulRedirectProperties;

    @Autowired
    private ZuulProperties zuulProperties;

    @Autowired
    private RouteLocator routeLocator;

    private static final String LOCATION_HEADER = "Location";

    public LocationRewriteExFilter() {
    }

    public LocationRewriteExFilter(ZuulProperties zuulProperties,
                                 RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
        this.zuulProperties = zuulProperties;
    }

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER - 100;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        int statusCode = ctx.getResponseStatusCode();
        return HttpStatus.valueOf(statusCode).is3xxRedirection();
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        Route route = routeLocator.getMatchingRoute(
                urlPathHelper.getPathWithinApplication(ctx.getRequest()));

        if (route != null) {
            Pair<String, String> lh = locationHeader(ctx);
            if (lh != null) {
                String location = lh.second();
                URI originalRequestUri = UriComponentsBuilder
                        .fromHttpRequest(new ServletServerHttpRequest(ctx.getRequest()))
                        .build().toUri();

                UriComponentsBuilder redirectedUriBuilder = UriComponentsBuilder
                        .fromUriString(location);

                UriComponents redirectedUriComps = redirectedUriBuilder.build();

                String originalPath = getRestoredPath(
                        this.routeLocator,
                        this.zuulRedirectProperties,
                        this.zuulProperties,
                        route,
                        redirectedUriComps);

                String modifiedLocation;
                if(originalPath != null){
                    modifiedLocation = redirectedUriBuilder
                            .scheme(originalRequestUri.getScheme())
                            .host(originalRequestUri.getHost())
                            .port(originalRequestUri.getPort()).replacePath(originalPath).build()
                            .toUriString();
                }else{
                    logger.debug("Not redirect to original, maybe it is an external location!");
                    modifiedLocation = redirectedUriComps.toString();
                }

                logger.debug("Redirect modified location is '{}'!", modifiedLocation);
                lh.setSecond(modifiedLocation);
            }
        }
        return null;
    }

    /**
     * The target allowed is 'itself', 'other route service' or 'outer service'
     * 'itself' is common and allowed path absolute or relative
     * 'internal route service' must be absolute
     * 'outer service' must be absolute
     *
     *  So
     *  1. If path is relative it must 'itself'
     *
     *  2. If path is absolute and usually can be found in the route locator.
     *  it may be 'itself' or 'internal route service'.
     *
     *  And if it can't be found in the locator,
     *  maybe the route locator uses the service ID for location, and the path use host or IP,
     *  can't find internal route based on redirected path.
     *
     * @param routeLocator route locator for query target route
     * @param zuulRedirectProperties option
     * @param zuulProperties option
     * @param route request route
     * @param redirectedUriComps Target
     * @return Proxy path
     */
    private String getRestoredPath(
            RouteLocator routeLocator,
            ZuulRedirectProperties zuulRedirectProperties,
            ZuulProperties zuulProperties,
            Route route,
            UriComponents redirectedUriComps) {
        StringBuilder path = new StringBuilder();

        logger.debug("Redirect primitive location is '{}'!", redirectedUriComps.toString());

        //check absolute path
        if(zuulRedirectProperties.isAllowToOtherRoute()
                && redirectedUriComps.getScheme() != null){
            Optional<Route> target = routeLocator.getRoutes().stream()
                    .sorted(Comparator.comparingInt(r -> ((Route)r).getLocation().length()).reversed())
                    .filter(r -> redirectedUriComps.toString().startsWith(r.getLocation()))
                    .findFirst();
            //target route exist
            if(target.isPresent()){
                Route tr = target.get();
                logger.debug("Redirect target is route '{}'!", tr.getId());
                ZuulRedirectProperties.Option option = zuulRedirectProperties.getRoutes().getOrDefault(tr.getId(),DEFAULT_OPTION);

                boolean reWriteToProxy = true;

                if(tr.getId().equals(route.getId())){
                    //it's itself
                    logger.debug("Redirect target is same as request target!");
                }else {
                    //it's other 'internal route service'
                    logger.debug("Redirect target is unrelated with the request target!");
                    if(!option.isAllowProxyRedirected()){
                        if(zuulRedirectProperties.isPassThroughOtherRoute()) {
                            //passthrough
                            return null;
                        }
                        reWriteToProxy = false;
                    }
                }

                if(reWriteToProxy){
                    logger.debug("Redirect target will be rewrite to the proxy route!");
                    String location = tr.getLocation();
                    String p = redirectedUriComps.toString().replace(location,location.endsWith("/") ? "/" : "");
                    path
                            .append(tr.getPrefix())
                            .append(p);
                    return path.toString();
                }
            }else{
                logger.debug("Redirect target is not any route!");
                //can't found target route
                if(zuulRedirectProperties.isPassThroughOuter()){
                    //passthrough
                    return null;
                }
                //now rewrite to itself
            }
        }
        logger.debug("Redirect target will force a rewrite to the proxy and point to the request target!");
        //not support not original target
        //agreement that the target always points to itself
        boolean downstreamHasGlobalPrefix = downstreamHasGlobalPrefix(zuulProperties);
        boolean downstreamHasRoutePrefix = downstreamHasRoutePrefix(route);

        String redirectedPathWithoutPrefix;
        if(downstreamHasGlobalPrefix && downstreamHasRoutePrefix){
            redirectedPathWithoutPrefix = redirectedUriComps.getPath()
                    .substring((route.getPrefix()).length());
        }else if(downstreamHasGlobalPrefix){
            redirectedPathWithoutPrefix = redirectedUriComps.getPath()
                    .substring((zuulProperties.getPrefix()).length());
        }else if(downstreamHasRoutePrefix){
            String routePrefix = route.getPrefix().substring(zuulProperties.getPrefix().length());
            redirectedPathWithoutPrefix = redirectedUriComps.getPath()
                    .substring(routePrefix.length());
        }else{
            redirectedPathWithoutPrefix = redirectedUriComps.getPath();
        }

        path.append("/").append(route.getPrefix())
                .append(redirectedPathWithoutPrefix);

        return path.toString();
    }

    private boolean routePrefixContainGlobalPrefix(){
        //issus 3147
        return zuulHasGlobalPrefix(zuulProperties);
//        return (zuulProperties.isStripPrefix()
//                && StringUtils.hasText(zuulProperties.getPrefix()));
    }

    private boolean downstreamHasGlobalPrefix(ZuulProperties zuulProperties) {
        return (!zuulProperties.isStripPrefix()
                && StringUtils.hasText(zuulProperties.getPrefix()));
    }

    private boolean downstreamHasRoutePrefix(Route route) {
        if(routePrefixContainGlobalPrefix()){
            String routePrefix = route.getPrefix().substring(zuulProperties.getPrefix().length());
            return (!route.isPrefixStripped()
                    && StringUtils.hasText(routePrefix));
        }else {
            return (!route.isPrefixStripped()
                    && StringUtils.hasText(route.getPrefix()));
        }
    }

    private boolean downstreamHasRoutePrefix(Route route, String routePrefix) {
        return (!route.isPrefixStripped()
                && routePrefix.startsWith("/"));
    }

    private boolean zuulHasGlobalPrefix(ZuulProperties zuulProperties) {
        return StringUtils.hasText(zuulProperties.getPrefix());
    }

    private Pair<String, String> locationHeader(RequestContext ctx) {
        if (ctx.getZuulResponseHeaders() != null) {
            for (Pair<String, String> pair : ctx.getZuulResponseHeaders()) {
                if (pair.first().equals(LOCATION_HEADER)) {
                    return pair;
                }
            }
        }
        return null;
    }
}