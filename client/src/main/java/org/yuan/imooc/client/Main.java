package org.yuan.imooc.client;

import org.yuan.imooc.common.TCPConstants;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            TCPClient.linkWith(TCPConstants.ADDR_SERVER, TCPConstants.PORT_SERVER);
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }

}
