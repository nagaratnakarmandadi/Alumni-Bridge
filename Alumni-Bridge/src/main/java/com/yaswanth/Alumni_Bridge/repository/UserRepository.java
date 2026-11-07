package com.yaswanth.Alumni_Bridge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.yaswanth.Alumni_Bridge.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	User findByEmail(String email);

	boolean existsByEmail(String email);

	long countByRole(String role);

	List<User> findByNameContainingIgnoreCase(String name);

	List<User> findByRole(String role);

	List<User> findByNameContainingIgnoreCaseAndRole(String name, String role);

	List<User> findByIdNotAndRoleNot(Long id, String role);
}
