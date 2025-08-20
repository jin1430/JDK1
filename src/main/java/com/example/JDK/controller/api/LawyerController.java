package com.example.JDK.controller.api;


// Refactor: categorised as api controller; moved for structure-only readability.
import com.example.JDK.entity.Lawyer;
import com.example.JDK.service.LawyerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/lawyers")
@RequiredArgsConstructor
public class LawyerController {

    private final LawyerService lawyerService;

    @PostMapping
    public Lawyer registerLawyer(@RequestBody Lawyer lawyer) {
        return lawyerService.registerLawyer(lawyer);
    }

    @GetMapping("/{id}")
    public Lawyer getLawyer(@PathVariable Long id) {
        return lawyerService.getLawyerById(id);
    }

    @GetMapping
    public List<Lawyer> getAllLawyers() {
        return lawyerService.getAllLawyers();
    }

    @PutMapping("/{id}")
    public Lawyer updateLawyer(@PathVariable Long id, @RequestBody Lawyer lawyer) {
        return lawyerService.updateLawyer(id, lawyer);
    }

    @DeleteMapping("/{id}")
    public boolean deleteLawyer(@PathVariable Long id) {
        return lawyerService.deleteLawyer(id);
    }
}
