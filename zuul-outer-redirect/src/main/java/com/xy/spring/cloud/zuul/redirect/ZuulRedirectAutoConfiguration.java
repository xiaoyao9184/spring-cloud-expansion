package com.xy.spring.cloud.zuul.redirect;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by xiaoyao9184 on 2018/8/7.
 */
@Configuration
@ConditionalOnClass(EnableZuulProxy.class)
@EnableConfigurationProperties(ZuulRedirectProperties.class)
@Import(LocationRewriteExFilter.class)
public class ZuulRedirectAutoConfiguration {


}
