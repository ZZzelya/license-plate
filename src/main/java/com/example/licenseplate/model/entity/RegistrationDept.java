package com.example.licenseplate.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registration_depts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"licensePlates", "applications"})
@ToString(exclude = {"licensePlates", "applications"})
public class RegistrationDept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 50)
    private String region;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL,
        fetch = FetchType.LAZY)
    @Builder.Default
    private List<LicensePlate> licensePlates = new ArrayList<>();

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL,
        fetch = FetchType.LAZY)
    @Builder.Default
    private List<Application> applications = new ArrayList<>();
}