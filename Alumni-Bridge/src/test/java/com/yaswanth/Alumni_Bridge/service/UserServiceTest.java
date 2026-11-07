package com.yaswanth.Alumni_Bridge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("John Doe");
        sampleUser.setEmail("john@example.com");
        sampleUser.setPassword("plainPass");
        sampleUser.setRole("STUDENT");
        sampleUser.setBlocked(false);
    }

    @Test
    void testRegisterUser_Success_HashesPassword() {
        sampleUser.setId(null);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = userService.registerUser(sampleUser);

        assertNotNull(registered);
        assertEquals("$2a$12$hashedPassword", registered.getPassword());
        assertFalse(registered.getBlocked());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void testRegisterUser_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        sampleUser.setId(null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(sampleUser);
        });

        assertTrue(thrown.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLoginUser_Success() {
        sampleUser.setPassword("$2a$12$hashedPassword");
        when(userRepository.findByEmail("john@example.com")).thenReturn(sampleUser);
        when(passwordEncoder.matches("rawPassword", "$2a$12$hashedPassword")).thenReturn(true);

        User loggedIn = userService.loginUser("john@example.com", "rawPassword");

        assertNotNull(loggedIn);
        assertEquals("John Doe", loggedIn.getName());
    }

    @Test
    void testLoginUser_BlockedUser_Rejected() {
        sampleUser.setBlocked(true);
        when(userRepository.findByEmail("john@example.com")).thenReturn(sampleUser);

        User loggedIn = userService.loginUser("john@example.com", "rawPassword");

        assertNull(loggedIn);
    }

    @Test
    void testLoginUser_WrongPassword_Rejected() {
        sampleUser.setPassword("$2a$12$hashedPassword");
        when(userRepository.findByEmail("john@example.com")).thenReturn(sampleUser);
        when(passwordEncoder.matches("wrongPassword", "$2a$12$hashedPassword")).thenReturn(false);

        User loggedIn = userService.loginUser("john@example.com", "wrongPassword");

        assertNull(loggedIn);
    }

    @Test
    void testDeleteUser_CascadesCleanly() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleUser));

        userService.deleteUser(1L);

        verify(messageRepository, times(1)).deleteBySenderOrReceiver(sampleUser, sampleUser);
        verify(eventRegistrationRepository, times(1)).deleteByUser(sampleUser);
        verify(applicationRepository, times(1)).deleteByUser(sampleUser);
        verify(userRepository, times(1)).delete(sampleUser);
    }

    @Test
    void testSearchUsers_MultiField_CallsRepository() {
        when(userRepository.searchDirectory("Java", "ALUMNI", "Google", "Spring", "2024", null, 1L))
                .thenReturn(List.of(sampleUser));

        List<User> results = userService.searchUsers("Java", "ALUMNI", "Google", "Spring", "2024", 1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("John Doe", results.get(0).getName());
        verify(userRepository, times(1)).searchDirectory("Java", "ALUMNI", "Google", "Spring", "2024", null, 1L);
    }

    @Test
    void testSearchUsers_WithMentorshipFilter_CallsRepository() {
        when(userRepository.searchDirectory("", "ALUMNI", "", "", "", true, 1L))
                .thenReturn(List.of(sampleUser));

        List<User> results = userService.searchUsers("", "ALUMNI", "", "", "", true, 1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(userRepository, times(1)).searchDirectory("", "ALUMNI", "", "", "", true, 1L);
    }
}
