package io.netty.example.test.bio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
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
public class BIOClientExample {
    public static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1",8000);
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    // 向服务端发送消息（消息内容为：客户端的ip+端口+Hello World，示例：/127.0.0.1:999999 Hello World）
                    String message = socket.getLocalSocketAddress().toString() + " Hello World";
                    outputStream.write(message.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }}, 0L, 3L, TimeUnit.SECONDS);

    }
}