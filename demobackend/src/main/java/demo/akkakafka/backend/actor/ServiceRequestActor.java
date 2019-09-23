package demo.akkakafka.backend.actor;

import akka.actor.UntypedAbstractActor;
import demo.akkakafka.backend.request.ServiceRequest;
import demo.akkakafka.backend.service.DemoBackenEndService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServiceRequestActor extends UntypedAbstractActor {

  @Autowired
  private DemoBackenEndService service;

  @Override
  public void onReceive(Object message) throws Throwable {
    ServiceRequest request = (ServiceRequest) message;
    service.processRequest(request);
  }
}
