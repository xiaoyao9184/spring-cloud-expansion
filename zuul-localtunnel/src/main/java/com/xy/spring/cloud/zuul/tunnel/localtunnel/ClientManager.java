package com.xy.spring.cloud.zuul.tunnel.localtunnel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiaoyao9184 on 2018/7/12.
 */
public class ClientManager {
    private Map<String,Option> opt;
    private Map<String,Client> clients;
    private Statistics stats;



    public ClientManager(){
        opt = new HashMap<>();
        clients = new HashMap<>();
        stats = new Statistics();

        opt.put(DEFAULT_OPTION_KEY, DEFAULT_OPTION);
    }

    public ClientManager(Map<String,Option> options){
        opt = options;
        clients = new HashMap<>();
        stats = new Statistics();

        opt.putIfAbsent(DEFAULT_OPTION_KEY, DEFAULT_OPTION);
    }


    public Client getClient(String id) {
        return this.clients.get(id);
    }

    public boolean canInitClient(String id){
        if(this.clients.containsKey(id)){
            Option option = opt.getOrDefault(id,DEFAULT_OPTION);
            return option.getAcceptRepeat();
        }
        return true;
    }

    public Statistics getStats() {
        return stats;
    }

    /**
     *
     * @param id
     * @return
     * @throws IOException
     */
    public Info newClient(String id) throws IOException {
        Option option = opt.getOrDefault(id,DEFAULT_OPTION);


        // can't ask for id already is use
        if (clients.containsKey(id) &&
                !option.getAcceptRepeat()) {
            throw new RuntimeException("Client already " + id + " exists!");
        }

        TunnelAgent agent = new TunnelAgent(
                id,
                option.getMaxTcpSockets()
        );

        Client client = new Client(
                id,
                agent
        );

        // add to clients map immediately
        // avoiding races with other clients requesting same id
        clients.put(id,client);

        //TODO close client to remove

        //port
        Integer port;
        if(option.getPort() > 0){
            port = agent.listen(option.getPort());
        }else{
            port = agent.listen();
        }

        stats.add();

        Info info = new Info();
        info.setId(id);
        info.setPort(port);
        info.setMaxConnCount(option.getInitTcpSockets());
        client.setInfo(info);
        return info;
    }


    public static class Statistics {
        private Integer tunnels = 0;

        public Integer getTunnels() {
            return tunnels;
        }

        public void setTunnels(Integer tunnels) {
            this.tunnels = tunnels;
        }

        public synchronized void add(){
            tunnels++;
        }
    }


    public static class Option {
        private Integer port;
        private Integer maxTcpSockets;
        private Integer initTcpSockets;
        private Boolean acceptRepeat;

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Integer getMaxTcpSockets() {
            return maxTcpSockets;
        }

        public void setMaxTcpSockets(Integer maxTcpSockets) {
            this.maxTcpSockets = maxTcpSockets;
        }

        public Integer getInitTcpSockets() {
            return initTcpSockets;
        }

        public void setInitTcpSockets(Integer initTcpSockets) {
            this.initTcpSockets = initTcpSockets;
        }

        public Boolean getAcceptRepeat() {
            return acceptRepeat;
        }

        public void setAcceptRepeat(Boolean acceptRepeat) {
            this.acceptRepeat = acceptRepeat;
        }
    }


    public static final String DEFAULT_OPTION_KEY = "_default_";

    public static Option DEFAULT_OPTION = new Option(){
        {
            setPort(0);
            setMaxTcpSockets(100);
            setInitTcpSockets(10);
            setAcceptRepeat(false);
        }
    };
}
