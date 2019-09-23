package demo.akkakafka.backend.messaging.producer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.typesafe.config.Config;
import demo.akkakafka.backend.request.ServiceRequest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

@Component
public class MessageProducer {
  private final static Logger LOGGER = LoggerFactory.getLogger(MessageProducer.class);

  @Value("${kafka.bootstrap-server}")
  private String bootstrapServer = "localhost:9092";

  private final ActorSystem system;
  private final ProducerSettings<String, String> producerSettings;
  private Materializer materializer;

  private final ObjectMapper mapper = new ObjectMapper();
  private final ObjectWriter serviceRequestWriter = mapper.writerFor(ServiceRequest.class);

  public MessageProducer(ActorSystem actorSystem) {
    this.system = actorSystem;
    Config config = system.settings().config().getConfig("akka.kafka.producer");
    this.producerSettings =
        ProducerSettings.create(config, new StringSerializer(), new StringSerializer())
            .withBootstrapServers(bootstrapServer);
    this.materializer = Materializer.apply(system);
  }

  public void produce(String messageKey, ServiceRequest request, String topic) {
    try {
      final String requestString = serviceRequestWriter.writeValueAsString(request);
      CompletionStage<Done> done = Source.range(1, 1)
              .map(key -> messageKey)
              .map(value -> new ProducerRecord<>(topic, messageKey, requestString))
              .runWith(Producer.plainSink(producerSettings), materializer);
      done.thenApply(d -> done.toCompletableFuture().complete(Done.done()));
    } catch (IOException ex) {
      LOGGER.error("Failed to map service request from incoming message: ", request);
    }
  }
}
