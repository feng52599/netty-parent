package io.netty.example.test.javaNio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @program: NioProject
 * @description:
 * @author: feng
 * @create: 2023-02-20 21:26
 * @version: 1.0
 */
public class JavaNioClientExample {
    public static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
    public static void main(String[] args) throws IOException {
        SocketChannel clientSocketChannel = SocketChannel.open();
        boolean connect = clientSocketChannel.connect(new InetSocketAddress(8001));
        // 因为连接是一个异步操作，所以需要在下面一行判断连接有没有完成。如果连接还没有完成，就进行后面的操作，会出现异常
        if (!connect) {
            // 如果连接未完成，就等待连接完成
            clientSocketChannel.finishConnect();
        }
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    String message = clientSocketChannel.getLocalAddress() + " Hello World";
                    clientSocketChannel.write(ByteBuffer.wrap(message.getBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }}, 0L, 1L, TimeUnit.SECONDS);

    }
}