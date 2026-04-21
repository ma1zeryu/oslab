package common;

public class Config {
    public final int chairs;
    public final int totalCustomers;
    public final int customerIntervalMs;
    public final int haircutMs;

    public Config(int chairs, int totalCustomers, int customerIntervalMs, int haircutMs) {
        this.chairs = chairs;
        this.totalCustomers = totalCustomers;
        this.customerIntervalMs = customerIntervalMs;
        this.haircutMs = haircutMs;
    }
}