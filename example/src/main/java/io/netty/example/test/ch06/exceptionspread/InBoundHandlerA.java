package io.netty.example.test.ch06.exceptionspread;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author
 */
public class InBoundHandlerA extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("InBoundHandlerA.exceptionCaught()");

        ctx.fireExceptionCaught(cause);
    }
}
