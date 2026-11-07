package com.yaswanth.Alumni_Bridge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yaswanth.Alumni_Bridge.entity.Event;
import com.yaswanth.Alumni_Bridge.entity.EventRegistration;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.EventRegistrationRepository;
import com.yaswanth.Alumni_Bridge.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @InjectMocks
    private EventService eventService;

    private Event sampleEvent;
    private User attendee;

    @BeforeEach
    void setUp() {
        sampleEvent = new Event();
        sampleEvent.setId(30L);
        sampleEvent.setTitle("Alumni Meetup");
        sampleEvent.setDate(LocalDate.now().plusDays(10));
        sampleEvent.setLocation("Campus Hall");

        attendee = new User();
        attendee.setId(7L);
        attendee.setName("Alice");
    }

    @Test
    void testRegister_Success() {
        when(eventRegistrationRepository.existsByUserAndEvent(attendee, sampleEvent)).thenReturn(false);
        when(eventRegistrationRepository.save(any(EventRegistration.class))).thenAnswer(i -> i.getArgument(0));

        eventService.register(attendee, sampleEvent);

        verify(eventRegistrationRepository, times(1)).save(any(EventRegistration.class));
    }

    @Test
    void testRegister_AlreadyRegistered_DoesNotDuplicate() {
        when(eventRegistrationRepository.existsByUserAndEvent(attendee, sampleEvent)).thenReturn(true);

        eventService.register(attendee, sampleEvent);

        verify(eventRegistrationRepository, never()).save(any());
    }

    @Test
    void testDeleteEvent_CascadesRegistrations() {
        when(eventRepository.findById(30L)).thenReturn(Optional.of(sampleEvent));

        eventService.deleteEvent(30L);

        verify(eventRegistrationRepository, times(1)).deleteByEvent(sampleEvent);
        verify(eventRepository, times(1)).delete(sampleEvent);
    }
}
