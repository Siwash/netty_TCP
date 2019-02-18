package org.wisdom.client.echo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.wisdom.dataTransefer.DTObject;
import org.wisdom.protocol.TcpProtocol;
import org.wisdom.utils.ByteUtils;
import pojo.User;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class EchoHandler extends ChannelInboundHandlerAdapter {

    //连接成功后发送消息测试
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        User user = new User();
        user.setBirthday(new Date());
        user.setUID(UUID.randomUUID().toString());
        user.setName("冉鹏峰");
        user.setAge(24);
        DTObject dtObject = new DTObject();
        dtObject.setClassName(user.getClass().getName());
        dtObject.setObject(ByteUtils.InstanceObjectMapper().writeValueAsBytes(user));
        TcpProtocol tcpProtocol = new TcpProtocol();
        byte [] objectBytes=ByteUtils.InstanceObjectMapper().writeValueAsBytes(dtObject);
        tcpProtocol.setLen(objectBytes.length);
        tcpProtocol.setData(objectBytes);
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeByte(tcpProtocol.getHeader());
        buffer.writeInt(tcpProtocol.getLen());
        buffer.writeBytes(Arrays.copyOfRange(tcpProtocol.getData(),0,tcpProtocol.getLen()/2));

        ctx.write(buffer);
        ctx.flush();
        Thread.sleep(3000);
        buffer = ctx.alloc().buffer();
        buffer.writeBytes(Arrays.copyOfRange(tcpProtocol.getData(),tcpProtocol.getLen()/2,tcpProtocol.getLen()));
        buffer.writeByte(tcpProtocol.getTail());
        //模拟粘包的第二帧数据
        buffer.writeByte(tcpProtocol.getHeader());
        buffer.writeInt(tcpProtocol.getLen());
        buffer.writeBytes(tcpProtocol.getData());
        buffer.writeByte(tcpProtocol.getTail());
        //模拟粘包的第三帧数据
        buffer.writeByte(tcpProtocol.getHeader());
        buffer.writeInt(tcpProtocol.getLen());
        buffer.writeBytes(tcpProtocol.getData());
        buffer.writeByte(tcpProtocol.getTail());
        ctx.write(buffer);
        ctx.flush();

    }
}
