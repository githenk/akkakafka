package demo.akkakafka.backend.messaging;

import demo.akkakafka.backend.messaging.consumer.ReplyMessageConsumer;
import demo.akkakafka.backend.messaging.producer.MessageProducer;
import demo.akkakafka.backend.request.ServiceRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

@Component
public class MessageProcessor {

  @Resource
  private ReplyMessageConsumer serviceReplyMessageConsumer;

  @Resource
  private MessageProducer messageProducer;

  public ServiceRequest message(String key, ServiceRequest request, String topic) throws Exception {
    CompletableFuture<ServiceRequest> future = new CompletableFuture<>();
    serviceReplyMessageConsumer.consumeMessage(key, future);
    messageProducer.produce(key, request, topic);
    return future.get();
  }

  public void reply(String key, ServiceRequest request, String topic) {
    messageProducer.produce(key, request, topic);
  }
}
