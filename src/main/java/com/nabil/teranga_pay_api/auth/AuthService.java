package com.nabil.teranga_pay_api.auth;

import com.nabil.teranga_pay_api.account.Account;
import com.nabil.teranga_pay_api.account.AccountRepository;
import com.nabil.teranga_pay_api.common.PhoneNormalizer;
import com.nabil.teranga_pay_api.user.Role;
import com.nabil.teranga_pay_api.user.User;
import com.nabil.teranga_pay_api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

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

        return new RegisterResponse(user.getId(), user.getFullName(), user.getPhone());
    }
}