package demo.akkakafka.backend.service;

import demo.akkakafka.backend.request.ServiceRequest;
import demo.akkakafka.backend.AkkaKafkaDemoBackEndServiceApplication;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import akka.kafka.testkit.javadsl.EmbeddedKafkaJunit4Test;
import akka.stream.Materializer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = AkkaKafkaDemoBackEndServiceApplication.class)
@TestInstance(Lifecycle.PER_CLASS)
public class MessageRequestResponseTest extends EmbeddedKafkaJunit4Test {

  private static ActorSystem system = ActorSystem.create();

  private static Materializer materializer = Materializer.apply(system);


  public MessageRequestResponseTest(){
        super(system, materializer, 9092);
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testRequestResponse() throws Exception {

    ServiceRequest request = new ServiceRequest(UUID.randomUUID().toString(),"Henri heittelee!","aathostestreply");
    TestMessaging testMessaging = new TestMessaging(system);
    CompletableFuture<ServiceRequest> future = new CompletableFuture<>();
    testMessaging.consumeMessage(request.getKey(), future);
    testMessaging.produce(request.getKey(), request, "aathos");

    ServiceRequest done = future.get();

    Assert.assertTrue(done.getRequest().contains("Henri heittelee!"));

  }

}
