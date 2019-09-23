package demo.akkakafka.service;

import demo.akkakafka.messaging.consumer.MessageConsumer;
import demo.akkakafka.dto.request.DemoRequestDTO;
import demo.akkakafka.dto.response.DemoResponseDTO;
import demo.akkakafka.messaging.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class DemoWebService {

  @Resource
  private MessageConsumer messageConsumer;

  @Resource
  private MessageProducer messageProducer;

  public String processServiceRequest() throws Exception {
    DemoRequestDTO request = new DemoRequestDTO();
    request.setRequestMessage("Henri heittelee!");
    return processServiceRequest(request).getResponseMessage();
  }

  public DemoResponseDTO processServiceRequest(DemoRequestDTO request) throws Exception {
    Logger LOGGER = LoggerFactory.getLogger(DemoWebService.class);

    String key = UUID.randomUUID().toString();
    CompletableFuture<String> future = new CompletableFuture<>();
    messageConsumer.consumeMessage(key, future);
    messageProducer.produce(key, request.getRequestMessage());

    String response = future.get();
    LOGGER.info("Processed service request with response: ", response);

    DemoResponseDTO demoResponseDTO = new DemoResponseDTO();
    demoResponseDTO.setResponseMessage(response);
    return demoResponseDTO;
  }
}
