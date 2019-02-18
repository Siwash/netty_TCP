package org.wisdom.protocol;

import java.util.Arrays;

/**
 *type      0x51    0x52    0x53
 * mean:    object  list    map
 * */
public class TcpProtocol_3_0 {
    private byte header=0x58;
    private byte type;
    private byte classLen;
    private int len;
    private byte[] className;
    private byte [] data;
    private byte tail=0x63;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getClassLen() {
        return classLen;
    }

    public void setClassLen(byte classLen) {
        this.classLen = classLen;
    }

    public byte[] getClassName() {
        return className;
    }

    public void setClassName(byte[] className) {
        this.className = className;
    }



    public byte getTail() {
        return tail;
    }

    public void setTail(byte tail) {
        this.tail = tail;
    }

    public TcpProtocol_3_0(int len, byte[] data) {
        this.len = len;
        this.data = data;
    }

    public TcpProtocol_3_0(byte type, byte classLen, int len, byte[] className, byte[] data) {
        this.type = type;
        this.classLen = classLen;
        this.len = len;
        this.className = className;
        this.data = data;
    }

    public TcpProtocol_3_0() {
    }

    @Override
    public String toString() {
        return "TcpProtocol_3_0{" +
                "header=" + header +
                ", type=" + type +
                ", classLen=" + classLen +
                ", len=" + len +
                ", className=" + Arrays.toString(className) +
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
