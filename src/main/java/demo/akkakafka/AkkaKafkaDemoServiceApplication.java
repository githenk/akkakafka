package demo.akkakafka;

import akka.actor.ActorSystem;
import demo.akkakafka.messaging.consumer.MessageConsumer;
import demo.akkakafka.messaging.producer.MessageProducer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AkkaKafkaDemoServiceApplication {

  private ActorSystem system;

  {
    this.system = ActorSystem.apply();
  }

  @Bean
  public ActorSystem actorSystem() {
    return this.system;
  }

  @Bean
  public MessageConsumer getMessageConsumer() {
    return new MessageConsumer(system);
  }

  @Bean
  public MessageProducer getMessageProducer() {
    return new MessageProducer(system);
  }

  public static void main(String[] args) {
    SpringApplication.run(AkkaKafkaDemoServiceApplication.class);
  }

}
