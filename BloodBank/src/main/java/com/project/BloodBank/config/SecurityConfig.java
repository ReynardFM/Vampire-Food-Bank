package com.project.BloodBank.config;

import com.project.BloodBank.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// Who may reach what, and how signing in works.
//
// Spring Security works as a chain of filters sitting in front of every request. This class does
// not check anything itself - it describes the rules, and the framework enforces them before a
// controller is ever called. That is why the controllers contain almost no security code.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    // @Bean means Spring creates this once and injects it wherever a PasswordEncoder is asked for -
    // here into the provider below, and into UserService when it hashes a new password.
    //
    // BCrypt is built for passwords: deliberately slow, so guessing at scale is expensive, and
    // salted, so the same password hashes differently every time and identical passwords cannot be
    // spotted in the table.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Joins the two halves of authentication: where users come from, and how their password is
    // checked. Given both, the framework does the comparison itself - nothing in this project ever
    // handles a password after registration.
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // The rules themselves.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Matched strictly top to bottom: the first pattern that matches a URL decides it,
                // and nothing below is consulted. So these must run from most specific to least,
                // and putting anyRequest() first would make everything after it dead code.
                .authorizeHttpRequests(auth -> auth
                        // Reachable without signing in. The static folders have to be here or the
                        // login page would load without its own stylesheet, and /error too, or a
                        // failure while signed out would itself be blocked.
                        .requestMatchers("/", "/home", "/about-us", "/login", "/register",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                        // Donations are entered by staff on a donor's behalf, so recording is
                        // admin-only even though /donations/** is otherwise a member area.
                        .requestMatchers("/admin/**", "/dashboard", "/dashboard/**",
                                "/donations/record/**")
                        .hasAuthority("ROLE_ADMIN")
                        // Any signed-in account. Note this cannot express "your own request only" -
                        // that depends on the row, so DonationRequestController checks it itself.
                        .requestMatchers("/donor/**", "/requests/**", "/donations/**").authenticated()

                        // The catch-all. Anything not listed above requires signing in, so a new
                        // controller is private by default rather than accidentally public.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // Where to go after signing in. Sending everyone to one page would drop
                        // administrators on a donor profile, so it is decided per role here.
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                            if (isAdmin) {
                                response.sendRedirect("/dashboard");
                            } else {
                                response.sendRedirect("/donor/profile");
                            }
                        })
                        .permitAll()
                )
                // Signing out clears everything rather than just forgetting the authentication:
                // the session is destroyed and the cookie deleted, so nothing is left for a shared
                // machine's next user to resume. ?logout is what makes the confirmation appear.
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
