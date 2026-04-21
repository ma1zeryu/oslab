import common.Config;
import common.SimulationRunner;
import monitor.MonitorBarberShop;
import semaphore.SemaphoreBarberShop;
import reentrantlock.LockBarberShop;

public class BarberShopLab {
    public static void main(String[] args) throws InterruptedException {
        Config config = new Config(
                5, // chairs
                100, // totalCustomers
                80, // customerIntervalMs
                200 // haircutMs
        );

        SimulationRunner.run("Monitor", new MonitorBarberShop(config.chairs), config);
        SimulationRunner.run("Semaphore", new SemaphoreBarberShop(config.chairs), config);
        SimulationRunner.run("ReentrantLock", new LockBarberShop(config.chairs), config);
    }
}