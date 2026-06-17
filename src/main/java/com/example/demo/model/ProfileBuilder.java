package com.example.demo.model;

import java.time.LocalDate;
import java.util.UUID;

public class ProfileBuilder {

    /**
     * Builds a new Profile with sensible defaults and a generated UUID + registration number.
     */
    public static Profile buildDefault(ProfileType type, String fullName, String department, String title) {
        String uuid = UUID.randomUUID().toString();
        String regNumber = generateRegistrationNumber(department);

        return Profile.builder()
                .uuid(uuid)
                .registrationNumber(regNumber)
                .type(type)
                .fullName(fullName)
                .department(department)
                .title(title)
                .issueDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(4))
                .barcodeType(BarcodeType.CODE_128)
                .build();
    }

    private static String generateRegistrationNumber(String department) {
        String deptCode = (department == null || department.isBlank()) ? "GEN" : department.substring(0, 3).toUpperCase();
        return java.time.Year.now() + "-" + deptCode + "-" + String.format("%03d", (int) (Math.random() * 999) + 1);
    }
}
