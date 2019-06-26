package org.yuan.imooc.client;

import org.yuan.imooc.common.TCPConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainTest {
    private static boolean done;

    public static void main(String[] args) throws IOException {
        int size = 0;
        final List<TCPClient> clientList = new ArrayList<>();
        for (int i=0; i<10; i++) {
            try {
                TCPClient client = TCPClient.startWith(TCPConstants.ADDR_SERVER, TCPConstants.PORT_SERVER);
                if(client == null) {
                    System.out.println("连接异常");
                    continue;
                }

                clientList.add(client);

                size++;
                System.out.println("连接成功: " + size);
            }
            catch(IOException ex) {
                System.out.println("连接异常");
            }

            try {
                Thread.sleep(20);
            }
            catch(InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        System.in.read();

        Thread thread = new Thread(() -> {
            while (!done) {
                for (TCPClient client : clientList) {
                    client.send("Hello...");
                }
                try {
                    Thread.sleep(1000);
                }
                catch(InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
        thread.start();

        System.in.read();

        done = true;
        try {
            thread.join();
        }
        catch(InterruptedException ex) {
            ex.printStackTrace();
        }

        for (TCPClient client : clientList) {
            client.exit();
        }
    }
}
