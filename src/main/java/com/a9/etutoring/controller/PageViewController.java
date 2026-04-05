package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.analytics.PageViewRequest;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.PageViewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class PageViewController {

    private final PageViewService pageViewService;

    public PageViewController(PageViewService pageViewService) {
        this.pageViewService = pageViewService;
    }

    @PostMapping("/page-views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordPageView(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody PageViewRequest request,
        HttpServletRequest httpRequest
    ) {
        String ua = httpRequest.getHeader("User-Agent");
        pageViewService.recordPageView(principal.getId(), request, ua);
    }
}
