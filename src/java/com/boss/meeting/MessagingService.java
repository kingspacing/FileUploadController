package com.boss.meeting;

/**
 * Created by Administrator on 2014/12/11.
 */
public interface MessagingService {
    public void start();
    public void stop();
    public void send(String channel, String message);
    public void addListener(MessageListener listener);
    public void removeListener(MessageListener listener);
}
