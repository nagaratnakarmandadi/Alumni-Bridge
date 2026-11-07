package com.yaswanth.Alumni_Bridge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.yaswanth.Alumni_Bridge.entity.Event;
import com.yaswanth.Alumni_Bridge.entity.User;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

	List<Event> findAll();

	List<Event> findByCreatedBy(User user);
}
