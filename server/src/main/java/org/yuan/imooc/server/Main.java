package org.yuan.imooc.server;

import org.yuan.imooc.common.TCPConstants;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        TCPServer server = new TCPServer(TCPConstants.PORT_SERVER);
        boolean success = server.start();
        if(!success) {
            System.out.println("服务器启动失败");
            return;
        }

        Scanner scan = new Scanner(System.in);
        String line;
        do {
            line = scan.nextLine();
            server.broadcast(line);
        } while("bye".equalsIgnoreCase(line));

        server.stop();
    }

}
