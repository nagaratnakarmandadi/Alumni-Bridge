package com.yaswanth.Alumni_Bridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

	private Boolean openForMentorship = false;

	@OneToOne
	@JoinColumn(name = "user_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private User user;
}
