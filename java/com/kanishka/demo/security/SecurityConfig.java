package com.kanishka.demo.security;

import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // ── Public ──────────────────────────────────────────────────
                        .requestMatchers(
                                "/", "/css/**", "/js/**", "/images/**", "/videos/**",
                                "/products", "/products/*",
                                "/auth/**", "/error",
                                "/profile/picture/**"
                        ).permitAll()

                        // Public GET for product reviews
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/products/*/reviews"
                        ).permitAll()

                        // ── Admin only ───────────────────────────────────────────────
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**").hasAnyRole("ADMIN", "STAFF")

                        // ── Must be logged in ────────────────────────────────────────
                        .requestMatchers(
                                "/profile", "/profile/**",
                                "/addresses/**",
                                "/orders/**", "/cart/**", "/checkout/**",
                                "/products/*/reviews"
                        ).authenticated()

                        .requestMatchers("/wholesale/**").hasAnyRole("USER", "ADMIN")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )

                .rememberMe(remember -> remember
                        .key("goldenSecretKey")
                        .userDetailsService(userDetailsService())
                        .tokenValiditySeconds(604800)
                        .rememberMeParameter("remember-me")
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                response.sendRedirect("/auth/login"))
                )

                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account: " + email));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}