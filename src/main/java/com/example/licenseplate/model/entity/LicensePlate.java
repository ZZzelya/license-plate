package com.example.licenseplate.model.entity;

import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.model.enums.PlateStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "license_plates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"department", "applications"})
@ToString(exclude = {"department", "applications"})
public class LicensePlate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", unique = true, nullable = false, length = 20)
    private String plateNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String series;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", nullable = false)
    private RegistrationDept department;

    @OneToMany(mappedBy = "licensePlate", cascade = CascadeType.ALL,
        fetch = FetchType.LAZY)
    @Builder.Default
    private List<Application> applications = new ArrayList<>();

    public PlateStatus getCurrentStatus() {
        if (applications == null || applications.isEmpty()) {
            return PlateStatus.AVAILABLE;
        }

        return applications.stream()
            .filter(app -> app.getStatus() == ApplicationStatus.PENDING ||
                app.getStatus() == ApplicationStatus.CONFIRMED ||
                app.getStatus() == ApplicationStatus.COMPLETED)
            .max((a1, a2) -> a2.getSubmissionDate().compareTo(a1.getSubmissionDate()))
            .map(app -> {
                return switch (app.getStatus()) {
                    case PENDING -> PlateStatus.RESERVED;
                    case CONFIRMED -> PlateStatus.RESERVED;
                    case COMPLETED -> PlateStatus.ISSUED;
                    default -> PlateStatus.AVAILABLE;
                };
            })
            .orElse(PlateStatus.AVAILABLE);
    }

    public boolean isAvailable() {
        return getCurrentStatus() == PlateStatus.AVAILABLE;
    }
}