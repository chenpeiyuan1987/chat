package org.yuan.imooc.common;

public class ByteUtils {

    public static boolean startsWith(byte[] source, byte[] match) {
        return startsWith(source, 0, match);
    }

    public static boolean startsWith(byte[] source, int offset, byte[] match) {
        if(match.length > (source.length - offset)) {
            return false;
        }

        for(int i=0; i<match.length; i++) {
            if(source[offset + i] != match[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(byte[] source, byte[] match) {
        if(match.length != source.length) {
            return false;
        }
        return startsWith(source, 0, match);
    }

    public static void getBytes(byte[] src, int srcBegin, int srcFinis, byte[] dst, int dstBegin) {
        System.arraycopy(src, srcBegin, dst, dstBegin, srcFinis - srcBegin);
    }

    public static byte[] subbytes(byte[] src, int srcBegin, int srcFinis) {
        byte[] dst;
        dst = new byte[srcFinis - srcBegin];
        getBytes(src, srcBegin, srcFinis, dst, 0);
        return dst;
    }

    public static byte[] subbytes(byte[] src, int srcBegin) {
        return subbytes(src, srcBegin, src.length);
    }
}
