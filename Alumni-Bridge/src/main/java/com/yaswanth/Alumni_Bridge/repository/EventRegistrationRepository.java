package com.yaswanth.Alumni_Bridge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.yaswanth.Alumni_Bridge.entity.Event;
import com.yaswanth.Alumni_Bridge.entity.EventRegistration;
import com.yaswanth.Alumni_Bridge.entity.User;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

	boolean existsByUserAndEvent(User user, Event event);

	List<EventRegistration> findByUser(User user);

	List<EventRegistration> findByEvent(Event event);

	void deleteByEvent(Event event);

	void deleteByUser(User user);
}
