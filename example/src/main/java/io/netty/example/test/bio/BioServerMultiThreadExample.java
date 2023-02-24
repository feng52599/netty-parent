package io.netty.example.test.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @program: NioProject
 * @description:
 * @author: feng
 * @create: 2023-02-20 21:37
 * @version: 1.0
 */
public class BioServerMultiThreadExample {
    public static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8000);
        try {
            while (true) {
                Socket socketInstance = serverSocket.accept();
                readMessage(socketInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readMessage(Socket socketInstance) throws IOException {
        new Thread(() -> {
            try {
                InputStream inputStream = socketInstance.getInputStream();
                byte[] bytes = new byte[1024];
                int len = 0;
                while ((len = inputStream.read(bytes)) != -1) {
                    System.out.println(new String(bytes, 0, len));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        // executorService.submit(new Runnable() {
        //     @Override
        //     public void run() {
        //
        //     }
        // });

    }
}