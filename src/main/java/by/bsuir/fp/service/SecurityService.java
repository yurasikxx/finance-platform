package by.bsuir.fp.service;

import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.User;
import by.bsuir.fp.repository.UserRepository;
import by.bsuir.fp.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("User not authenticated");
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

            return user.getId();
        }

        throw new RuntimeException("Unknown authentication type: " + authentication.getClass());
    }

    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("User not authenticated");
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getEmail();
        }

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            return oauthUser.getAttribute("email");
        }

        return authentication.getName();
    }
}