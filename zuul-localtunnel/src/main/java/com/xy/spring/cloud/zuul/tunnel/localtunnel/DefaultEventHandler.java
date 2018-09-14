package com.xy.spring.cloud.zuul.tunnel.localtunnel;

import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

/**
 * Created by xiaoyao9184 on 2018/9/10.
 */
public class DefaultEventHandler implements ClientManager.EventHandler {

    private Consumer<ClientManager.TunnelEvent> eventConsumer;

    public DefaultEventHandler(Consumer<ClientManager.TunnelEvent> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void acceptable(String id, SocketChannel socketChannel) {
        ClientManager.TunnelEvent event = new ClientManager.TunnelEvent(id,ClientManager.EventCode.Acceptable);
        event.setChannel(socketChannel);
        eventConsumer.accept(event);
    }

}
