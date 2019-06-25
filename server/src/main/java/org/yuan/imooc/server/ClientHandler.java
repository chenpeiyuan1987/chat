package org.yuan.imooc.server;

import org.yuan.imooc.common.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientSendHandler sendHandler;
    private final ClientHandlerCallback callback;
    private final String clientInfo;


    public ClientHandler(Socket socket, ClientHandlerCallback callback) throws IOException {
        this.socket = socket;
        this.readHandler = new ClientReadHandler(socket.getInputStream());
        this.sendHandler = new ClientSendHandler(socket.getOutputStream());
        this.callback = callback;
        this.clientInfo = String.format("%s:%s", socket.getInetAddress(), socket.getPort());
        System.out.printf("新客户端连接: %s\n", clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void exit() {
        readHandler.exit();
        sendHandler.exit();
        CloseUtils.close(socket);
        System.out.printf("客户端已退出: %s:%s\n", socket.getInetAddress(), socket.getPort());
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
        private boolean done = true;
        private final InputStream stream;

        public ClientReadHandler(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                do {
                    String line = reader.readLine();
                    if(line == null) {
                        System.out.println("客户端无法读取数据");
                        ClientHandler.this.exitBySelf();
                        break;
                    }
                    callback.onNewMessageArrived(ClientHandler.this, line);
                } while(!done);
            }
            catch(Exception ex) {
                if(!done) {
                    System.out.println("连接异常断开");
                    ClientHandler.this.exitBySelf();
                }
            }
            finally {
                CloseUtils.close(stream);
            }
        }

        void exit() {
            done = true;
            CloseUtils.close(stream);
        }
    }

    class ClientSendHandler {
        private boolean done = false;
        private final PrintStream stream;
        private final ExecutorService service;

        public ClientSendHandler(OutputStream output) {
            this.stream = new PrintStream(output);
            this.service = Executors.newSingleThreadExecutor();
        }

        void exit() {
            done = true;
            CloseUtils.close(stream);
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
                this.msg = msg;
            }

            @Override
            public void run() {
                if(ClientSendHandler.this.done) {
                    return;
                }

                try {
                    ClientSendHandler.this.stream.println(msg);
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
