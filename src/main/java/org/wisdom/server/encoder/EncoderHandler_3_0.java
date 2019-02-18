package org.wisdom.server.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.log4j.Logger;
import org.wisdom.protocol.TcpProtocol_3_0;

public class EncoderHandler_3_0 extends MessageToByteEncoder {
    private  Logger logger = Logger.getLogger(this.getClass());
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof TcpProtocol_3_0){
            TcpProtocol_3_0 protocol = (TcpProtocol_3_0) msg;
            out.writeByte(protocol.getHeader());
            out.writeByte(protocol.getType());
            out.writeByte(protocol.getClassLen());
            out.writeInt(protocol.getLen());
            out.writeBytes(protocol.getClassName());
            out.writeBytes(protocol.getData());
            out.writeByte(protocol.getTail());
            logger.debug("数据编码成功："+out);
        }else {
            logger.info("不支持的数据协议："+msg.getClass()+"\t期待的数据协议类是："+ TcpProtocol_3_0.class);
        }
    }
}
