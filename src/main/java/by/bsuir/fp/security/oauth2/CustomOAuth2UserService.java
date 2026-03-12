package by.bsuir.fp.security.oauth2;

import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("sub");

        log.info("OAuth2 login attempt: email={}, name={}, googleId={}", email, name, googleId);

        userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (existingUser.getGoogleId() == null) {
                        existingUser.setGoogleId(googleId);
                        userRepository.save(existingUser);
                        log.info("Google ID linked to existing user: {}", email);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(name != null ? name : email.split("@")[0])
                            .email(email)
                            .googleId(googleId)
                            .passwordHash(UUID.randomUUID().toString())
                            .defaultCurrency(CurrencyCode.BYN)
                            .build();
                    log.info("New user created via Google OAuth2: {}", email);
                    return userRepository.save(newUser);
                });

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                attributes,
                "email"
        );
    }
}