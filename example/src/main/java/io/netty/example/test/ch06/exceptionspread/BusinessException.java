package io.netty.example.test.ch06.exceptionspread;

public class BusinessException extends Exception {

    public BusinessException(String message) {
        super(message);
    }
}
