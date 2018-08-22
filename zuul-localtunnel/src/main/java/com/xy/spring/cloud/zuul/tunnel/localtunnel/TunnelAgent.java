package com.xy.spring.cloud.zuul.tunnel.localtunnel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xiaoyao9184 on 2018/7/13.
 */
public class TunnelAgent {

    private static Logger logger = LoggerFactory.getLogger(TunnelAgent.class);

    // sockets we can hand out via pullConnection
    private Queue<Socket> availableSockets;

    // track maximum allowed sockets
    private Integer connectedSockets;
    private Integer maxTcpSockets;

    // new tcp server to service requests for this client
    private ServerSocketChannel channel;
    private Selector selector;
    private ReentrantLock lock;

    // flag to avoid double starts
    private Boolean started = false;
    private Boolean closed = false;


    /**
     *
     * @param maxTcpSockets
     * @param selector
     * @param selectorLock
     * @throws IOException
     */
    public TunnelAgent(Integer maxTcpSockets, Selector selector, ReentrantLock selectorLock) throws IOException {
        this.availableSockets = new LinkedList<>();

        this.connectedSockets = 0;
        this.maxTcpSockets = maxTcpSockets;
        this.channel = ServerSocketChannel.open();
        this.selector = selector;
        this.lock = selectorLock;
    }

    /**
     * make listen
     * @param port port
     * @return port
     * @throws IOException
     */
    public Integer listen(Integer port) throws IOException {
        if (this.started) {
            throw new RuntimeException("already started");
        }
        this.started = true;


        channel.bind(new InetSocketAddress(port), this.maxTcpSockets);
        channel.configureBlocking(false);

        //sync selector register and select
        lock.lock();
        selector.wakeup();
        channel.register(selector, SelectionKey.OP_ACCEPT, this);
        lock.unlock();

        return channel.socket().getLocalPort();
    }

    /**
     * make listen
     * @return port
     * @throws IOException
     */
    public Integer listen() throws IOException {
        return listen(0);
    }

    /**
     * pull socket from available
     * @return Socket
     */
    public synchronized Socket pullConnection(){
        if (this.closed) {
            throw new RuntimeException("Tunnel agent already closed!");
        }

        logger.debug("Create connection use tunnel agent cached!");

        // socket is a tcp connection back to the user hosting the site
        Socket sock = this.availableSockets.poll();
        this.connectedSockets--;

        // no available sockets
        if (sock == null) {
            logger.warn("No available sockets form tunnel agent!");
            return null;
        }

        logger.debug("Already got socket form tunnel agent!");
        return sock;
    }

    /**
     * push socket to available
     * @param socket Socket
     * @return ok
     * @throws IOException
     */
    public synchronized boolean pushConnection(Socket socket) throws IOException {
        if (TunnelAgent.this.connectedSockets >= TunnelAgent.this.maxTcpSockets) {
            logger.debug("Exceeded the maximum number of connections", socket.getInetAddress().getHostAddress(), socket.getPort());
            socket.close();
            return false;
        }

        logger.debug("New connection from: {}:{}", socket.getInetAddress().getHostAddress(), socket.getPort());
        this.connectedSockets += 1;
        this.availableSockets.add(socket);
        return true;
    }


    /**
     * Select runnable of Selector thread
     */
    public static class SelectorSocketAcceptHandler implements Runnable {

        private static Logger logger = LoggerFactory.getLogger(SelectorSocketAcceptHandler.class);

        private Selector selector;
        private ReentrantLock lock;

        public SelectorSocketAcceptHandler(Selector selector, ReentrantLock lock){
            this.selector = selector;
            this.lock = lock;
        }

        public void run() {
            logger.debug("Start to tunnel socket select loop!");
            while (true) {
                try {
                    //sync other thread of selector register
                    lock.lock();
                    lock.unlock();
                    //
                    int readyChannels = selector.select();
                    if(readyChannels == 0) continue;

                    logger.debug("Selected count {} channel!", readyChannels);
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while(keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        if(key.isAcceptable()) {
                            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                            TunnelAgent agent = (TunnelAgent) key.attachment();
                            agent.pushConnection(channel.accept().socket());
                        }
                        keyIterator.remove();
                    }
                } catch (IOException e) {
                    logger.error("Socket accept error!",e);
                }
            }
        }
    }

}
