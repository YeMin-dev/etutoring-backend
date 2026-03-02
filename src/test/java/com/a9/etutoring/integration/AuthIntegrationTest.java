package com.a9.etutoring.integration;

import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void signupSuccess() throws Exception {
        String body = """
            {
              "username":"student1",
              "firstName":"Stu",
              "lastName":"Dent",
              "email":"student1@example.com",
              "password":"Password123"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void loginSuccess() throws Exception {
        createUser("loginuser", "login@example.com", UserRole.STUDENT, "Password123");

        String body = """
            {
              "username":"loginuser",
              "password":"Password123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void duplicateUsernameAndEmailConflict() throws Exception {
        createUser("dupuser", "dup@example.com", UserRole.STUDENT, "Password123");

        String duplicateUsernameBody = """
            {
              "username":"dupuser",
              "firstName":"A",
              "lastName":"B",
              "email":"new@example.com",
              "password":"Password123"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateUsernameBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));

        String duplicateEmailBody = """
            {
              "username":"newuser",
              "firstName":"A",
              "lastName":"B",
              "email":"dup@example.com",
              "password":"Password123"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateEmailBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void meUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meReturnsPrincipalData() throws Exception {
        createUser("meuser", "me@example.com", UserRole.STUDENT, "Password123");
        String token = loginAndGetToken("meuser", "Password123");

        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("meuser"))
            .andExpect(jsonPath("$.email").value("me@example.com"))
            .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void roleBasedEndpointAccessChecks() throws Exception {
        createUser("admin", "admin@example.com", UserRole.ADMIN, "Password123");
        createUser("tutor", "tutor@example.com", UserRole.TUTOR, "Password123");
        createUser("student", "student@example.com", UserRole.STUDENT, "Password123");

        String adminToken = loginAndGetToken("admin", "Password123");
        String tutorToken = loginAndGetToken("tutor", "Password123");
        String studentToken = loginAndGetToken("student", "Password123");

        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tutor/ping").header("Authorization", "Bearer " + tutorToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/ping").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk());
    }

    private void createUser(String username, String email, UserRole role, String rawPassword) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setCreatedDate(Instant.now());
        userRepository.save(user);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = """
            {
              "username":"%s",
              "password":"%s"
            }
            """.formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(response);
        if (!matcher.find()) {
            throw new IllegalStateException("accessToken not found in response");
        }
        return matcher.group(1);
    }
}
