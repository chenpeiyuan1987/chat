package org.yuan.imooc.server;

import org.yuan.imooc.common.CloseUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener listener;
    private List<ClientHandler> handlerList = new ArrayList<>();
    private final ExecutorService service;
    private Selector selector;
    private ServerSocketChannel server;

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
            selector = Selector.open();
            server = ServerSocketChannel.open();
            // 设置非阻塞
            server.configureBlocking(false);
            // 绑定本地端口
            server.socket().bind(new InetSocketAddress(port));
            // 注册客户端连接到达监听
            server.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("服务器信息: " + server.getLocalAddress().toString());

            listener = new ClientListener();
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

        CloseUtils.close(server, selector);

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

        @Override
        public void run() {
            Selector selector = TCPServer.this.selector;
            System.out.println("服务器已就绪");

            do {
                try {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }

                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isAcceptable()) {
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel client = server.accept();

                            try {
                                ClientHandler handler = new ClientHandler(client, TCPServer.this);
                                handler.readToPrint();
                                synchronized (TCPServer.this) {
                                    handlerList.add(handler);
                                }
                            }
                            catch (IOException ex) {
                                ex.printStackTrace();
                                System.out.println("客户端连接异常：" + ex.getMessage());
                            }
                        }
                    }
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            } while(!done);

            System.out.println("服务器已关闭");
        }

        void exit() {
            done = true;
            // 唤醒阻塞
            selector.wakeup();
        }
    }
}
