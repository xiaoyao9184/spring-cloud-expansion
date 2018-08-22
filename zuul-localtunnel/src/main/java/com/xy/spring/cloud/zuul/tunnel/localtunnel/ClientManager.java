package com.xy.spring.cloud.zuul.tunnel.localtunnel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xiaoyao9184 on 2018/7/12.
 */
public class ClientManager {

    private static Logger logger = LoggerFactory.getLogger(ClientManager.class);

    private OptionProvider optionProvider;
    private Map<String,Client> clients;
    private Statistics stats;


    private final ReentrantLock selectorLock = new ReentrantLock();
    private Selector selector;
    private TunnelAgent.SelectorSocketAcceptHandler socketAcceptHandler;
    private Thread socketAcceptThread;


    public ClientManager(OptionProvider provider) throws IOException {
        this.optionProvider = provider;
        this.clients = new HashMap<>();
        this.stats = new Statistics();

        initSelector();
    }

    private void initSelector() throws IOException {
        this.selector = Selector.open();
        this.socketAcceptHandler = new TunnelAgent.SelectorSocketAcceptHandler(selector,selectorLock);
        this.socketAcceptThread = new Thread(socketAcceptHandler,"tunnel-socket-accept-select");
        this.socketAcceptThread.start();
    }




    public Map<String,Client> getClient() {
        return this.clients;
    }

    public Client getClient(String id) {
        return this.clients.get(id);
    }

    public boolean canInitClient(String id){
        if(clients.containsKey(id)){
            Option option = optionProvider.provideOrDefault(id, DEFAULT_OPTION);
            return option.getAcceptRepeat();
        }
        return true;
    }

    public Statistics getStats() {
        return stats;
    }

    public OptionProvider getOptionProvider() {
        return optionProvider;
    }

    /**
     * Init client
     * @param id ID of tunnel
     * @return Tunnel Info
     * @throws IOException
     */
    public Info newClient(String id) throws IOException {
        Option option = optionProvider.provideOrDefault(id);

        // can't ask for id already is use
        if (clients.containsKey(id) &&
                !option.getAcceptRepeat()) {
            throw new RuntimeException("Client already " + id + " exists!");
        }

        TunnelAgent agent = new TunnelAgent(
                option.getMaxTcpSockets(),
                selector,
                selectorLock
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

    /**
     * Init or new client
     * @param id ID of tunnel
     * @param proxyPath proxy path for request
     * @return Tunnel Info
     */
    public Info getOrNewClient(String id, String proxyPath){
        Client client = clients.get(id);
        Info info;
        if(client == null){
            try {
                logger.debug("Tunnel making new client with id '{}'!", id);
                info = newClient(id);
            } catch (IOException e) {
                logger.error("Tunnel making new client error!",e);
                throw new RuntimeException("Tunnel making new client error!",e);
            }
        }else{
            logger.debug("Tunnel use already client with id '{}'!", id);
            info = client.getInfo();
        }

        info.setUrl(proxyPath);
        logger.debug("Tunnel client '{}', port:{}, conn count:{}, url: {}.",
                info.getId(),
                info.getPort(), info.getMaxConnCount(), info.getUrl());

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



    public static Option DEFAULT_OPTION = new Option(){
        {
            setPort(0);
            setMaxTcpSockets(100);
            setInitTcpSockets(10);
            setAcceptRepeat(false);
        }
    };


    public interface OptionProvider {

        void configure();

        Map<String,Option> provide();

        Option provide(String name);

        Option provideOrDefault(String name);

        default Option provideOrDefault(String name, Option defaultOption) {
            Option option = provide(name);
            if(option == null){
                option = defaultOption;
            }
            return option;
        };
    }
}
