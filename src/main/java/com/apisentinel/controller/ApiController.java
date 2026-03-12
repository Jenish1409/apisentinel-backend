package com.apisentinel.controller;

import com.apisentinel.payload.request.ApiRequest;
import com.apisentinel.payload.response.ApiResponse;
import com.apisentinel.payload.response.ApiResponseWrapper;
import com.apisentinel.service.ApiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/apis")
public class ApiController {
    
    @Autowired
    private ApiService apiService;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ResponseEntity<?> registerApi(@Valid @RequestBody ApiRequest apiRequest) {
        try {
            apiService.createApi(apiRequest, getCurrentUserEmail());
            return ResponseEntity.ok(new ApiResponseWrapper<>(true, "API registered successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(false, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserApis() {
        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "APIs fetched successfully", apiService.getUserApis(getCurrentUserEmail())));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary() {
        try {
            return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Dashboard summary fetched successfully", apiService.getDashboardSummary(getCurrentUserEmail())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(false, e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getApiDetails(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Details fetched", apiService.getApiDetails(id, getCurrentUserEmail())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(false, e.getMessage()));
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getApiHistory(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(new ApiResponseWrapper<>(true, "History fetched", apiService.getApiHistory(id, getCurrentUserEmail())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(false, e.getMessage()));
        }
    }

    @GetMapping("/{id}/incidents")
    public ResponseEntity<?> getApiIncidents(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Incidents fetched", apiService.getApiIncidents(id, getCurrentUserEmail())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(false, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateApi(@PathVariable Long id, @Valid @RequestBody ApiRequest apiRequest) {
        try {
            apiService.updateApi(id, apiRequest, getCurrentUserEmail());
            return ResponseEntity.ok(new ApiResponseWrapper<>(true, "API updated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApi(@PathVariable Long id) {
        try {
            apiService.deleteApi(id, getCurrentUserEmail());
            return ResponseEntity.ok(new ApiResponseWrapper<>(true, "API deleted successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(false, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleApi(@PathVariable Long id) {
        try {
            apiService.toggleApi(id, getCurrentUserEmail());
            return ResponseEntity.ok(new ApiResponseWrapper<>(true, "API monitoring toggled successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(false, e.getMessage()));
        }
    }
}
