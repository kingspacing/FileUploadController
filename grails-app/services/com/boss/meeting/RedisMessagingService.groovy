package com.boss.meeting

import grails.transaction.Transactional
import org.apache.log4j.Logger
import org.springframework.beans.factory.InitializingBean

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * Created by Administrator on 2014/12/11.
 */
@Transactional
class RedisMessagingService implements MessagingService, InitializingBean {
    private static  Logger logger = Logger.getLogger(RedisMessagingService.class.name);
    private final Set<MessageListener> listeners = new HashSet<MessageListener>();
    private final Executor exec = Executors.newSingleThreadExecutor();
    private Runnable pubsubListener;

    def redisService

    @Override
    void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    void start() {
        logger.debug("Starting redis pubsub...");
        try {
            pubsubListener = new Runnable() {
                @Override
                public void run() {
                    redisService.withRedis { Jedis redis ->
                        redis.psubscribe(new PubSubListener(), "bm:mgr:*");
                    }
                }
            };
            exec.execute(pubsubListener);
        } catch (Exception e) {
            logger.error("Error in subscribe: " + e.getMessage());
        }
    }

    @Override
    void stop() {

    }

    @Override
    void send(String channel, String message) {

    }

    @Override
    void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    @Override
    void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    private class PubSubListener extends JedisPubSub {

        public PubSubListener() {
            super();
        }

        @Override
        public void onMessage(String channel, String message) {
            // Not used.
        }

        @Override
        public void onPMessage(String pattern, String channel, String message) {
            logger.debug("Message Received in pattern: " + pattern);
            logger.debug("Message Received in channel: " + channel);
            logger.debug("Message: " + message);
        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
            logger.debug("Subscribed to the pattern:" + pattern);
        }

        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {
            // Not used.
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            // Not used.
        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            // Not used.
        }
    }
}
