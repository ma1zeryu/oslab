package semaphore;

import common.Log;
import common.Shop;

import java.util.concurrent.Semaphore;

public class SemaphoreBarberShop implements Shop {
    private final int chairs;
    private int waiting = 0;
    private int served = 0;
    private int dropped = 0;
    private boolean closing = false;

    private final Semaphore mutex = new Semaphore(1); // 修改一些共享变量的时候需要锁上，waiting/served/dropped
    private final Semaphore customers = new Semaphore(0);
    private final Semaphore barbers = new Semaphore(0);

    public SemaphoreBarberShop(int chairs) {
        this.chairs = chairs;
    }

    @Override
    public boolean customerArrive(int customerId) throws InterruptedException {
        Log.info("顾客" + customerId + " 到达");

        mutex.acquire();
        if (waiting >= chairs) {
            dropped++;
            Log.info("顾客" + customerId + " 发现满座，离开");
            mutex.release();
            return false;
        }

        waiting++;
        Log.info("顾客" + customerId + " 入座等待，当前waiting=" + waiting);
        mutex.release();

        customers.release(); // 通知有顾客来了
        barbers.acquire(); // 等待理发师叫号

        Log.info("顾客" + customerId + " 开始理发");
        return true;
    }

    @Override
    public boolean barberWork() throws InterruptedException {
        // 如果已经关店且没有顾客，就退出
        if (closing && customers.availablePermits() == 0) {
            return false;
        }

        customers.acquire(); // 没顾客时阻塞，相当于睡眠

        mutex.acquire();
        if (waiting > 0) {
            waiting--;
            served++;
            Log.info("理发师取到一位顾客，当前waiting=" + waiting + ", served=" + served);
        }
        mutex.release();

        barbers.release(); // 通知一位顾客可以开始理发
        return true;
    }

    @Override
    public void closeShop() {
        closing = true;
        // 释放一个 customers，防止理发师永远阻塞
        customers.release();
    }

    @Override
    public int getWaitingCount() throws RuntimeException {
        try {
            mutex.acquire();
            int val = waiting;
            mutex.release();
            return val;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getServedCount() {
        return served;
    }

    @Override
    public int getDroppedCount() {
        return dropped;
    }
}