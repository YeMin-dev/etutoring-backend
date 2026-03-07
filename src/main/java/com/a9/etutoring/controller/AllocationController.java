package com.a9.etutoring.controller;

import com.a9.etutoring.service.AllocationService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/allocations")
public class AllocationController {

    private static final Logger logger = LoggerFactory.getLogger(AllocationController.class);

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping("/allocate/{studentId}/{tutorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void allocateTutor(@PathVariable UUID studentId, @PathVariable UUID tutorId) {
        logger.info("Starting tutor allocation: studentId={}, tutorId={}", studentId, tutorId);
        allocationService.allocateTutor(studentId, tutorId);
        logger.info("Completed tutor allocation: studentId={}, tutorId={}", studentId, tutorId);
    }
}