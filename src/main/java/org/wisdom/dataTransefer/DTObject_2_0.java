package org.wisdom.dataTransefer;

import java.util.Arrays;

/**通过全类名字符串解析成具体的对象
 * **/
public class DTObject_2_0<T> {
    private String className;
    private T object;

    public DTObject_2_0(String className, T object) {
        this.className = className;
        this.object = object;
    }

    @Override
    public String toString() {
        return "DTObject_2_0{" +
                "className='" + className + '\'' +
                ", object=" + object +
                '}';
    }

    public DTObject_2_0() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }
}
