// src/main/java/com/example/JDK/config/SecurityConfig.java
package com.example.JDK.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // ★ Mustache에서 {{_csrf.*}}로 바로 쓰게끔 공식 핸들러로 이름을 "_csrf"로 고정
        CsrfTokenRequestAttributeHandler csrfAttrHandler = new CsrfTokenRequestAttributeHandler();
        csrfAttrHandler.setCsrfRequestAttributeName("_csrf");

        http
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // H2 콘솔
                .csrf(csrf -> csrf
                                .csrfTokenRequestHandler(csrfAttrHandler)
                        // 필요하면 로그인만 CSRF 예외 (개발 편의): 아래 주석 해제
                        // .ignoringRequestMatchers(new AntPathRequestMatcher("/view/login", "POST"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/view", "/view/home",
                                "/view/login", "/view/signup", "/view/signup/**",
                                "/css/**", "/js/**", "/images/**", "/webjars/**",
                                "/h2-console/**", "/error", "/favicon.ico",
                                "/uploads/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/view/posts/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/view/mypage/**").hasAnyRole("MEMBER","LAWYER")
                        .anyRequest().authenticated()
                )
                .formLogin(l -> l
                        .loginPage("/view/login")
                        .loginProcessingUrl("/view/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(roleBasedSuccessHandler())   // 역할별 리다이렉트
                        .failureUrl("/view/login?error")
                        .permitAll()
                )
                .logout(l -> l
                        .logoutUrl("/logout")           // POST 전송 권장 (헤더에서 CSRF 포함)
                        .logoutSuccessUrl("/view")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    /** 로그인 성공 시 ADMIN → /admin, 그 외 → /view/mypage */
    private AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN"));
            response.sendRedirect(isAdmin ? "/admin" : "/view");
        };
    }

    /** PasswordEncoder 빈 (필수) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
