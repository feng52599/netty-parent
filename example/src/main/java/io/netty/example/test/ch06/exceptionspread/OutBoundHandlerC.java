package io.netty.example.test.ch06.exceptionspread;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;

/**
 * @author
 */
public class OutBoundHandlerC extends ChannelOutboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("OutBoundHandlerC.exceptionCaught()");
        ctx.fireExceptionCaught(cause);
    }
}

