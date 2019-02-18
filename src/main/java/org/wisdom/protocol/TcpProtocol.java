package org.wisdom.protocol;

import java.util.Arrays;

public class TcpProtocol {
    private byte header=0x58;
    private int len;
    private byte [] data;
    private byte tail=0x63;

    public byte getTail() {
        return tail;
    }

    public void setTail(byte tail) {
        this.tail = tail;
    }

    public TcpProtocol(int len, byte[] data) {
        this.len = len;
        this.data = data;
    }

    public TcpProtocol() {
    }

    @Override
    public String toString() {
        return "TcpProtocol{" +
                "header=" + header +
                ", len=" + len +
                ", data=" + Arrays.toString(data) +
                ", tail=" + tail +
                '}';
    }

    public byte getHeader() {
        return header;
    }

    public void setHeader(byte header) {
        this.header = header;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
