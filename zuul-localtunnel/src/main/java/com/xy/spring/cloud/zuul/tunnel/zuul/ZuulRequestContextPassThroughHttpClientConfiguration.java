package com.xy.spring.cloud.zuul.tunnel.zuul;

import com.netflix.zuul.context.RequestContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
public class ZuulRequestContextPassThroughHttpClientConfiguration implements BeanPostProcessor {

    private static Logger logger = LoggerFactory.getLogger(ZuulRequestContextPassThroughHttpClientConfiguration.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (SimpleHostRoutingFilter.class.isAssignableFrom(bean.getClass())
                && bean instanceof SimpleHostRoutingFilter) {
            logger.debug("Will create proxy for {}'s field 'httpClient' " +
                            "and replace it to support execute pass through zuul RequestContext to HttpContext!",
                    SimpleHostRoutingFilter.class.getSimpleName());

            Field field = ReflectionUtils
                    .findField(SimpleHostRoutingFilter.class, "httpClient");
            ReflectionUtils.makeAccessible(field);

            Object httpClient = ReflectionUtils.getField(field,bean);
            ProxyFactory factory = new ProxyFactory();
            factory.setTarget(httpClient);
            factory.addAdvice(new ZuulRequestContextPassThroughHttpClientHttpContextSupportAdapter());
            httpClient = factory.getProxy();
            ReflectionUtils.setField(field,bean,httpClient);

            return bean;
        }
        return bean;
    }

    /**
     * Pass through Zuul's RequestContext to HttpClient's HttpContext
     * @see SimpleHostRoutingFilter#forwardRequest
     */
    private static class ZuulRequestContextPassThroughHttpClientHttpContextSupportAdapter implements MethodInterceptor {

        private static Logger logger = LoggerFactory.getLogger(ZuulRequestContextPassThroughHttpClientHttpContextSupportAdapter.class);

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            if (invocation.getMethod().getName().equals("execute")
                    && invocation.getArguments().length == 2
                    && invocation.getArguments()[0] instanceof HttpHost
                    && invocation.getArguments()[1] instanceof HttpRequest) {
                RequestContext ctx = RequestContext.getCurrentContext();
                if (ctx != null) {
                    logger.debug("Find Zuul RequestContext, will set it the HttpContext!");
                    BasicHttpContext context = new BasicHttpContext();
                    context.setAttribute("zuul.request-context",ctx);

                    Method method = ReflectionUtils
                            .findMethod(HttpClient.class, "execute",
                                    HttpHost.class, HttpRequest.class, HttpContext.class);
                    return ReflectionUtils
                            .invokeMethod(method, invocation.getThis(),
                                    invocation.getArguments()[0], invocation.getArguments()[1], context);
                }
            }
            return invocation.proceed();
        }

    }

}
