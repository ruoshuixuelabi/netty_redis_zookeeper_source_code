package com.crazymakercircle.netty.basic;

import com.crazymakercircle.im.common.bean.User;
import com.crazymakercircle.util.Logger;
import com.crazymakercircle.util.ThreadUtil;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;


public class NettyReactorTest {

    @Test
    public void testJUCThreadPool() {

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Runnable runTarget = new Runnable() {
            @Override
            public void run() {
                Logger.tcfo(" i am execute by  thread pool");
            }
        };

        for (int i = 0; i < 10; i++) {
            pool.submit(runTarget);

        }

        ThreadUtil.sleepSeconds(1000);
    }

    @Test
    public void testNettyThreadPool() {

//        ExecutorService pool = new DefaultEventLoop();
        ExecutorService pool = new NioEventLoopGroup(2);
        Runnable runTarget = new Runnable() {
            @Override
            public void run() {
                Logger.tcfo(" i am execute by  thread pool");
            }
        };
        for (int i = 0; i < 10; i++) {
            pool.submit(runTarget);

        }

        ThreadUtil.sleepSeconds(1000);
    }

    @Test
    public void testJUCscheduleAtFixedRate() {

        ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);
        Runnable runTarget = new Runnable() {
            @Override
            public void run() {
                Logger.tcfo(" i am execute by  thread pool");
            }
        };
        for (int i = 0; i < 10; i++) {
            pool.scheduleAtFixedRate(runTarget, 1, 1, TimeUnit.MINUTES);

        }

        ThreadUtil.sleepSeconds(1000);
    }

    @Test
    public void testNettyscheduleAtFixedRate() {

        ScheduledExecutorService pool = new NioEventLoopGroup(2);
        Runnable runTarget = new Runnable() {
            @Override
            public void run() {
                Logger.tcfo(" i am execute by  thread pool");
            }
        };
        for (int i = 0; i < 10; i++) {
            ((ScheduledExecutorService) pool).scheduleAtFixedRate(runTarget, 10, 10, TimeUnit.SECONDS);

        }

        ThreadUtil.sleepSeconds(1000);
    }

    @Test
    public void testUserbuilder() {
        User user = User.builder()
                .devId("??????01")
                .name("??????????????? ??????")
                .platform(17)
                .build();
        System.out.println(user);
    }


    static class IntegerWrapper {
        Integer i = 0;
        ReentrantLock lock;

        public IntegerWrapper() {
            lock = new ReentrantLock();
        }

        public Integer getValue() {
            return i;
        }


    }

    static class Task implements Runnable {
        IntegerWrapper integerWrapper = null;
        CountDownLatch latch;

        public Task(IntegerWrapper i, CountDownLatch latch) {
            this.integerWrapper = i;
            this.latch = latch;

        }

        @Override
        public void run() {
            integerWrapper.lock.lock();
            try {
                integerWrapper.i++;
            } finally {
                integerWrapper.lock.unlock();
            }

            latch.countDown();
        }

        @Override
        public String toString() {
            return integerWrapper.toString();
        }
    }


    @Test
    public void testThreadPoolTaskSchedule() {

        IntegerWrapper[] integerWrappers = new IntegerWrapper[100];

        //500w??????8????????? ???juc ????????????????????? 19567ms
        //500w??????8????????? ???juc ????????????????????? 19s
        //1000w??????16????????? ???juc ????????????????????? 33101ms
        //1000w??????16????????? ???juc ????????????????????? 33s

//        Integer countPerInt = 50000;
        Integer countPerInt = 100000;

        CountDownLatch latch = new CountDownLatch(countPerInt * 100);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            integerWrappers[i] = new IntegerWrapper();
        }

//        ThreadPoolExecutor pool = ThreadUtil.getCpuIntenseTargetThreadPool();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        for (int j = 0; j < countPerInt; j++) {
            for (int i = 0; i < 100; i++) {
                IntegerWrapper integerWrapper = integerWrappers[i];
                pool.submit(new Task(integerWrapper, latch));

            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("????????????????????? " + (endTime - startTime) + "ms");
        System.out.println("????????????????????? " + (endTime - startTime) / 1000 + "s");

        for (int i = 0; i < 100; i++) {
            IntegerWrapper integerWrapper = integerWrappers[i];
            if (integerWrapper.i < countPerInt) {
                System.err.printf(i + "  error:" + integerWrapper.getValue());
            }
        }
    }


    //?????????netty ?????????

    static class IntegerWrapperNoneLock {
        Integer i = 0;
        EventExecutor eventloop;


        public Integer getValue() {
            return i;
        }

        public void register(EventExecutor eventloop) {
            this.eventloop=eventloop;

        }
    }

    static class TaskNoneLock implements Runnable {
        IntegerWrapperNoneLock integerWrapper = null;
        CountDownLatch latch;

        public TaskNoneLock(IntegerWrapperNoneLock i, CountDownLatch latch) {
            this.integerWrapper = i;
            this.latch = latch;
        }

        @Override
        public void run() {
            integerWrapper.i++;
            latch.countDown();
        }

        @Override
        public String toString() {
            return integerWrapper.toString();
        }
    }

    @Test
    public void testThreadPoolOfNetty() {

        IntegerWrapperNoneLock[] integerWrappers = new IntegerWrapperNoneLock[100];

        // 500??????8????????? ???juc ????????????????????? 19567ms
        // 500??????8????????? ???juc ????????????????????? 19s
        // 500??????8????????? ???netty ????????????????????? 16587ms
        // 500??????8????????? ???netty ????????????????????? 16s

        //1000w??????16????????? ???juc ????????????????????? 33101ms
        //1000w??????16????????? ???juc ????????????????????? 33s
        //
        // 1000w??????16????????? ???netty ?????????????????????  29848ms
        //1000w??????16????????? ???netty ????????????????????? 29s
//        Integer countPerInt = 50000;
        Integer countPerInt = 100000;

        CountDownLatch latch = new CountDownLatch(countPerInt * 100);

        long startTime = System.currentTimeMillis();

        //??????netty  ?????????
        DefaultEventLoopGroup pool = new DefaultEventLoopGroup(16);
//        ThreadPoolExecutor pool = ThreadUtil.getCpuIntenseTargetThreadPool();

        for (int i = 0; i < 100; i++) {
            integerWrappers[i] = new IntegerWrapperNoneLock();

            EventExecutor eventloop = pool.next();

            integerWrappers[i].register(eventloop);

        }


        for (int j = 0; j < countPerInt; j++) {

            for (int i = 0; i < 100; i++) {

                IntegerWrapperNoneLock integerWrapper = integerWrappers[i];

                EventExecutor eventloop = integerWrapper.eventloop;

                eventloop.submit(new TaskNoneLock(integerWrapper, latch));

            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("????????????????????? " + (endTime - startTime) + "ms");
        System.out.println("????????????????????? " + (endTime - startTime) / 1000 + "s");

        for (int i = 0; i < 100; i++) {
            IntegerWrapperNoneLock integerWrapper = integerWrappers[i];
            if (integerWrapper.i < countPerInt) {
                System.err.printf(i + "  error:" + integerWrapper.getValue());
            }
        }
    }


}