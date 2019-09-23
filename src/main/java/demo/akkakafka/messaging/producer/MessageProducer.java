package demo.akkakafka.messaging.producer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import demo.akkakafka.messaging.consumer.MessageConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MessageProducer {
  private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);

  @Value("${kafka.bootstrap-server}")
  private String bootstrapServer = "localhost:9092";

  private final ActorSystem system;
  private final ProducerSettings<String, String> producerSettings;
  private Materializer materializer;

  public MessageProducer(ActorSystem actorSystem) {
    this.system = actorSystem;
    Config config = system.settings().config().getConfig("akka.kafka.producer");
    this.producerSettings =
        ProducerSettings.create(config, new StringSerializer(), new StringSerializer())
            .withBootstrapServers(bootstrapServer);
    this.materializer = Materializer.apply(system);
  }

  public CompletableFuture<Done> produce(String messageKey, String message) {
    logger.debug("Akka Kafka Demo application is producing a message with key: {}", messageKey);
    CompletionStage<Done> done =
        Source.range(1, 1)
            .map(key -> messageKey)
            .map(value -> new ProducerRecord<>("aathos", messageKey, message))
            .runWith(Producer.plainSink(producerSettings), materializer);
    done.toCompletableFuture().complete(Done.done());
    return done.toCompletableFuture();
  }
}
