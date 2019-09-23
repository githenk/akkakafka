package demo.akkakafka.backend.request;

import lombok.Data;

@Data
public class ServiceRequest {

  private String replyTopic;

  private String key;

  private String request;

  public ServiceRequest(){}

  public ServiceRequest(String key, String request, String replyTopic){
    this.replyTopic = replyTopic;
    this.key = key;
    this.request = request;
  }

}
