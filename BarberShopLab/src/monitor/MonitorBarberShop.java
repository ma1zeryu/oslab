package monitor;

import common.Log;
import common.Shop;

public class MonitorBarberShop implements Shop {
    private final int chairs;
    private int waiting = 0;
    private int served = 0;
    private int dropped = 0;
    private boolean closing = false;

    public MonitorBarberShop(int chairs) {
        this.chairs = chairs;
    }

    @Override
    public synchronized boolean customerArrive(int customerId) {
        Log.info("顾客" + customerId + " 到达");

        if (waiting >= chairs) {
            dropped++;
            Log.info("顾客" + customerId + " 发现满座，离开");
            return false;
        }

        waiting++;
        Log.info("顾客" + customerId + " 入座等待，当前waiting=" + waiting);
        notifyAll(); // 唤醒理发师
        return true;
    }

    @Override
    public synchronized boolean barberWork() throws InterruptedException {
        while (waiting == 0 && !closing) { // 防止伪唤醒，醒来不代表条件成立
            Log.info("理发师睡眠中...");
            wait();
        }

        if (waiting == 0 && closing) {
            return false;
        }

        waiting--;
        served++;
        Log.info("理发师开始理发，当前waiting=" + waiting + ", served=" + served);

        // 注意：真正的理发耗时不要放在 synchronized 里
        return true;
    }

    @Override
    public synchronized void closeShop() {
        closing = true;
        notifyAll();
    }

    @Override
    public synchronized int getWaitingCount() {
        return waiting;
    }

    @Override
    public synchronized int getServedCount() {
        return served;
    }

    @Override
    public synchronized int getDroppedCount() {
        return dropped;
    }
}