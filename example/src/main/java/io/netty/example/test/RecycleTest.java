package io.netty.example.test;

import io.netty.util.Recycler;

public class RecycleTest {
    private static final Recycler<UserA> RECYCLER = new Recycler<UserA>() {
        @Override
        protected UserA newObject(Handle<UserA> handle) {
            return new UserA(handle);
        }
    };
    private static class UserA {
        // handle 负责对象回收
        private final Recycler.Handle<UserA> handle;

        public UserA(Recycler.Handle<UserA> handle) {
            this.handle = handle;
        }

        public void recycle() {
            handle.recycle(this);
        }
    }

    public static void main(String[] args) {
        UserA userA = RECYCLER.get();
        userA.recycle();

        UserA userA1 = RECYCLER.get();
        System.out.println(userA1 == userA);// true
    }
}
