package com.yaswanth.Alumni_Bridge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yaswanth.Alumni_Bridge.entity.Application;
import com.yaswanth.Alumni_Bridge.entity.Job;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.ApplicationRepository;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private User candidate;
    private Job job;
    private Application application;

    @BeforeEach
    void setUp() {
        candidate = new User();
        candidate.setId(5L);
        candidate.setName("Student Bob");

        job = new Job();
        job.setId(20L);
        job.setTitle("Frontend Developer");

        application = new Application();
        application.setId(100L);
        application.setUser(candidate);
        application.setJob(job);
        application.setStatus("APPLIED");
    }

    @Test
    void testApply_SetsDateAndStatus() {
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        Application saved = applicationService.apply(application);

        assertNotNull(saved);
        assertNotNull(saved.getAppliedDate());
        assertEquals("APPLIED", saved.getStatus());
        verify(applicationRepository, times(1)).save(application);
    }

    @Test
    void testAlreadyApplied_True() {
        when(applicationRepository.existsByUserAndJob(candidate, job)).thenReturn(true);

        boolean result = applicationService.alreadyApplied(candidate, job);

        assertTrue(result);
    }

    @Test
    void testAlreadyApplied_NullUserOrJob_ReturnsFalse() {
        assertFalse(applicationService.alreadyApplied(null, job));
        assertFalse(applicationService.alreadyApplied(candidate, null));
    }

    @Test
    void testUpdateStatus() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        applicationService.updateStatus(100L, "SHORTLISTED");

        assertEquals("SHORTLISTED", application.getStatus());
        verify(applicationRepository, times(1)).save(application);
    }

    @Test
    void testGetApplicationsByJob() {
        when(applicationRepository.findByJob(job)).thenReturn(List.of(application));

        List<Application> list = applicationService.getApplicationsByJob(job);

        assertEquals(1, list.size());
        assertEquals("Frontend Developer", list.get(0).getJob().getTitle());
    }
}
