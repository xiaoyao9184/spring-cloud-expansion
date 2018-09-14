package com.xy.spring.cloud.zuul.tunnel.apache;

import com.netflix.zuul.context.RequestContext;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.Client;
import com.xy.spring.cloud.zuul.tunnel.localtunnel.ClientManager;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by xiaoyao9184 on 2018/7/13.
 */
public class TunnelPlainConnectionSocketFactory extends PlainConnectionSocketFactory implements ConnectionSocketFactory {

    private static Logger logger = LoggerFactory.getLogger(TunnelPlainConnectionSocketFactory.class);

    private ClientManager clientManager;

    public void setClientManager(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public Socket createSocket(HttpContext httpContext) throws IOException {
        String clientId = null;
        //use zuul attribute for clientId
        Object contextAttribute = httpContext.getAttribute("zuul.request-context");
        if(contextAttribute instanceof RequestContext){
            RequestContext context = (RequestContext) contextAttribute;
            logger.debug("Found zuul RequestContext in HttpContext!");
            clientId = context.get("proxy").toString();
        }
        //use tunnel id for clientId
        String tunnelId = (String) httpContext.getAttribute("tunnel.id");
        if(!StringUtils.isEmpty(tunnelId)){
            clientId = tunnelId;
        }

        Client client = clientManager.getClient(clientId);
        if(client != null){
            logger.debug("Use tunnel client '{}' socket!", clientId);
            Socket socket = client.getAgent().pullConnection();
            if(socket != null){
                return socket;
            }
            logger.debug("Tunnel client '{}' cant available any socket!", clientId);
        }
        logger.debug("Cant find tunnel client '{}' socket!", clientId);

        logger.debug("Create socket use normal way!");
        return super.createSocket(httpContext);
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket socket, HttpHost httpHost, InetSocketAddress inetSocketAddress, InetSocketAddress inetSocketAddress1, HttpContext httpContext) throws IOException {
        if(socket.isClosed()){
            logger.warn("Tunnel client socket already closed");
        }

        if(!socket.isConnected()){
            logger.debug("Connect socket use normal way!");
            return super.connectSocket(connectTimeout, socket, httpHost, inetSocketAddress, inetSocketAddress1, httpContext);
        }

        return socket;
    }

}
