package demo.akkakafka.service.controller;

import demo.akkakafka.dto.request.DemoRequestDTO;
import demo.akkakafka.dto.response.DemoResponseDTO;
import demo.akkakafka.service.DemoWebService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class DemoWebServiceController {

  @Autowired
  transient DemoWebService demoWebService;

  @GetMapping("/")
  public ResponseEntity<String> getService() throws Exception{
    return ResponseEntity.ok().body(demoWebService.processServiceRequest());
  }

  @ApiOperation(
      value = "The endpoint returns all pension groups which belong to the given insurance",
      response = DemoResponseDTO.class)
  @PostMapping
  public ResponseEntity<DemoResponseDTO> postServiceRequest(@RequestBody DemoRequestDTO request) throws Exception {
    return ResponseEntity.of(Optional.ofNullable(demoWebService.processServiceRequest(request)));
  }

}
