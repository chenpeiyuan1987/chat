package org.yuan.imooc.server;

public class TCPServer {
    private final int port;

    public TCPServer(int port) {
        this.port = port;
    }

    /**
     * 启动
     * @return
     */
    public boolean start() {
        return false;
    }

    /**
     * 广播
     * @param line
     */
    public void broadcast(String line) {
    }

    /**
     * 关闭
     */
    public void stop() {
    }
}
