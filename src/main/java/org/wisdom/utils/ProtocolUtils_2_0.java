package org.wisdom.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.wisdom.dataTransefer.DTObject;
import org.wisdom.protocol.TcpProtocol_2_0;
import org.wisdom.protocol.TcpProtocol_3_0;

import java.util.List;
import java.util.Map;

public class ProtocolUtils_2_0 {
    public final static byte OBJ_TYPE=0x51;
    public final static byte LIST_TYPE=0x52;
    public final static byte MAP_TYPE=0x53;
    /**
     * 创建集合类list map对象
     * */
    public static TcpProtocol_2_0 prtclInstance(Object o, String className){
        TcpProtocol_2_0 protocol = new TcpProtocol_2_0();
        if (o instanceof List){
            protocol.setType(LIST_TYPE);
        }else if (o instanceof Map){
            protocol.setType(MAP_TYPE);
        }else if (o instanceof Object){
            protocol.setType(OBJ_TYPE);
        }
        initProtocol(o, className, protocol);

        return protocol;
    }
    /***
     *
     * 创建单一的对象
     */
    public static TcpProtocol_2_0 prtclInstance(Object o){
        TcpProtocol_2_0 protocol = new TcpProtocol_2_0();
        protocol.setType(OBJ_TYPE);
        initProtocol(o, o.getClass().getName(), protocol);

        return protocol;
    }

    private static void initProtocol(Object o, String className, TcpProtocol_2_0 protocol) {
        try {
            DTObject dtObject = new DTObject();
            byte [] objectBytes= ByteUtils.InstanceObjectMapper().writeValueAsBytes(o);
            dtObject.setObject(objectBytes);
            dtObject.setClassName(className);
            byte[] bytes = ByteUtils.InstanceObjectMapper().writeValueAsBytes(dtObject);
            protocol.setLen(bytes.length);
            protocol.setData(bytes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
