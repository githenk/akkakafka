package demo.akkakafka.producer;

import akka.kafka.testkit.javadsl.EmbeddedKafkaJunit4Test;
import akka.stream.Materializer;
import demo.akkakafka.messaging.producer.MessageProducer;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.UUID;

public class MessageProducerTest extends EmbeddedKafkaJunit4Test {
  private static ActorSystem system = ActorSystem.create();

  private static Materializer materializer = Materializer.apply(system);

  public MessageProducerTest(){
    super(system, materializer, 9092);
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testMessageProducer() {
    String key = UUID.randomUUID().toString();
    MessageProducer producer = new MessageProducer(system);
    producer.produce(key, "Henri heittelee!");
  }

}
