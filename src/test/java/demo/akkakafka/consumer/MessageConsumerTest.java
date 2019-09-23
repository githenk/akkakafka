package demo.akkakafka.consumer;

import akka.kafka.testkit.javadsl.EmbeddedKafkaJunit4Test;
import akka.stream.Materializer;
import demo.akkakafka.messaging.consumer.MessageConsumer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import demo.akkakafka.messaging.producer.MessageProducer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@TestInstance(Lifecycle.PER_CLASS)
public class MessageConsumerTest extends EmbeddedKafkaJunit4Test {

  private static ActorSystem system = ActorSystem.create();

  private static Materializer materializer = Materializer.apply(system);


  public MessageConsumerTest(){
      super(system, materializer, 9092);
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testMessageConsumer() throws Exception {

    String message = "Henri heittelee.";

    String key = UUID.randomUUID().toString();
    MessageConsumer consumer = new MessageConsumer(system);
    CompletableFuture<String> future = new CompletableFuture<>();
    consumer.consumeMessage(key, future);
    MessageProducer producer = new MessageProducer(system);
    producer.produce(key, message);

    String done = future.get();

    Assert.assertEquals(message, done);

  }



}
