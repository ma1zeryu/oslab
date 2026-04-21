package reentrantlock;

import common.Log;
import common.Shop;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LockBarberShop implements Shop {
    private final int chairs;
    private int waiting = 0;
    private int served = 0;
    private int dropped = 0;
    private boolean closing = false;
    private long nextTicket = 1;
    private long servingTicket = 1;

    // ReentrantLock + Condition和管程做差不多的事，但更手动
    // 分成多种Condition，有更强的语义，控制更准确
    private final ReentrantLock lock = new ReentrantLock(); // 显式版的synchronized
    private final Condition customerArrived = lock.newCondition(); // Condition：显式版的wait/notify/notifyAll
    private final Condition barberReady = lock.newCondition(); // 锁上的等待队列

    public LockBarberShop(int chairs) {
        this.chairs = chairs;
    }

    @Override
    public boolean customerArrive(int customerId) throws InterruptedException {
        lock.lock();
        try {
            Log.info("顾客" + customerId + " 到达");

            if (waiting >= chairs) {
                dropped++;
                Log.info("顾客" + customerId + " 发现满座，离开");
                return false;
            }

            waiting++;
            Log.info("顾客" + customerId + " 入座等待，当前waiting=" + waiting);
            long myTicket = nextTicket++;

            customerArrived.signal(); // 通知理发师

            while (myTicket != servingTicket && !closing) {
                barberReady.await();
            }

            if (closing && myTicket != servingTicket) {
                return false;
            }
            Log.info("顾客" + customerId + " 开始理发");
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean barberWork() throws InterruptedException {
        lock.lock();
        try {
            while (waiting == 0 && !closing) {
                Log.info("理发师睡眠中...");
                customerArrived.await();
            }

            if (waiting == 0 && closing) {
                return false;
            }

            waiting--;
            served++;
            Log.info("理发师开始理发，当前waiting=" + waiting + ", served=" + served);
            servingTicket++;

            barberReady.signalAll(); // 唤醒顾客重检是否轮到自己的号码
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeShop() {
        lock.lock();
        try {
            closing = true;
            customerArrived.signalAll();
            barberReady.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getWaitingCount() {
        lock.lock();
        try {
            return waiting;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getServedCount() {
        lock.lock();
        try {
            return served;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getDroppedCount() {
        lock.lock();
        try {
            return dropped;
        } finally {
            lock.unlock();
        }
    }
}
