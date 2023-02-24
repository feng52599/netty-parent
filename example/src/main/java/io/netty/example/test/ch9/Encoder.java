package io.netty.example.test.ch9;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.example.test.User;
import io.netty.handler.codec.MessageToByteEncoder;

public class Encoder extends MessageToByteEncoder<User> {
    @Override
    protected void encode(ChannelHandlerContext ctx, User msg, ByteBuf out) throws Exception {
        byte[] bytes = msg.getUsername().getBytes();
        out.writeInt(4 + bytes.length);
        out.writeInt(msg.getAge());
        out.writeBytes(bytes);
    }
}
