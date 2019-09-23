package demo.akkakafka.backend.service;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Committer;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.typesafe.config.Config;
import demo.akkakafka.backend.request.ServiceRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class TestMessaging {

  private String bootstrapServer = "localhost:9092";

  private final ActorSystem system;
  private final ConsumerSettings<String, byte[]> consumerSettings;
  private final CommitterSettings committerSettings;
  private final ProducerSettings<String, String> producerSettings;
  private ConcurrentHashMap<String, CompletableFuture<ServiceRequest>> messageMap;
  private Consumer.DrainingControl<Done> control;
  private Materializer materializer;
  private final ObjectMapper mapper = new ObjectMapper();
  private final ObjectReader serviceRequestReader = mapper.readerFor(ServiceRequest.class);
  private final ObjectWriter serviceRequestWriter = mapper.writerFor(ServiceRequest.class);

  public TestMessaging(ActorSystem actorSystem) {
    this.system = actorSystem;
    Config config = system.settings().config().getConfig("akka.kafka.consumer");
    this.consumerSettings = ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
        .withBootstrapServers(bootstrapServer)
        .withGroupId("aathosTestReply")
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    Config committerConfig = system.settings().config().getConfig("akka.kafka.committer");
    this.committerSettings = CommitterSettings.apply(committerConfig);
    Config producerConfig = system.settings().config().getConfig("akka.kafka.producer");
    this.producerSettings =
        ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
            .withBootstrapServers(bootstrapServer);
    this.messageMap = new ConcurrentHashMap<>();
    this.materializer = Materializer.apply(system);
    init();
  }

  private void init() {
    control =
        Consumer.committableSource(consumerSettings, Subscriptions.topics("aathostestreply"))
            .mapAsync(
                1,
                msg ->
                    processMessage(msg.record().key(), msg.record().value())
                        .thenApply(done -> msg.committableOffset()))
            .toMat(Committer.sink(committerSettings.withMaxBatch(1)), Keep.both())
            .mapMaterializedValue(Consumer::createDrainingControl)
            .run(materializer);
  }

  void consumeMessage(String key, CompletableFuture<ServiceRequest> completableFutureResponseMessage) {
    if(control.isShutdown().toCompletableFuture().isDone()) {
      init();
    }
    messageMap.put(key, completableFutureResponseMessage);
  }

  private CompletionStage<Done> processMessage(String key, byte[] value) throws Exception {
    CompletableFuture<Done> done = new CompletableFuture<>();
    ServiceRequest incomingRequest = null;
    if(messageMap.containsKey(key)){

      incomingRequest = serviceRequestReader.readValue(new String(value));
      messageMap.get(key).complete(incomingRequest);
      messageMap.remove(key);
    }
    done.complete(Done.done());
    return done;
  }


  void produce(String messageKey, ServiceRequest request, String topic) throws Exception {
    final String requestString = serviceRequestWriter.writeValueAsString(request);
    CompletionStage<Done> done = Source.range(1, 1)
        .map(key -> messageKey)
        .map(value -> new ProducerRecord<>(topic, messageKey, requestString))
        .runWith(Producer.plainSink(producerSettings), materializer);
    done.thenApply(d -> done.toCompletableFuture().complete(Done.done()));
    //done.toCompletableFuture().complete(Done.done());
  }
}
