package com.example.JDK.controller.view;


// Refactor: categorised as view controller; moved for structure-only readability.
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
@RequiredArgsConstructor
@RequestMapping("/view/cases")
public class CaseViewController {

//    private final CaseRepository caseRepository;
//
//    // 판례 상세 페이지
//    @GetMapping("/{id}")
//    public String viewCaseDetail(@PathVariable Long id, Model model) {
//        Case caseEntity = caseRepository.findById(id).orElse(null);
//        if (caseEntity == null) {
//            return "redirect:/view/posts?error=caseNotFound";
//        }
//        model.addAttribute("caseEntity", caseEntity);
//        return "case/detail";  // case/detail.mustache로 랜더링
//    }
}
