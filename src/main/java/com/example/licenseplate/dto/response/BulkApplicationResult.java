package com.example.licenseplate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkApplicationResult {
    private int totalRequested;
    private int successful;
    private int failed;
    @Builder.Default
    private List<ApplicationDto> successfulApplications = new ArrayList<>();
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}