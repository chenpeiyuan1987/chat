package org.yuan.imooc.server;

import org.yuan.imooc.common.CloseUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPServer {
    private final int port;
    private ClientListener listener;
    private List<ClientHandler> handlerList = new ArrayList<>();

    public TCPServer(int port) {
        this.port = port;
    }

    /**
     * 启动
     * @return
     */
    public boolean start() {
        try {
            listener = new ClientListener(port);
            listener.start();
        }
        catch(IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 广播
     * @param line
     */
    public void broadcast(String line) {
        for(ClientHandler handler : handlerList) {
            handler.send(line);
        }
    }

    /**
     * 关闭
     */
    public void stop() {
        if(listener != null) {
            listener.exit();
        }

        for(ClientHandler handler : handlerList) {
            handler.exit();
        }
        handlerList.clear();
    }

    private class ClientListener extends Thread {
        private boolean done = false;
        private ServerSocket server;

        public ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.printf("服务器信息: %s:%s\n", server.getInetAddress(), server.getLocalPort());
        }

        @Override
        public void run() {
            System.out.println("服务器已就绪");

            do {
                Socket client;
                try {
                    client = server.accept();
                }
                catch(IOException ex) {
                    continue;
                }

                try {
                    ClientHandler handler = new ClientHandler(client, new ClientHandler.CloseNotify() {
                        @Override
                        public void onSelfClosed(ClientHandler handler) {
                            handlerList.remove(handler);
                        }
                    });
                    handler.readToPrint();
                    handlerList.add(handler);
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                    System.out.println("客户端连接异常: " + ex.getMessage());
                }
            } while(!done);

            System.out.println("服务器已关闭");
        }

        void exit() {
            done = true;
            CloseUtils.close(server);
        }
    }
}
