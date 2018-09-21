package com.xy.spring.cloud.zuul.tunnel.apache;

import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.AbstractConnPool;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by xiaoyao9184 on 2018/9/10.
 */
public class AutoReleaseConnectionSocketApplicationListener
        implements ApplicationListener<AutoReleaseConnectionSocketApplicationListener.ApplicationTunnelEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AutoReleaseConnectionSocketApplicationListener.class);

    private HttpClientConnectionManager manager;
    private HttpRouteLocator locator;

    private Object pool;
    private static Object notExistState = new Object();

    public AutoReleaseConnectionSocketApplicationListener(
            HttpClientConnectionManager manager,
            HttpRouteLocator locator){
        this.manager = manager;
        this.locator = locator;

        //connection pool
        Field field = ReflectionUtils
                .findField(PoolingHttpClientConnectionManager.class, "pool");
        ReflectionUtils.makeAccessible(field);
        this.pool = ReflectionUtils.getField(field, this.manager);
    }

    /**
     * init proxy route pool by replace it
     * @param route HttpRoute
     */
    private void initProxyPool(HttpRoute route){
        //get route pool map
        Field routeToPoolField = ReflectionUtils
                .findField(AbstractConnPool.class, "routeToPool");
        ReflectionUtils.makeAccessible(routeToPoolField);
        @SuppressWarnings("unchecked")
        Map<HttpRoute,Object> routeToPool = (Map<HttpRoute, Object>) ReflectionUtils.getField(routeToPoolField, this.pool);

        //init normal pool
        Method method = ReflectionUtils
                .findMethod(AbstractConnPool.class, "getPool", Object.class);
        ReflectionUtils.makeAccessible(method);
        Object routeSpecificPool = ReflectionUtils.invokeMethod(method, this.pool, route);

        //check is proxy pool
        Method[] methods = routeSpecificPool.getClass().getDeclaredMethods();
        boolean isProxy = Stream.of(methods)
                .anyMatch(m -> m.getName().endsWith("getTargetSource"));
        if(isProxy){
            return;
        }

        //create proxy pool
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(routeSpecificPool);
        factory.addAdvice(new RouteSpecificPoolGetPoolMethodInterceptor());
        Object proxyRouteSpecificPool = factory.getProxy();

        //replace normal pool with proxy pool
        routeToPool.replace(route,routeSpecificPool, proxyRouteSpecificPool);
    }

    public void release(String id, HttpRoute route) throws InterruptedException, ExecutionException, IOException {
        BasicHttpContext context = new BasicHttpContext();
        context.setAttribute("tunnel.id", id);

        initProxyPool(route);

        ConnectionRequest connectionRequest = manager.requestConnection(route, notExistState);
        //lease
        HttpClientConnection connection = connectionRequest.get(5, TimeUnit.MICROSECONDS);
        manager.connect(
                connection,
                route,
                0,
                context);
        //make complete
        manager.routeComplete(connection, route, context);
        //make buffer bind
        connection.flush();
        //release
        manager.releaseConnection(connection,null, 0, TimeUnit.MICROSECONDS);
    }

    @Override
    public void onApplicationEvent(ApplicationTunnelEvent tunnelEvent) {
        ClientManager.TunnelEvent event = tunnelEvent.getEvent();
        if(event.getCode() == ClientManager.EventCode.Acceptable){
            String id = event.getId();
            HttpRoute route = locator.locate(id);
            if(route != null){
                try {
                    release(id, route);
                } catch (Exception e) {
                    logger.error("Cant auto release tunnel socket to apache http client!", e);
                }
            }else{
                logger.warn("Cant find HttpRoute form tunnel {}, throw away event.", id);
            }
        }
    }


    /**
     * Decorator for TunnelEvent
     * @see com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager.TunnelEvent
     */
    public static class ApplicationTunnelEvent extends ApplicationEvent {

        private ClientManager.TunnelEvent event;

        /**
         * Create a new ApplicationEvent.
         *
         * @param source the object on which the event initially occurred (never {@code null})
         */
        public ApplicationTunnelEvent(Object source) {
            super(source);
        }

        public ApplicationTunnelEvent(Object source, ClientManager.TunnelEvent event) {
            super(source);
            this.event = event;
        }

        public ClientManager.TunnelEvent getEvent() {
            return event;
        }


        public static ApplicationTunnelEvent to(ClientManager.TunnelEvent event){
            return new ApplicationTunnelEvent(event,event);
        }
    }

    /**
     * Locator for location HttpRoute by id
     */
    public interface HttpRouteLocator {

        HttpRoute locate(String id);

    }

    /**
     * Support get not exist state entry(null entry)
     * @see org.apache.http.pool.RouteSpecificPool#getFree(Object)
     */
    private static class RouteSpecificPoolGetPoolMethodInterceptor implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            if (invocation.getMethod().getName().equals("getFree")
                    && invocation.getArguments().length == 1
                    && notExistState.equals(invocation.getArguments()[0])) {
                return null;
            }
            return invocation.proceed();
        }

    }
}
