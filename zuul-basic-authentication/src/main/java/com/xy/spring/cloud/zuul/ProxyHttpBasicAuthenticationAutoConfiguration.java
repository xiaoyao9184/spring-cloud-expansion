package com.xy.spring.cloud.zuul;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
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
    public HttpBasicAuthenticationPreZuulFilter httpBasicAuthenticationPreZuulFilter(
            ProxyAuthenticationProperties properties,
            HttpBasicAuthenticationProperties basicAuthenticationProperties) {
        ProxyRequestHelper helper = new ProxyRequestHelper();
        return new HttpBasicAuthenticationPreZuulFilter(helper, properties, basicAuthenticationProperties);
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
