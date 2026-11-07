package com.yaswanth.Alumni_Bridge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	@Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.profile p WHERE " +
	       "(:keyword IS NULL OR :keyword = '' OR " +
	       " LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(p.company) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(p.skills) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(p.branch) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(p.degree) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(p.graduationYear) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
	       "(:role IS NULL OR :role = '' OR u.role = :role) AND " +
	       "(:company IS NULL OR :company = '' OR LOWER(p.company) LIKE LOWER(CONCAT('%', :company, '%'))) AND " +
	       "(:skill IS NULL OR :skill = '' OR LOWER(p.skills) LIKE LOWER(CONCAT('%', :skill, '%'))) AND " +
	       "(:gradYear IS NULL OR :gradYear = '' OR p.graduationYear = :gradYear) AND " +
	       "(:currentUserId IS NULL OR u.id != :currentUserId) AND (u.role != 'ADMIN') AND (u.blocked = false OR u.blocked IS NULL) " +
	       "ORDER BY u.name ASC")
	List<User> searchDirectory(@Param("keyword") String keyword,
	                           @Param("role") String role,
	                           @Param("company") String company,
	                           @Param("skill") String skill,
	                           @Param("gradYear") String gradYear,
	                           @Param("currentUserId") Long currentUserId);
}
