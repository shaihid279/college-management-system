package com.college.cms.service;

import com.college.cms.model.User;
import com.college.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    /** Input username ho ya email, dono se user mil jata hai. */
    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        String id = input == null ? "" : input.trim();
        User u = users.findByUsername(id)
                .or(() -> users.findFirstByEmailIgnoreCase(id))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .disabled(!u.isEnabled() || !u.isApproved())
                .build();
    }
}