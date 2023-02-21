package io.netty.example.test;

import io.netty.buffer.PooledByteBufAllocator;

public class TestAllocatorBuffer {
    public static void main(String[] args) {
        int page = 1024 * 8;
        PooledByteBufAllocator allocator = new PooledByteBufAllocator();
        allocator.directBuffer(2 * page);
    }
}
