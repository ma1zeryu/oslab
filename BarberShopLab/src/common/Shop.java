package common;

public interface Shop {
    // 一个顾客到店，返回true代表成功进入系统，false代表丢弃
    boolean customerArrive(int customerId) throws InterruptedException;

    // 理发师尝试服务以为顾客，返回false代表结束了
    boolean barberWork() throws InterruptedException;

    // 关店，唤醒阻塞进程，准备退出
    void closeShop();

    int getWaitingCount();

    int getServedCount();

    int getDroppedCount();
}