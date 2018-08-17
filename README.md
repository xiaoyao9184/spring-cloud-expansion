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



## zuul-localtunnel

Ok, This is a very hacked module.

But maybe just maybe that your gateway and services are network separated,
And your gateway cant not access your services.

We need tunnel like ngrok or frp, why not [localtunnel](https://github.com/localtunnel/localtunnel)?

This module combines localtunnel and zuul to achieve the above purposes.

You can understand that this module is the zuul version of the localtunnel server. 
Of course, only http is supported. 


Proceed as follows: 
(server as localtunnel-zuul-server, client as localtunnel-client)

1. server started and waiting for request.
2. client request a connection information for the zuul routing root node with id.
3. server find routing information based on the id, and return it.
4. client initiate multiple connections to the server port based on the information, these sockets will be saved.
5. user access server route, zuul will use the previously stored sockets for proxy



For example

- Create a zuul network gateway, add zuul-localtunnel dependence.
- Adding a iis service(localhost:80) for proxy, properties like this.
    
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
      tunnel:
        sockets:
          iis:
            maxConnCount: 100
            port: 2333
    
    ```

- Start up your tunnel-zuul gateway
- Build localtunnel-client config with iis(localhost:80) in same network.
- Start up localtunnel-client, done.

Now your tunnel-zuul gateway will proxy iis service(localhost:80) by use route 'iis', 
proxy data pack through tunnel-zuul's port 2333 to iis's port 80




## zuul-outer-redirect

Ok, Rewrite redirect location already supported in zuul,
But is always rewrite to point gateway itself.

Use this to do the following:

- redirect to other outer service not gateway or request service
- redirect to gateway and proxy to downstream service


For example
```yaml

zuul:
  host:
    socket-timeout-millis: 100000
    connect-timeout-millis: 20000
  prefix: /proxy
  routes:
    redirect:
      path: /redirect/**
      url: http://localhost:8080
    redirect-api:
      path: /redirect/api/**
      url: http://localhost:8080/api
    httpbin:
      path: /httpbin/**
      url: http://httpbin.org

proxy:
  redirect:
    allowToOtherRoute: true
    passThroughOtherRoute: true
    passThroughOuter: true
    routes:
      httpbin:
        allowProxyRedirected: false
```

If request target route is 'redirect', it can redirect this:


| redirect | target to route | rewrite |
|-----|-----|-----|
| http://localhost:8080/** | redirect | http://localhost:8080/proxy/redirect/** |
| http://localhost:8080/api/** | redirect-api | http://localhost:8080/proxy/redirect/api/** |
| http://httpbin.org/** | httpbin | NONE |
| http://www.gayhub.com | NONE | NONE |
