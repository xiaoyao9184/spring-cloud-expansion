# spring-cloud-expansion

[![](https://jitpack.io/v/xiaoyao9184/spring-cloud-expansion.svg)](https://jitpack.io/#xiaoyao9184/spring-cloud-expansion)

XY's spring cloud expansion


# info


## zuul-basic-authentication

We know Spring Cloud Security use OAuth2 for proxy.

And we also know ```OAuth2TokenRelayFilter``` can relay token from gateway to proxy service.

Proxy authentication can be extended, 
just add new filter for new `Scheme` in ```ProxyAuthenticationProperties```.

So this project add 3 thing for support Http Basic Authentication proxy

- ```HttpBasicAuthenticationPreZuulFilter``` support add header
- ```HttpBasicAuthenticationProperties``` used to set the routing username and password
- ```ProxyHttpBasicAuthenticationAutoConfiguration``` just auto configuration above two

You can use it just setting properties like this

```yaml

zuul:
  routes:
    hub:
      path: /gayhub/**
      url: http://www.github.com
    httpbin:
      path: /httpbin/**
      url: http://httpbin.org
    iis:
      path: /iis/**
      url: http://localhost:80

proxy:
  auth:
    routes:
      hub: none
      httpbin: basic
      iis: basic
    basics:
      httpbin:
        username: name
        password: "**password**"
      iis:
        username: u
        password: p

```

OK
- you will use `name` and `**password**` for proxy httpbin(http://httpbin.org)
- you will use `u` and `p` for proxy iis(http://localhost:80)

