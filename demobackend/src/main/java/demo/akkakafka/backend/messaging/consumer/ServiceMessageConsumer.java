package demo.akkakafka.backend.messaging.consumer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Committer;
import akka.kafka.javadsl.Consumer;
import akka.pattern.Patterns;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.util.Timeout;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import static demo.akkakafka.backend.actor.SpringExtension.SPRING_EXTENSION_PROVIDER;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@Scope("singleton")
public class ServiceMessageConsumer {

  private final static Logger LOGGER = LoggerFactory.getLogger(ServiceMessageConsumer.class);

  @Value("${kafka.bootstrap-server}")
  private String bootstrapServer = "localhost:9092";

  @Value("${kafka.service-topic}")
  private String consumerTopic = "aathos";

  @Value("${app.groupid}")
  private String groupid = "aathos";

  private final ActorSystem system;
  private final ConsumerSettings<String, byte[]> consumerSettings;
  private final CommitterSettings committerSettings;
  private Consumer.DrainingControl<Done> control;
  private Materializer materializer;
  private Timeout timeout;

  private final ObjectMapper mapper = new ObjectMapper();
  private final ObjectReader serviceRequestReader = mapper.readerFor(ServiceRequest.class);

  public ServiceMessageConsumer(ActorSystem actorSystem) {
    this.system = actorSystem;
    Config config = system.settings().config().getConfig("akka.kafka.consumer");
    this.consumerSettings = ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
        .withBootstrapServers(bootstrapServer)
        .withGroupId(groupid)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    Config committerConfig = system.settings().config().getConfig("akka.kafka.committer");
    this.committerSettings = CommitterSettings.apply(committerConfig);
    this.materializer = Materializer.apply(system);
    this.timeout = Timeout.create(Duration.ofSeconds(3));
    init();
  }

  private void init() {
    control =
        Consumer.committableSource(consumerSettings, Subscriptions.topics(consumerTopic))
            .mapAsync(
                1,
                msg ->
                  processMessage(msg.record().value())
                      .thenApply(done -> msg.committableOffset()))
            .toMat(Committer.sink(committerSettings.withMaxBatch(1)), Keep.both())
            .mapMaterializedValue(Consumer::createDrainingControl)
            .run(materializer);
  }

  private CompletionStage<Done> processMessage(byte[] value) {
    CompletableFuture<Done> done = new CompletableFuture<>();
    try {
      ServiceRequest incomingRequest = serviceRequestReader.readValue(new String(value));
      Patterns.ask(system.actorOf(SPRING_EXTENSION_PROVIDER.get(system).props("serviceRequestActor")), incomingRequest, timeout);
    } catch (IOException ex) {
      LOGGER.error("Failed to map service request from incoming message: {}", new String(value));
    }
    done.complete(Done.done());
    return done;
  }

  @PreDestroy
  public void destroy() {
    control.shutdown();
  }
}