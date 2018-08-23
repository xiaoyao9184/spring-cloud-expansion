package com.xy.spring.cloud.zuul;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.security.oauth2.proxy.ProxyAuthenticationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@Configuration
@EnableConfigurationProperties(HttpBasicAuthenticationProperties.class)
@ConditionalOnClass(EnableZuulProxy.class)
public class ProxyHttpBasicAuthenticationAutoConfiguration {

    @Bean
    @Conditional(PropertySetCondition.class)
    public HttpBasicAuthenticationProvider httpBasicAuthenticationProvider(
            HttpBasicAuthenticationProperties properties) {
        DefaultHttpBasicAuthenticationProvider provider = new DefaultHttpBasicAuthenticationProvider((p) -> {
            p.setCache(properties.getBasics());
        });
        provider.configure();
        return provider;
    }

    @Bean
    @ConditionalOnBean(HttpBasicAuthenticationProvider.class)
    public HttpBasicAuthenticationPreZuulFilter httpBasicAuthenticationPreZuulFilter(
            ProxyAuthenticationProperties properties,
            HttpBasicAuthenticationProvider provider) {
        ProxyRequestHelper helper = new ProxyRequestHelper();
        return new HttpBasicAuthenticationPreZuulFilter(helper, properties, provider);
    }

    public static class PropertySetCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(final ConditionContext context,
                                                final AnnotatedTypeMetadata metadata) {
            final RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(context.getEnvironment());
            final Map<String, Object> properties = resolver.getSubProperties(HttpBasicAuthenticationProperties.PROPERTIE_MAIN + ".");
            return new ConditionOutcome(!properties.isEmpty(), "HttpBasicAuthentication PropertySetCondition");
        }
    }
}
