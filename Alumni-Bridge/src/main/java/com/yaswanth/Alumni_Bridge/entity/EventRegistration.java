package com.yaswanth.Alumni_Bridge.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "event_registration", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "event_id"})
})
@Data
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Event event;

    private String registrationDate;
}
