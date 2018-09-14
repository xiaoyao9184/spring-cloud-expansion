package com.xy.spring.cloud.zuul.tunnel.apache;

import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiaoyao9184 on 2018/9/10.
 */
public class AutoReleaseConnectionSocketApplicationListener
        implements ApplicationListener<AutoReleaseConnectionSocketApplicationListener.ApplicationTunnelEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AutoReleaseConnectionSocketApplicationListener.class);

    private HttpClientConnectionManager manager;
    private HttpRouteLocator locator;

    public AutoReleaseConnectionSocketApplicationListener(
            HttpClientConnectionManager manager,
            HttpRouteLocator locator){
        this.manager = manager;
        this.locator = locator;
    }


    public void release(String id, HttpRoute route) throws InterruptedException, ExecutionException, IOException {
        BasicHttpContext context = new BasicHttpContext();
        context.setAttribute("tunnel.id", id);

        ConnectionRequest connectionRequest = manager.requestConnection(route, null);
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
}
