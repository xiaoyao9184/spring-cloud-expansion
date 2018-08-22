package com.xy.spring.cloud.zuul.tunnel.localtunnel;

import com.xy.spring.cloud.zuul.tunnel.ZuulTunnelProperties;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager.DEFAULT_OPTION;

/**
 * Created by xiaoyao9184 on 2018/8/22.
 */
public class DefaultOptionProvider implements ClientManager.OptionProvider {

    public static final String DEFAULT_OPTION_KEY = "_default_";

    private Map<String,ClientManager.Option> cache;

    private Consumer<DefaultOptionProvider> configurator;

    public void setConfigurator(Consumer<DefaultOptionProvider> configurator) {
        this.configurator = configurator;
    }

    @Override
    public void configure() {
        configurator.accept(this);
    }

    @Override
    public synchronized Map<String, ClientManager.Option> provide() {
        return cache;
    }

    @Override
    public synchronized ClientManager.Option provide(String name) {
        return cache.get(name);
    }

    @Override
    public synchronized ClientManager.Option provideOrDefault(String name) {
        return cache.computeIfAbsent(name, n -> cache.getOrDefault(DEFAULT_OPTION_KEY, ClientManager.DEFAULT_OPTION));
    }

    public synchronized void init(Map<String, ZuulTunnelProperties.TunnelSocket> routeMap){
        cache = mapOptions(routeMap);
        cache.putIfAbsent(DEFAULT_OPTION_KEY, ClientManager.DEFAULT_OPTION);
    }

    private static Map<String,ClientManager.Option> mapOptions(Map<String, ZuulTunnelProperties.TunnelSocket> routeMap){
        return routeMap.values().stream()
                .map(tunnelSocket -> {
                    ClientManager.Option option = new ClientManager.Option();
                    option.setPort(tunnelSocket.getPort() == null ?
                            DEFAULT_OPTION.getPort() : tunnelSocket.getPort());
                    option.setMaxTcpSockets(tunnelSocket.getMaxConnCount() == null ?
                            DEFAULT_OPTION.getMaxTcpSockets() : tunnelSocket.getMaxConnCount());
                    option.setInitTcpSockets(tunnelSocket.getConnCount() == null ?
                            DEFAULT_OPTION.getInitTcpSockets() : tunnelSocket.getConnCount());
                    option.setAcceptRepeat(tunnelSocket.getAcceptRepeat() == null ?
                            DEFAULT_OPTION.getAcceptRepeat() : tunnelSocket.getAcceptRepeat());
                    return new AbstractMap.SimpleEntry<>(tunnelSocket.getId(),option);
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }
}
