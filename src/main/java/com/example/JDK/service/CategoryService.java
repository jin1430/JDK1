package com.example.JDK.service;

import com.example.JDK.entity.Category;
import com.example.JDK.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** 전체 카테고리 조회 (id 오름차순) */
    public List<Category> getAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    /** ID로 단건 조회 */
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. id=" + id));
    }

    /** JPA reference (연관관계 주입용) */
    public Category getRef(Long id) {
        return categoryRepository.getReferenceById(id);
    }

    /** 이름으로 조회 */
    public Category getByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. name=" + name));
    }

    @Transactional
    public Category create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리명은 비어 있을 수 없습니다.");
        }
        if (categoryRepository.existsByName(name)) {
            return categoryRepository.findByName(name).get();
        }
        Category c = new Category();
        c.setName(name);
        return categoryRepository.save(c);
    }

    @Transactional
    public Category update(Long id, String name) {
        Category c = getById(id);
        c.setName(name);
        return c; // Dirty checking
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    /** 초기 기본값(필요 시 호출) */
    @Transactional
    public void ensureDefaults() {
        if (categoryRepository.count() == 0) {
            create("성범죄");
            create("폭행/협박");
            create("명예훼손/모욕");
            create("재산범죄");
            create("교통사고/범죄");
        }
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() { return categoryRepository.findAll(); }

    @Transactional(readOnly = true)
    public Category findByNameOrThrow(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("카테고리 없음: " + name));
    }
}
