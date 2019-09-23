package demo.akkakafka.messaging.consumer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Committer;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import com.typesafe.config.Config;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Scope("singleton")
public class MessageConsumer {
  private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);

  @Value("${kafka.bootstrap-server}")
  private String bootstrapServer = "localhost:9092";

  @Value("${app.groupid}")
  private String groupid = "demo";

  private final ActorSystem system;
  private final ConsumerSettings<String, byte[]> consumerSettings;
  private final CommitterSettings committerSettings;
  private ConcurrentHashMap<String, CompletableFuture<String>> messageMap;
  private Consumer.DrainingControl<Done> control;


  public MessageConsumer(ActorSystem actorSystem) {
    this.system = actorSystem;
    Config config = system.settings().config().getConfig("akka.kafka.consumer");
    this.consumerSettings = ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
        .withBootstrapServers(bootstrapServer)
        .withGroupId(groupid)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    Config committerConfig = system.settings().config().getConfig("akka.kafka.committer");
    this.committerSettings = CommitterSettings.apply(committerConfig);
    this.messageMap = new ConcurrentHashMap<>();
    init();
  }

  public void consumeMessage(String key, CompletableFuture<String> completableFutureResponseMessage) {
    if(control.isShutdown().toCompletableFuture().isDone()) {
      init();
    }
    messageMap.put(key, completableFutureResponseMessage);
  }


  private void init() {
    Materializer materializer = Materializer.apply(system);

    control =
        Consumer.committableSource(consumerSettings, Subscriptions.topics("aathos"))
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
    if(messageMap.containsKey(key)){
      String message = new String(value);
      messageMap.get(key).complete(message);
      messageMap.remove(key);
    }
    done.complete(Done.done());
    return done;
  }
}