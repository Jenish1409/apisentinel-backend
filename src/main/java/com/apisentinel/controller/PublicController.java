package com.apisentinel.controller;

import com.apisentinel.payload.request.ContactRequest;
import com.apisentinel.payload.response.ApiResponseWrapper;
import com.apisentinel.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/contact")
    public ResponseEntity<ApiResponseWrapper<Void>> handleContact(@Valid @RequestBody ContactRequest request) {
        emailService.sendContactEmail(
                request.getName(),
                request.getEmail(),
                request.getSubject(),
                request.getMessage()
        );

        return ResponseEntity.ok(new ApiResponseWrapper<Void>(true, "Message sent successfully", null));
    }
}
