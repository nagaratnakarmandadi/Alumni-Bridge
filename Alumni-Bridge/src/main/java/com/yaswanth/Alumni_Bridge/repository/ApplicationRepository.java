package com.yaswanth.Alumni_Bridge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.yaswanth.Alumni_Bridge.entity.Application;
import com.yaswanth.Alumni_Bridge.entity.Job;
import com.yaswanth.Alumni_Bridge.entity.User;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByUserAndJob(User user, Job job);

    List<Application> findByUser(User user);

    List<Application> findByJob(Job job);

    void deleteByJob(Job job);

    void deleteByUser(User user);
}
