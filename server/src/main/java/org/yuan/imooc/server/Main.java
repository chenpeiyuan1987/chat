package org.yuan.imooc.server;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        TCPServer server = new TCPServer(10000);
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
