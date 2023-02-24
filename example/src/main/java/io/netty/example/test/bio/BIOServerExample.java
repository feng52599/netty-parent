package io.netty.example.test.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @program: NioProject
 * @description:
 * @author: feng
 * @create: 2023-02-20 21:14
 * @version: 1.0
 */
public class BIOServerExample {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8000);
        try {
            Socket socketInstance = serverSocket.accept();
            InputStream inputStream = socketInstance.getInputStream();
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(bytes)) != -1) {
                System.out.println(new String(bytes, 0, len));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}