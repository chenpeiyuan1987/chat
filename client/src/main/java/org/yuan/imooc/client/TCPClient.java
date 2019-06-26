package org.yuan.imooc.client;

import org.yuan.imooc.common.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClient {
    private final Socket socket;
    private final ReadHandler readHandler;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ReadHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit() {
        readHandler.exit();
        CloseUtils.close(socket);
        CloseUtils.close(printStream);
    }

    public void send(String line) {
        printStream.println(line);
    }

    public static TCPClient startWith(String addr, int port) throws IOException {
        Socket socket = new Socket();

        socket.setSoTimeout(3000);
        socket.connect(new InetSocketAddress(Inet4Address.getByName(addr), port), 3000);

        System.out.println("已发起服务端连接");
        System.out.printf("客户端信息: %s:%s\n", socket.getLocalAddress(), socket.getLocalPort());
        System.out.printf("服务端信息: %s:%s\n", socket.getInetAddress(), socket.getPort());

        try {
            ReadHandler handler = new ReadHandler(socket.getInputStream());
            handler.start();

            return new TCPClient(socket, handler);
        }
        catch(Exception ex) {
            System.out.println("连接异常");
            CloseUtils.close(socket);
        }

        return null;
    }

    static class ReadHandler extends Thread {
        private boolean done = false;
        private final InputStream stream;

        public ReadHandler(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                do {
                    String line;
                    try {
                        line = reader.readLine();
                    }
                    catch(SocketTimeoutException ex) {
                        continue;
                    }
                    if(line == null) {
                        System.out.println("连接已关闭，无法读取数据");
                        break;
                    }
                    System.out.println(line);
                } while(!done);
            }
            catch(Exception ex) {
                if(!done) {
                    System.out.println("连接异常断开: " + ex.getMessage());
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
}
