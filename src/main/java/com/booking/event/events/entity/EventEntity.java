package com.booking.event.events.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "events")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull 
    private Long organizerId;

    @NotBlank 
    @Size (max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 500)
    private String venue;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @NotNull
    @Min (1)
    private Integer capacity;

    @NotNull
    @Min(0)
    private Integer availableTickets;

    @CreationTimestamp 
    private Instant createdAt;

    @UpdateTimestamp 
    private Instant updatedAt;
}
