package com.example.JDK.controller.admin;


// Refactor: categorised as admin controller; moved for structure-only readability.
import com.example.JDK.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 카테고리 목록 */
    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categoryList", categoryService.getAll());
        return "admin/categories";
    }

    /** 카테고리 추가 */
    @PostMapping("/add")
    public String addCategory(@RequestParam String name) {
        categoryService.create(name);
        return "redirect:/admin/categories";
    }

    /** 수정 폼 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.getById(id));
        return "admin/category-edit";
    }

    /** 수정 처리 */
    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable Long id, @RequestParam String name) {
        categoryService.update(id, name);
        return "redirect:/admin/categories";
    }

    /** 삭제 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/admin/categories";
    }
}
