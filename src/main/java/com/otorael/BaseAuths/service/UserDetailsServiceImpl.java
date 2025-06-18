package com.otorael.BaseAuths.service;


import com.otorael.BaseAuths.model.Auths;
import com.otorael.BaseAuths.repository.AuthsRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthsRepository authsRepository;

    public UserDetailsServiceImpl(AuthsRepository authsRepository) {
        this.authsRepository = authsRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Auths auths = authsRepository.findByEmail(email).orElseThrow(()-> new UsernameNotFoundException("User with email "+email+" not found"));
        return auths;
    }
}
