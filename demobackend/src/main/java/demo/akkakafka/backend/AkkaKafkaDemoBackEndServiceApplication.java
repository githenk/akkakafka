package demo.akkakafka.backend;

import akka.actor.ActorSystem;
import static demo.akkakafka.backend.actor.SpringExtension.SPRING_EXTENSION_PROVIDER;
import demo.akkakafka.backend.messaging.MessageProcessor;
import demo.akkakafka.backend.messaging.consumer.ReplyMessageConsumer;
import demo.akkakafka.backend.messaging.consumer.ServiceMessageConsumer;
import demo.akkakafka.backend.messaging.producer.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AkkaKafkaDemoBackEndServiceApplication {

  private ActorSystem system;

  @Autowired
  private ApplicationContext applicationContext;

  @Bean
  public ActorSystem actorSystem() {
    system = ActorSystem.create("akkaKafkaDemoBackend");
    SPRING_EXTENSION_PROVIDER.get(system)
        .initialize(applicationContext);
    return system;
  }

  public static void main(String[] args) {
      SpringApplication.run(AkkaKafkaDemoBackEndServiceApplication.class);
  }

}
