package org.yuan.imooc.client;

import org.yuan.imooc.common.TCPConstants;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        TCPClient client = null;

        try {
            client = TCPClient.startWith(TCPConstants.ADDR_SERVER, TCPConstants.PORT_SERVER);
            if(client == null) {
                return;
            }
            write(client);
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        finally {
            if(client != null) {
                client.exit();
            }
        }
    }

    private static void write(TCPClient client) {
        Scanner scan = new Scanner(System.in);

        do {
            String line = scan.nextLine();
            client.send(line);

            if ("bye".equalsIgnoreCase(line)) {
                break;
            }
        } while(true);
    }

}
