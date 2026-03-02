package com.a9.etutoring.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RoleProtectedController {

    @GetMapping("/admin/ping")
    public Map<String, String> adminPing() {
        return Map.of("message", "admin-ok");
    }

    @GetMapping("/tutor/ping")
    public Map<String, String> tutorPing() {
        return Map.of("message", "tutor-ok");
    }

    @GetMapping("/student/ping")
    public Map<String, String> studentPing() {
        return Map.of("message", "student-ok");
    }
}
