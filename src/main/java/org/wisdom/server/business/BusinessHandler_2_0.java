package org.wisdom.server.business;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import org.wisdom.dataTransefer.DTObject_2_0;
import org.wisdom.utils.ByteUtils;
import pojo.User;

import java.util.List;
import java.util.Map;

public class BusinessHandler_2_0 extends ChannelInboundHandlerAdapter {
    private ObjectMapper objectMapper= ByteUtils.InstanceObjectMapper();
    private Logger logger = Logger.getLogger(this.getClass());
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof List){
            logger.info("这是一个List:"+(List)msg);
        }else if (msg instanceof Map){
            logger.info("这是一个Map:"+(Map)msg);
        }else{
            logger.info("这是一个对象："+msg.getClass().getName());
            logger.info("这是一个对象："+msg);
        }
    }
}
