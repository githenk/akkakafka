package demo.akkakafka.backend.messaging.consumer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Committer;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.typesafe.config.Config;
import demo.akkakafka.backend.request.ServiceRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReplyMessageConsumer {
  private static final Logger logger = LoggerFactory.getLogger(ReplyMessageConsumer.class);

  @Value("${kafka.bootstrap-server}")
  private String bootstrapServer = "localhost:9092";

  private final ActorSystem system;
  private final ConsumerSettings<String, byte[]> consumerSettings;
  private final CommitterSettings committerSettings;
  private ConcurrentHashMap<String, CompletableFuture<ServiceRequest>> messageMap;
  private Consumer.DrainingControl<Done> control;

  private final ObjectMapper mapper = new ObjectMapper();
  private final ObjectReader serviceRequestReader = mapper.readerFor(ServiceRequest.class);
  private Materializer materializer;


  public ReplyMessageConsumer(ActorSystem actorSystem) {
    this.system = actorSystem;
    Config config = system.settings().config().getConfig("akka.kafka.consumer");
    this.consumerSettings = ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
        .withBootstrapServers(bootstrapServer)
        .withGroupId("aathosreply")
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    Config committerConfig = system.settings().config().getConfig("akka.kafka.committer");
    this.committerSettings = CommitterSettings.apply(committerConfig);
    this.messageMap = new ConcurrentHashMap<>();
    this.materializer = Materializer.apply(system);
    init();
  }

  public void consumeMessage(String key, CompletableFuture<ServiceRequest> completableFutureResponseMessage) {
    if(control.isShutdown().toCompletableFuture().isDone()) {
      init();
    }
    messageMap.put(key, completableFutureResponseMessage);
  }

  private void init() {
    control =
        Consumer.committableSource(consumerSettings, Subscriptions.topics("aathosreply"))
            .mapAsync(
                1,
                msg ->
                  processMessage(msg.record().key(), msg.record().value())
                      .thenApply(done -> msg.committableOffset()))
            .toMat(Committer.sink(committerSettings.withMaxBatch(1)), Keep.both())
            .mapMaterializedValue(Consumer::createDrainingControl)
            .run(materializer);
  }

  private CompletionStage<Done> processMessage(String key, byte[] value) {
    CompletableFuture<Done> done = new CompletableFuture<>();
    ServiceRequest incomingRequest = null;
    if(messageMap.containsKey(key)){
      try {
        incomingRequest = serviceRequestReader.readValue(new String(value));
      } catch (IOException ex) {
        logger.error("Failed to map service request from incoming message: {}", new String(value));
      }
      messageMap.get(key).complete(incomingRequest);
      messageMap.remove(key);
    }
    done.complete(Done.done());
    return done;
  }

  @PreDestroy
  public void destroy() {
    control.shutdown();
  }
}