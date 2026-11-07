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

import com.yaswanth.Alumni_Bridge.entity.Job;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.ApplicationRepository;
import com.yaswanth.Alumni_Bridge.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private JobService jobService;

    private Job sampleJob;
    private User poster;

    @BeforeEach
    void setUp() {
        poster = new User();
        poster.setId(2L);
        poster.setName("Alumni Jane");
        poster.setRole("ALUMNI");

        sampleJob = new Job();
        sampleJob.setId(10L);
        sampleJob.setTitle("Software Engineer");
        sampleJob.setCompany("Tech Corp");
        sampleJob.setDescription("Exciting opportunity");
        sampleJob.setLocation("Remote");
        sampleJob.setPostedBy(poster);
    }

    @Test
    void testSaveJob() {
        when(jobRepository.save(sampleJob)).thenReturn(sampleJob);

        Job saved = jobService.saveJob(sampleJob);

        assertNotNull(saved);
        assertEquals("Software Engineer", saved.getTitle());
        verify(jobRepository, times(1)).save(sampleJob);
    }

    @Test
    void testGetAllJobs() {
        when(jobRepository.findAll()).thenReturn(List.of(sampleJob));

        List<Job> jobs = jobService.getAllJobs();

        assertEquals(1, jobs.size());
        assertEquals("Tech Corp", jobs.get(0).getCompany());
    }

    @Test
    void testDeleteJob_CascadesApplications() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(sampleJob));

        jobService.deleteJob(10L);

        verify(applicationRepository, times(1)).deleteByJob(sampleJob);
        verify(jobRepository, times(1)).delete(sampleJob);
    }
}
