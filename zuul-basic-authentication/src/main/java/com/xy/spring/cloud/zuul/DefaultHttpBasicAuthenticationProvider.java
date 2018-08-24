package com.xy.spring.cloud.zuul;

import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by xiaoyao9184 on 2018/8/23.
 */
public class DefaultHttpBasicAuthenticationProvider implements HttpBasicAuthenticationProvider {

    private Map<String,RouteUsernamePassword> cache;
    private Map<String,String> headerCache;

    private Consumer<DefaultHttpBasicAuthenticationProvider> configurator;

    public DefaultHttpBasicAuthenticationProvider(Consumer<DefaultHttpBasicAuthenticationProvider> configurator){
        this.configurator = configurator;
    }

    @Override
    public void configure() {
        configurator.accept(this);
    }

    @Override
    public boolean canProvide(String name) {
        return cache.containsKey(name);
    }

    @Override
    public synchronized Map<String, RouteUsernamePassword> provide() {
        return cache;
    }

    @Override
    public synchronized RouteUsernamePassword provide(String name) {
        return cache.get(name);
    }

    @Override
    public synchronized String provideToken(String name) {
        return headerCache.get(name);
    }

    public synchronized void setCache(Map<String, RouteUsernamePassword> cache){
        this.cache = cache;
        this.headerCache = cache.entrySet().stream()
                .map(kv -> {
                    if(kv.getValue() == null){
                        return null;
                    }
                    String username = kv.getValue().getUsername();
                    String password = kv.getValue().getPassword();
                    if(StringUtils.isEmpty(username)
                            || StringUtils.isEmpty(password)){
                        return null;
                    }
                    String temp = username + ":" + password;
                    byte[] bytes = temp.getBytes();
                    String basic = "Basic " + Base64Utils.encodeToString(bytes);
                    return new AbstractMap.SimpleEntry<>(kv.getKey(),basic);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }
}
