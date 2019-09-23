package demo.akkakafka.backend.service;

import demo.akkakafka.backend.messaging.MessageProcessor;
import demo.akkakafka.backend.request.ServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoBackenEndService {
  private static final Logger logger = LoggerFactory.getLogger(DemoBackenEndService.class);

  @Autowired
  private MessageProcessor messageProcessor;

  @Value("${app.randomrequestnumber}")
  private boolean randomRequestNumber = true;

  @Value("${app.needreply}")
  private boolean needReply = true;

  public void processRequest(ServiceRequest request) throws Exception {
    // Business logic starts here

    // Pseudo service request number
    if(randomRequestNumber) {
      request.setRequest(request.getRequest() + " : asianumerolla " + (Math.random() * 2000 + 1) + ".");
    }
    // Example of sequential messaging to other services
    messageProcessor.message(request.getKey(), request, "aathosreply");

    // Business logic ends here

    // Reply back to requester
    if(needReply){
      messageProcessor.reply(request.getKey(), request, request.getReplyTopic());
    }


    logger.info("Aathos käsitteli asian {} .", request.getRequest());
  }
}
