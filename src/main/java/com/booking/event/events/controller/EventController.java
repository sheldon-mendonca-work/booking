package com.booking.event.events.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.event.events.dto.event.AllEventsResponseDTO;
import com.booking.event.events.dto.event.CreateEventRequestDTO;
import com.booking.event.events.dto.event.DeleteEventResponseDTO;
import com.booking.event.events.dto.event.EventResponseDTO;
import com.booking.event.events.service.EventService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/events")
public class EventController {
  private EventService eventService;

  public EventController(EventService eventService){
    this.eventService = eventService;
  }


  @GetMapping 
  public ResponseEntity<AllEventsResponseDTO> getAllEvents(){
    return ResponseEntity.status(HttpStatus.OK).body(eventService.getAllEvents());
  }

  @PostMapping 
  public ResponseEntity<EventResponseDTO> createEvent(
    @Valid @RequestBody CreateEventRequestDTO request
  ){
    return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
  }
  
  @GetMapping ("/{id}")
  public ResponseEntity<EventResponseDTO> getEventById(@PathVariable Long id){
    return ResponseEntity.status(HttpStatus.OK).body(eventService.getEventById(id));
  }

  @PutMapping("/{id}")
public ResponseEntity<EventResponseDTO> editEventById(
        @PathVariable Long id,
        @Valid @RequestBody CreateEventRequestDTO request) {

    return ResponseEntity.ok(
            eventService.editEventById(id, request)
    );
}

  @DeleteMapping("/{id}")
public ResponseEntity<DeleteEventResponseDTO> deleteEventById(
        @PathVariable Long id) {

    return ResponseEntity.ok(
            eventService.deleteEventById(id)
    );
}
}
