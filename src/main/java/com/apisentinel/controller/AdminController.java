package com.apisentinel.controller;

import com.apisentinel.entity.MonitoredApi;
import com.apisentinel.entity.User;
import com.apisentinel.payload.response.AdminUserResponse;
import com.apisentinel.payload.response.AdminUserResponse.ApiSummary;
import com.apisentinel.payload.response.ApiResponseWrapper;
import com.apisentinel.repository.ApiCheckHistoryRepository;
import com.apisentinel.repository.ApiIncidentRepository;
import com.apisentinel.repository.MonitoredApiRepository;
import com.apisentinel.repository.UserRepository;
import com.apisentinel.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired UserRepository userRepository;
    @Autowired MonitoredApiRepository apiRepository;
    @Autowired ApiCheckHistoryRepository historyRepository;
    @Autowired ApiIncidentRepository incidentRepository;
    @Autowired EmailService emailService;

    /** List all users — richer profile, zero sensitive data */
    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        List<AdminUserResponse> users = userRepository.findAll().stream()
                .map(u -> {
                    List<MonitoredApi> apis = apiRepository.findByUserId(u.getId());

                    List<ApiSummary> apiSummaries = apis.stream().map(api -> {
                        long total = historyRepository.countByApiId(api.getId());
                        long upCount = historyRepository.countByApiIdAndStatus(api.getId(),
                                com.apisentinel.entity.CheckStatus.UP);
                        double uptime = total > 0 ? (upCount * 100.0 / total) : 0;
                        String currentStatus = api.getLastCheckedAt() == null ? "PENDING"
                                : (historyRepository.findTopByApiIdOrderByCheckedAtDesc(api.getId())
                                    .map(h -> h.getStatus().name()).orElse("PENDING"));
                        return new ApiSummary(api.getId(), api.getName(), api.getMethod(),
                                api.getUrl(), currentStatus, Math.round(uptime * 100.0) / 100.0);
                    }).toList();

                    long totalIncidents = apis.stream()
                            .mapToLong(api -> incidentRepository.countByApiId(api.getId())).sum();

                    LocalDateTime lastActivity = apis.stream()
                            .map(MonitoredApi::getLastCheckedAt)
                            .filter(t -> t != null)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    return new AdminUserResponse(u.getId(), u.getEmail(), u.getRole(),
                            u.getCreatedAt(), apis.size(), totalIncidents, lastActivity, apiSummaries);
                })
                .toList();

        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Users fetched", users));
    }

    /** Delete a user and ALL their data (proper cascade order) */
    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        // Step 1: delete check history for each API
        List<MonitoredApi> apis = apiRepository.findByUserId(id);
        for (MonitoredApi api : apis) {
            historyRepository.deleteByApiId(api.getId());
            incidentRepository.deleteByApiId(api.getId());
        }

        // Step 2: delete the APIs themselves
        apiRepository.deleteAll(apis);

        // Step 3: delete the user
        userRepository.deleteById(id);

        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "User and all associated data deleted."));
    }

    /** Promote or demote a user's role */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        if (newRole == null || (!newRole.equals("ROLE_ADMIN") && !newRole.equals("ROLE_USER"))) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseWrapper<>(false, "Role must be ROLE_ADMIN or ROLE_USER."));
        }
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User user = opt.get();
        user.setRole(newRole);
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Role updated to " + newRole));
    }

    /** Send a message to a user via email */
    @PostMapping("/users/{id}/message")
    public ResponseEntity<?> sendMessage(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String subject = body.get("subject");
        String message = body.get("message");
        if (subject == null || message == null || subject.isBlank() || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseWrapper<>(false, "Subject and message are required."));
        }
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        try {
            emailService.sendAdminMessage(opt.get().getEmail(), subject, message);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponseWrapper<>(false, "Failed to send email. Check SMTP config."));
        }
        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Message sent to " + opt.get().getEmail()));
    }
}
