package com.example.JDK.controller.api;


// Refactor: categorised as api controller; moved for structure-only readability.
import com.example.JDK.entity.Notification;
import com.example.JDK.entity.User;
import com.example.JDK.repository.UserRepository;
import com.example.JDK.service.NotificationService;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("not found: " + email));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal principal) {
        String email = principal.getName();
        User me = requireUser(email);
        long count = notificationService.getUnreadCount(me);
        return Map.of("count", count);
    }

    @GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> list(
            Principal principal,
            @RequestParam(defaultValue = "10") int limit) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // limit 가드 (1~50 사이로)
        limit = Math.max(1, Math.min(limit, 50));

        User me = requireUser(principal.getName());

        Page<Notification> page =
                notificationService.getMyNotifications(me, PageRequest.of(0, limit));

        var fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME; // "2025-08-13T11:45:00"
        List<Map<String, Object>> body = page.getContent().stream()
                .map(n -> Map.<String, Object>of(
                        "id",        n.getId(),
                        "message",   n.getMessage(),
                        "linkUrl",   n.getLinkUrl(),
                        "read",      n.isRead(),
                        "createdAt", n.getCreatedAt() == null ? null : n.getCreatedAt().format(fmt)
                ))
                .toList();

        return ResponseEntity.ok(body); // ❗ 항상 200 + [] 보장
    }

    @PatchMapping("/{id}/read")
    public void markRead(Principal principal, @PathVariable Long id) {
        String email = principal.getName();
        User me = requireUser(email);
        notificationService.markAsRead(me, id);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAll(Principal principal) {
        String email = principal.getName();
        User me = requireUser(email);
        int updated = notificationService.markAllAsRead(me);
        return Map.of("updated", updated);
    }
}
