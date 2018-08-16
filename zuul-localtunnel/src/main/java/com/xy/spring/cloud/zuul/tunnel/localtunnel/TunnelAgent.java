package com.xy.spring.cloud.zuul.tunnel.localtunnel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by xiaoyao9184 on 2018/7/13.
 */
public class TunnelAgent {

    private static Logger logger = LoggerFactory.getLogger(TunnelAgent.class);

    // sockets we can hand out via createConnection
    private Queue<Socket> availableSockets;



    // when a createConnection cannot return a socket, it goes into a queue
    // once a socket is available it is handed out to the next callback

    // track maximum allowed sockets
    private Integer connectedSockets;
    private Integer maxTcpSockets;

    // new tcp server to service requests for this client
    private ServerSocket serverSocket;
    private SocketAcceptHandler socketAcceptHandler;
    private Thread socketAcceptThread;

    // flag to avoid double starts
    private Boolean started = false;
    private Boolean closed = false;

    public TunnelAgent(String name, Integer maxTcpSockets) throws IOException {
        this.availableSockets = new LinkedList<>();

        this.connectedSockets = 0;
        this.maxTcpSockets = maxTcpSockets;
    }

    public Integer listen(Integer port) throws IOException {
        if (this.started) {
            throw new RuntimeException("already started");
        }
        this.started = true;

        this.serverSocket = new ServerSocket(port);
        this.socketAcceptHandler = new SocketAcceptHandler(serverSocket);
        this.socketAcceptThread = new Thread(TUNNEL_GROUP,socketAcceptHandler,"tunnel-socket-accept");

        this.socketAcceptThread.start();
        return serverSocket.getLocalPort();
    }

    public Integer listen() throws IOException {
        return listen(0);
    }

    public synchronized Socket createConnection(){
        if (this.closed) {
            throw new RuntimeException("closed");
        }

        logger.debug("create connection");

        // socket is a tcp connection back to the user hosting the site
        Socket sock = this.availableSockets.poll();
        this.connectedSockets--;

        // no available sockets
        // wait until we have one
        if (sock == null) {
//            this.waitingCreateConn.push(cb);
//            this.debug('waiting connected: %s', this.connectedSockets);
//            this.debug('waiting available: %s', this.availableSockets.length);
            return null;
        }

        logger.debug("socket given");
        return sock;
    }

    public synchronized boolean newConnection(Socket socket) throws IOException {
        if (TunnelAgent.this.connectedSockets >= TunnelAgent.this.maxTcpSockets) {
            socket.close();
            return false;
        }

        logger.debug("new connection from: {}:{}", socket.getInetAddress().getHostAddress(), socket.getPort());
        this.connectedSockets += 1;
        this.availableSockets.add(socket);
        return true;
    }

    /**
     * Handle socket accept
     */
    private class SocketAcceptHandler implements Runnable {
        private ServerSocket serverSocket;
        public SocketAcceptHandler(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {
            while (true) {
                try {
                    // if accept return it mean some client connected.
                    Socket socket = serverSocket.accept();
                    //TODO maybe stop when cant create new connection
                    newConnection(socket);
                } catch (IOException e) {
                    logger.error("Socket accept error!",e);
                }
            }
        }
    }

    public static ThreadGroup TUNNEL_GROUP = tunnelGroup();

    private static ThreadGroup tunnelGroup(){
        ThreadGroup group = new ThreadGroup("Tunnel");
        return group;
    }

}
