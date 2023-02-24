package io.netty.example.test.javaNio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * @program: NioProject
 * @description:
 * @author: feng
 * @create: 2023-02-20 21:13
 * @version: 1.0
 */
public class JavaNIOServerExample {
    public static void main(String[] args) throws IOException {
        // 开启socket和selector
        // 创建了一个服务端的channel，也就是服务端的Socket。（在计算机网路的7层模型或者TCP/IP模型中，上层的应用程序通过Socket来和底层沟通）
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 这个轮询器就是后续用来从操作系统中，遍历有哪些socket准备好了
        Selector selector = Selector.open();
        serverSocketChannel.bind(new InetSocketAddress(8001));
        // 设置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        // 注册selector 的Accept时间
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // 调用轮询器的select()方法，是让轮询器从操作系统上获取所有的事件（例如：新客户端的接入、数据的写入、数据的写出等事件）
            selector.select(200);
            // 调用select()方法后，轮询器将查询到的事件全部放入到了selectedKeys中
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // 接受事件处理
                if (key.isAcceptable()) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    System.out.println("有新客户端来连接");
                    socketChannel.configureBlocking(false);
                    // 有新的客户端接入后，就样将客户端对应的channel所感兴趣的时间是可读事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                }
                // 可读事件处理
                if (key.isReadable()) {
                    // 从channel中读取数据
                    SocketChannel channel = (SocketChannel) key.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int read = channel.read(byteBuffer);
                    byteBuffer.flip();
                    System.out.println(Charset.defaultCharset().decode(byteBuffer));
                    // 读完了以后，再次将channel所感兴趣的时间设置为读事件，方便下次继续读。当如果后面要想往客户端写数据，那就注册写时间：SelectionKey.OP_WRITE
                    channel.register(selector,SelectionKey.OP_READ);
                }

                iterator.remove();
            }
        }
    }
}