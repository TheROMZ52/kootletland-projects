package ir.kootletland.ac.integration;

public interface Integration {
    String name();
    boolean available();
    void start();
    void stop();
}
