package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.UserRegistrationDto;
import by.bsuir.fp.controller.dto.UserResponseDto;
import by.bsuir.fp.exception.DuplicateEmailException;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto register(UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateEmailException("Пользователь с email " + registrationDto.getEmail() + " уже существует");
        }

        User user = User.builder()
                .username(registrationDto.getUsername())
                .email(registrationDto.getEmail())
                .passwordHash(passwordEncoder.encode(registrationDto.getPassword()))
                .defaultCurrency(registrationDto.getDefaultCurrency())
                .build();

        User savedUser = userRepository.save(user);

        return mapToResponseDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + id + " не найден"));
        return mapToResponseDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с email " + email + " не найден"));
        return mapToResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UserRegistrationDto updateDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + id + " не найден"));

        if (!user.getEmail().equals(updateDto.getEmail()) &&
                userRepository.existsByEmail(updateDto.getEmail())) {
            throw new DuplicateEmailException("Email " + updateDto.getEmail() + " уже используется");
        }

        user.setUsername(updateDto.getUsername());
        user.setEmail(updateDto.getEmail());
        user.setDefaultCurrency(updateDto.getDefaultCurrency());

        if (updateDto.getPassword() != null && !updateDto.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(updateDto.getPassword()));
        }

        return mapToResponseDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("Пользователь с id " + id + " не найден");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponseDto createOrUpdateFromGoogle(String email, String name, String googleId) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getGoogleId() == null) {
                        user.setGoogleId(googleId);
                        userRepository.save(user);
                    }
                    return mapToResponseDto(user);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(name)
                            .email(email)
                            .googleId(googleId)
                            .passwordHash(passwordEncoder.encode(Long.toHexString(System.currentTimeMillis())))
                            .defaultCurrency(CurrencyCode.BYN)
                            .build();
                    return mapToResponseDto(userRepository.save(newUser));
                });
    }

    private UserResponseDto mapToResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .defaultCurrency(user.getDefaultCurrency())
                .createdAt(user.getCreatedAt())
                .build();
    }
}