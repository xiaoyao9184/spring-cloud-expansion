package com.xy.spring.cloud.zuul.tunnel.localtunnel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by xiaoyao9184 on 2018/7/12.
 */
public class Info {

    private String id;

    @JsonProperty("max_conn_count")
    private Integer maxConnCount;

    private Integer port;

    private String url;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getMaxConnCount() {
        return maxConnCount;
    }

    public void setMaxConnCount(Integer maxConnCount) {
        this.maxConnCount = maxConnCount;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
