package com.xy.spring.cloud.zuul.tunnel.localtunnel;

/**
 * Created by xiaoyao9184 on 2018/7/12.
 */
public class Client {

    private String id;
    private TunnelAgent agent;
    private Info info;

    public Client(String id,TunnelAgent agent){
        this.id = id;
        this.agent = agent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TunnelAgent getAgent() {
        return agent;
    }

    public void setAgent(TunnelAgent agent) {
        this.agent = agent;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }
}
