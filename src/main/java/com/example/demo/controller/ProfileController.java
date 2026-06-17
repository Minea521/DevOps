package com.example.demo.controller;

import com.example.demo.model.Profile;
import com.example.demo.model.ProfileBuilder;
import com.example.demo.model.ProfileType;
import com.example.demo.service.ProfileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("profiles", profileService.getAllProfiles());
        return "profiles/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Profile profile = ProfileBuilder.buildDefault(ProfileType.STUDENT, "", "", "");
        model.addAttribute("profile", profile);
        model.addAttribute("types", ProfileType.values());
        return "profiles/form";
    }

    @PostMapping
    public String save(@ModelAttribute Profile profile,
                       @RequestParam("photo") MultipartFile photo) throws Exception {
        profileService.createProfile(profile, photo);
        return "redirect:/profiles";
    }

    @GetMapping("/{uuid}")
    public String view(@PathVariable String uuid, Model model) {
        Profile profile = profileService.getProfileByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        model.addAttribute("profile", profile);
        return "profiles/view";
    }

    @GetMapping("/{uuid}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String uuid) throws Exception {
        Profile profile = profileService.getProfileByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        byte[] pdfBytes = profileService.generateIdCardPdf(profile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=idcard-" + profile.getRegistrationNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/batch/pdf")
    public ResponseEntity<byte[]> batchPdf() throws Exception {
        List<Profile> profiles = profileService.getAllProfiles();
        byte[] pdfBytes = profileService.generateBatchPdf(profiles);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=batch-idcards.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}