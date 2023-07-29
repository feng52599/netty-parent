package io.netty.example.test.ch3;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.example.test.ch06.AuthHandler;
import io.netty.util.AttributeKey;

/**
 * @author
 */
public class Server {

    // socket 在哪里初始化=>反射，在哪里创建连接
    public static void main(String[] args) throws Exception {
        // 处理IO
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)// 设置socketChannel, 同时返回channelFactory，为构造channel后续通过反射构造NioServerSocketChannel 做准备
                    .childOption(ChannelOption.TCP_NODELAY, true)// 每个连接设置属性
                    .childAttr(AttributeKey.newInstance("childAttr"), "childAttrValue")// 连接创建时绑定属性
                    .handler(new ServerHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new AuthHandler());
                            //数据流读写逻辑

                        }
                    });
            // channel 创建入口
            ChannelFuture f = b.bind(8888).sync();

            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}