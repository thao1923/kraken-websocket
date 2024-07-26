package com.assignment.ws.controller;

import com.assignment.ws.service.WSPublicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/v1/kraken")
public class WSController {
    private final WSPublicService wsPublicService;

    @GetMapping("/connect/public")
    public ResponseEntity<String> connect(){
        boolean res =wsPublicService.connect();
        if (res){
            return new ResponseEntity<>(HttpStatus.OK);
        }else {
            return ResponseEntity.ok("{\"msg\": \"error connecting to Kraken\"}");
        }

    }

    @GetMapping("/close/public")
    public ResponseEntity<String> close(){
        wsPublicService.close();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestParam(required = false) List<String> symbol, @RequestParam String channel){
        try{
            wsPublicService.subscribe(symbol, channel);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("{error:\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam(required = false) List<String> symbol, @RequestParam String channel){
        try{
            wsPublicService.unsubscribe(symbol, channel);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("{error:\"" + e.getMessage() + "\"}");
        }
    }




}
