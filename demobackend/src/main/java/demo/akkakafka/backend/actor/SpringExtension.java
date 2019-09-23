package demo.akkakafka.backend.actor;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.Props;
import org.springframework.context.ApplicationContext;

/**
 * Adding Spring Support via Akka Extension https://www.baeldung.com/akka-with-spring
 * @Author baeldung
 */
public class SpringExtension
    extends AbstractExtensionId<SpringExtension.SpringExt> {

  public static final SpringExtension SPRING_EXTENSION_PROVIDER
      = new SpringExtension();

  @Override
  public SpringExt createExtension(ExtendedActorSystem system) {
    return new SpringExt();
  }

  public static class SpringExt implements Extension {
    private volatile ApplicationContext applicationContext;

    public void initialize(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    public Props props(String actorBeanName) {
      return Props.create(
          SpringActorProducer.class, applicationContext, actorBeanName);
    }
  }
}