package demo.akkakafka.backend.actor;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.springframework.context.ApplicationContext;

/**
 * Adding Spring Support via Akka Extension https://www.baeldung.com/akka-with-spring
 * @Author baeldung
 */
public class SpringActorProducer implements IndirectActorProducer {

  private ApplicationContext applicationContext;

  private String beanActorName;

  public SpringActorProducer(ApplicationContext applicationContext,
                             String beanActorName) {
    this.applicationContext = applicationContext;
    this.beanActorName = beanActorName;
  }

  @Override
  public Actor produce() {
    return (Actor) applicationContext.getBean(beanActorName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<? extends Actor> actorClass() {
    return (Class<? extends Actor>) applicationContext
        .getType(beanActorName);
  }
}