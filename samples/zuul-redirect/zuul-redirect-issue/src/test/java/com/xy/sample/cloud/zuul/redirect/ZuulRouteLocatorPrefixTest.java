package com.xy.sample.cloud.zuul.redirect;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UrlPathHelper;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by xiaoyao9184 on 2018/8/6.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        ZuulRouteLocatorPrefixBootApplication.class
})
@SpringBootTest(webEnvironment = DEFINED_PORT)
@AutoConfigureMockMvc
public class ZuulRouteLocatorPrefixTest {


    @Autowired
    private MockMvc mvc;

    @Autowired
    private GetRouteConfig getRouteConfig;

    @Autowired
    private ZuulProperties zuulProperties;


    @Test
    public void global_proxy_prefix()
            throws Exception {

        mvc.perform(get("/xiaoyao9184/spring-cloud-expansion/master/README.md"))
                .andExpect(status().isOk());

        //TODO
        //global prefix when global strip-prefix is false
        assert zuulProperties.getPrefix().equals("/xiaoyao9184");
        assert !zuulProperties.isStripPrefix();

        //TODO
        //Prefix contain global prefix
        assert getRouteConfig.getIssueRoute().getPrefix().equals("/xiaoyao9184");
        //Path already have global prefix
        assert getRouteConfig.getIssueRoute().getPath().startsWith("/xiaoyao9184");

        //TODO
        //FullPath incorrect
        assert getRouteConfig.getIssueRoute().getFullPath().equals("/xiaoyao9184/xiaoyao9184/spring-cloud-expansion/master/README.md");
    }



    @Configuration
    public static class GetRouteConfig {

        private Route issueRoute;

        public Route getIssueRoute() {
            return issueRoute;
        }


        @Bean
        public ZuulFilter testZuulFilter(
                @Autowired RouteLocator routeLocator
        ){
            UrlPathHelper urlPathHelper = new UrlPathHelper();
            return new ZuulFilter() {
                @Override
                public String filterType() {
                    return PRE_TYPE;
                }

                @Override
                public int filterOrder() {
                    return PRE_DECORATION_FILTER_ORDER + 1;
                }

                @Override
                public boolean shouldFilter() {
                    return true;
                }

                @Override
                public Object run() throws ZuulException {
                    RequestContext ctx = RequestContext.getCurrentContext();
                    final String requestURI = urlPathHelper.getPathWithinApplication(ctx.getRequest());
                    issueRoute = routeLocator.getMatchingRoute(requestURI);
                    return null;
                }
            };
        }
    }

}
