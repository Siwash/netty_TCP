package org.wisdom.dataTransefer;

import java.util.Arrays;

/**通过全类名字符串解析成具体的对象
 * **/
public class DTObject {
    private String className;
    private byte[] object;

    @Override
    public String toString() {
        return "DTObject{" +
                "className='" + className + '\'' +
                ", object=" + Arrays.toString(object) +
                '}';
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public byte[] getObject() {
        return object;
    }

    public void setObject(byte[] object) {
        this.object = object;
    }
}
