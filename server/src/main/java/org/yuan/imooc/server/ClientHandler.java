package org.yuan.imooc.server;

import org.yuan.imooc.common.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final SocketChannel socketChannel;
    private final ClientReadHandler readHandler;
    private final ClientSendHandler sendHandler;
    private final ClientHandlerCallback callback;
    private final String clientInfo;


    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback callback) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);

        Selector readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        readHandler = new ClientReadHandler(readSelector);

        Selector sendSelector = Selector.open();
        socketChannel.register(sendSelector, SelectionKey.OP_WRITE);
        sendHandler = new ClientSendHandler(sendSelector);

        this.callback = callback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接: " + clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void exit() {
        readHandler.exit();
        sendHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出: " + clientInfo);
    }

    public void send(String str) {
        sendHandler.send(str);
    }

    public void readToPrint() {
        readHandler.start();
    }

    public void exitBySelf() {
        exit();
        callback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        /**
         * 自身关闭通知
         * @param handler
         */
        void onSelfClosed(ClientHandler handler);

        /**
         * 收到消息通知
         * @param handler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler handler, String msg);

    }

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        public ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run() {
            try {
                do {
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

                        if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();

                            byteBuffer.clear();

                            int read = client.read(byteBuffer);
                            if (read > 0) {
                                String str = new String(byteBuffer.array(), 0, read - 1);
                                callback.onNewMessageArrived(ClientHandler.this, str);
                            }
                            else {
                                System.out.println("客户端已无法读取数据！");
                                ClientHandler.this.exitBySelf();
                                break;
                            }
                        }
                    }
                } while(!done);
            }
            catch(Exception ex) {
                if(!done) {
                    System.out.println("连接异常断开");
                    ClientHandler.this.exitBySelf();
                }
            }
            finally {
                CloseUtils.close(selector);
            }
        }

        void exit() {
            done = true;
            selector.wakeup();
            CloseUtils.close(selector);
        }
    }

    class ClientSendHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService service;

        public ClientSendHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.service = Executors.newSingleThreadExecutor();
        }

        void exit() {
            done = true;
            CloseUtils.close(selector);
            service.shutdownNow();
        }

        void send(String str) {
            if (done) {
                return;
            }
            service.execute(new SendRunnable(str));
        }

        class SendRunnable implements Runnable {
            private final String msg;

            public SendRunnable(String msg) {
                this.msg = msg + "\n";
            }

            @Override
            public void run() {
                if(ClientSendHandler.this.done) {
                    return;
                }

                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());
                byteBuffer.flip();

                while (!done && byteBuffer.hasRemaining()) {
                    try {
                        int len = socketChannel.write(byteBuffer);
                        if (len < 0) {
                            System.out.println("客户端已无法发送数据！");
                            ClientHandler.this.exitBySelf();
                            break;
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
