package org.yuan.imooc.common;

import java.io.Closeable;

public class CloseUtils {

    public static void close(Closeable... closeables) {
        for(Closeable closeable : closeables) {
            try {
                closeable.close();
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
