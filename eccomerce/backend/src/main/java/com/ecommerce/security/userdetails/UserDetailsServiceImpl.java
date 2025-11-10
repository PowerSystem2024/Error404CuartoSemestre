package com.ecommerce.security.userdetails;

import com.ecommerce.model.entity.Profile;
import com.ecommerce.model.entity.User;
import com.ecommerce.repository.ProfileRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Profile Not Found for user: " + email));

        return UserDetailsImpl.build(user, profile);
    }
}
