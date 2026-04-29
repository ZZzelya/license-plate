package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.PassportChangeRequestCreateRequest;
import com.example.licenseplate.dto.request.PassportChangeRequestReviewRequest;
import com.example.licenseplate.dto.response.PassportChangeRequestDto;
import com.example.licenseplate.service.PassportChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/passport-change-requests")
@RequiredArgsConstructor
public class PassportChangeRequestController {

    private final PassportChangeRequestService passportChangeRequestService;

    @PostMapping
    public ResponseEntity<PassportChangeRequestDto> createRequest(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody PassportChangeRequestCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(passportChangeRequestService.createRequest(authHeader, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PassportChangeRequestDto>> getMyRequests(
        @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(passportChangeRequestService.getMyRequests(authHeader));
    }

    @GetMapping
    public ResponseEntity<List<PassportChangeRequestDto>> getAllRequests(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(passportChangeRequestService.getAllRequests(authHeader, status));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<PassportChangeRequestDto> approveRequest(
        @PathVariable Long id,
        @RequestHeader("Authorization") String authHeader,
        @RequestBody(required = false) PassportChangeRequestReviewRequest request
    ) {
        PassportChangeRequestReviewRequest payload = request == null
            ? new PassportChangeRequestReviewRequest()
            : request;
        return ResponseEntity.ok(passportChangeRequestService.approveRequest(id, authHeader, payload));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<PassportChangeRequestDto> rejectRequest(
        @PathVariable Long id,
        @RequestHeader("Authorization") String authHeader,
        @RequestBody(required = false) PassportChangeRequestReviewRequest request
    ) {
        PassportChangeRequestReviewRequest payload = request == null
            ? new PassportChangeRequestReviewRequest()
            : request;
        return ResponseEntity.ok(passportChangeRequestService.rejectRequest(id, authHeader, payload));
    }
}
