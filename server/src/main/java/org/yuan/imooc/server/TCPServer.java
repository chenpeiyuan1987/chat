package org.yuan.imooc.server;

import org.yuan.imooc.common.CloseUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener listener;
    private List<ClientHandler> handlerList = new ArrayList<>();
    private final ExecutorService service;

    public TCPServer(int port) {
        this.port = port;
        this.service = Executors.newSingleThreadExecutor();
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
    public synchronized void broadcast(String line) {
        for(ClientHandler handler : handlerList) {
            handler.send(line);
        }
    }

    @Override
    public void onSelfClosed(ClientHandler handler) {
        handlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        // 打印到屏幕
        System.out.println("Received-" + handler.getClientInfo() + ":" + msg);

        // 异步提交转发任务
        service.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler item : handlerList) {
                    if (item.equals(handler)) {
                        continue;
                    }
                    item.send(msg);
                }
            }
        });
    }

    /**
     * 关闭
     */
    public void stop() {
        if(listener != null) {
            listener.exit();
        }

        synchronized(TCPServer.this) {
            for (ClientHandler handler : handlerList) {
                handler.exit();
            }
            handlerList.clear();
        }

        service.shutdownNow();
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
                    ClientHandler handler = new ClientHandler(client, TCPServer.this);
                    handler.readToPrint();
                    synchronized(TCPServer.this) {
                        handlerList.add(handler);
                    }
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
