package ai.youthmind.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostCreatedMessageListener implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("channel: {}", new String(pattern));
        log.info("message: {}", new String(message.getBody()));
    }
}
