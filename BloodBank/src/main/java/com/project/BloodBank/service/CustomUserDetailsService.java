package com.project.BloodBank.service;

import com.project.BloodBank.model.User;
import com.project.BloodBank.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The bridge between Spring Security and this application's user table.
//
// Spring Security knows nothing about how accounts are stored. It only knows this interface: give
// it whatever was typed in the username box, and get back a UserDetails. Everything after that -
// comparing the password hash, checking the account is enabled, reading the authorities - is done
// by the framework against the object returned here.
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Called once per sign-in attempt. "Username" here is the email, since that is what the form
    // asks for and what User.getUsername() returns.
    //
    // Deactivated accounts are deliberately not filtered out. Returning the row lets the framework
    // reject it through isEnabled(), which gives the user a proper "account disabled" outcome
    // instead of the same "bad credentials" as a typo.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        return user;
    }
}
