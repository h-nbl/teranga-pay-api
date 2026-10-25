package com.nabil.teranga_pay_api.auth;

import com.nabil.teranga_pay_api.account.Account;
import com.nabil.teranga_pay_api.account.AccountRepository;
import com.nabil.teranga_pay_api.common.InvalidPhoneNumberException;
import com.nabil.teranga_pay_api.common.PhoneNormalizer;
import com.nabil.teranga_pay_api.user.Role;
import com.nabil.teranga_pay_api.user.User;
import com.nabil.teranga_pay_api.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.nabil.teranga_pay_api.common.PhoneMasker;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       Clock clock) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String phone = PhoneNormalizer.normalize(request.phone());

        if (userRepository.existsByPhone(phone)) {
            throw new PhoneAlreadyRegisteredException();
        }

        Instant now = clock.instant();
        User user = new User(
                request.fullName().strip(),
                phone,
                passwordEncoder.encode(request.password()),
                Role.USER,
                now);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new PhoneAlreadyRegisteredException();
        }

        accountRepository.save(new Account(user, now));
        log.info("User registered: userId={}", user.getId());
        return new RegisterResponse(user.getId(), user.getFullName(), user.getPhone());
    }

    public LoginResponse login(LoginRequest request) {
        Optional<User> found = findByRawPhone(request.phone());

        String hashToCheck = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (found.isEmpty() || !passwordMatches) {
            String reason = found.isEmpty() ? "unknown_number" : "bad_password";
            log.warn("Login failed: reason={} phone={}", reason, PhoneMasker.mask(request.phone()));
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        log.info("Login succeeded: userId={}", user.getId());
        String token = tokenService.issue(user.getId(), user.getRole());
        return new LoginResponse(token, "Bearer", tokenService.expiresInSeconds());
    }

    private Optional<User> findByRawPhone(String rawPhone) {
        try {
            return userRepository.findByPhone(PhoneNormalizer.normalize(rawPhone));
        } catch (InvalidPhoneNumberException e) {
            return Optional.empty();
        }
    }
}