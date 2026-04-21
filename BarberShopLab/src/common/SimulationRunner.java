package common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SimulationRunner {
    public static void run(String title, Shop shop, Config config) throws InterruptedException {
        Log.info("========== 开始实验: " + title + " ==========");

        ExecutorService pool = Executors.newFixedThreadPool(8);// 创建固定线程池
        CountDownLatch customerDone = new CountDownLatch(config.totalCustomers);

        // 理发师线程
        Future<?> barberFuture = pool.submit(() -> {
            try {
                while (shop.barberWork()) {
                    Thread.sleep(config.haircutMs); // 模拟理发耗时
                }
                Log.info("理发师下班");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.info("理发师线程被中断");
            }
        });

        // 顾客线程：按时间陆续到达
        List<Future<?>> customerFutures = new ArrayList<>();
        for (int i = 1; i <= config.totalCustomers; i++) {
            final int customerId = i;
            Thread.sleep(config.customerIntervalMs); // 控制到达节奏

            Future<?> f = pool.submit(() -> {
                try {
                    shop.customerArrive(customerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.info("顾客" + customerId + " 被中断");
                } finally {
                    customerDone.countDown();
                }
            });
            customerFutures.add(f);
        }

        // 等所有顾客到达处理结束
        customerDone.await();

        // 关店
        shop.closeShop();

        try {
            barberFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        pool.shutdown();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            pool.shutdownNow();
        }

        Log.info("最终统计 => served=" + shop.getServedCount()
                + ", dropped=" + shop.getDroppedCount()
                + ", waiting=" + shop.getWaitingCount());
        Log.info("计数校验 => served + dropped = "
                + (shop.getServedCount() + shop.getDroppedCount()));
        Log.info("========== 实验结束: " + title + " ==========\n");
    }
}