package com.xy.spring.cloud.zuul;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by xiaoyao9184 on 2018/8/23.
 */
public class DefaultHttpBasicAuthenticationProvider implements HttpBasicAuthenticationProvider {

    private Map<String,RouteUsernamePassword> cache;

    private Consumer<DefaultHttpBasicAuthenticationProvider> configurator;

    public DefaultHttpBasicAuthenticationProvider(Consumer<DefaultHttpBasicAuthenticationProvider> configurator){
        this.configurator = configurator;
    }

    @Override
    public void configure() {
        configurator.accept(this);
    }

    @Override
    public synchronized Map<String, RouteUsernamePassword> provide() {
        return cache;
    }

    @Override
    public synchronized RouteUsernamePassword provide(String name) {
        return cache.get(name);
    }

    public synchronized void setCache(Map<String, RouteUsernamePassword> cache){
        this.cache = cache;
    }
}
