package com.yaswanth.Alumni_Bridge.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Profile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String skills;
	private String company;
	private String experience;
	
	@Column(columnDefinition = "TEXT")
	private String bio;

	private String degree;
	private String branch;
	private String graduationYear;
	private String phone;
	private String linkedinUrl;
	private String githubUrl;

	@OneToOne
	@JoinColumn(name = "user_id")
	private User user;
}
