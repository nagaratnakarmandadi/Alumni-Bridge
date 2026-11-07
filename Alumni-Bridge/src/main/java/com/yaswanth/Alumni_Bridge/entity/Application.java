package com.yaswanth.Alumni_Bridge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "application", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "job_id"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Application {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "job_id")
	private Job job;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	private String status = "APPLIED"; // APPLIED, REVIEWED, SHORTLISTED, ACCEPTED, REJECTED

	private String appliedDate;
	private String resumeUrl;
	private String coverNote;
}
