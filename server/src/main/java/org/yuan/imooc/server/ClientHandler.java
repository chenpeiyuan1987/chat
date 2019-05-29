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
    private final CloseNotify closeNotify;

    public ClientHandler(Socket socket, CloseNotify closeNotify) throws IOException {
        this.socket = socket;
        this.readHandler = new ClientReadHandler(socket.getInputStream());
        this.sendHandler = new ClientSendHandler(socket.getOutputStream());
        this.closeNotify = closeNotify;

        System.out.printf("新客户端连接: %s:%s\n", socket.getInetAddress(), socket.getPort());
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
        closeNotify.onSelfClosed(this);
    }

    public interface CloseNotify {
        void onSelfClosed(ClientHandler handler);
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
                    System.out.println(line);
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
