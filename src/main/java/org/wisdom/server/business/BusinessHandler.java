package org.wisdom.server.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import org.wisdom.dataTransefer.DTObject;
import org.wisdom.protocol.TcpProtocol;
import org.wisdom.utils.ByteUtils;

public class BusinessHandler extends ChannelInboundHandlerAdapter {
    private ObjectMapper objectMapper= ByteUtils.InstanceObjectMapper();
    private Logger logger = Logger.getLogger(this.getClass());
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof byte []){
            logger.debug("解码后的字节码："+new String((byte[]) msg,"UTF-8"));
            try {
                Object objectContainer = objectMapper.readValue((byte[]) msg, DTObject.class);
                if (objectContainer instanceof DTObject){
                    DTObject data = (DTObject) objectContainer;
                    if (data.getClassName()!=null&&data.getObject().length>0){
                        Object object = objectMapper.readValue(data.getObject(), Class.forName(data.getClassName()));
                        logger.info("收到实体对象："+object);
                    }
                }
            }catch (Exception e){
                logger.info("对象反序列化出现问题："+e);
            }

        }
    }
}
