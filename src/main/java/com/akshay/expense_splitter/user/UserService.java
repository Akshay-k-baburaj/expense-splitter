package com.akshay.expense_splitter.user;

import com.akshay.expense_splitter.exception.AccountDisabledException;
import com.akshay.expense_splitter.exception.AccountNotFoundException;
import com.akshay.expense_splitter.exception.DuplicateUserException;
import com.akshay.expense_splitter.exception.EmailVerificationRequiredException;
import com.akshay.expense_splitter.exception.LegacyAccountLinkRequiredException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final String emailClaim;
    private final String emailVerifiedClaim;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            @Value("${auth0.claims.email}") String emailClaim,
            @Value("${auth0.claims.email-verified}") String emailVerifiedClaim) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.emailClaim = emailClaim;
        this.emailVerifiedClaim = emailVerifiedClaim;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Jwt principal) {
        return userMapper.toResponse(requireCurrentUser(principal));
    }

    @Transactional
    public UserResponse createCurrentUser(Jwt principal, CreateUserProfileRequest request) {
        String subject = requireSubject(principal);
        var linkedAccount = userRepository.findByAuth0Subject(subject);
        if (linkedAccount.isPresent()) {
            User existing = linkedAccount.get();
            ensureActive(existing);
            return userMapper.toResponse(existing);
        }

        String email = requireVerifiedEmail(principal);
        if (userRepository.existsByEmail(email)) {
            throw new LegacyAccountLinkRequiredException(
                    "An existing account must be securely linked before this profile can be used.");
        }
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUserException("Username is already in use");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setAuth0Subject(subject);
        try {
            return userMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateUserException("Username or email is already in use");
        }
    }

    @Transactional
    public UserResponse updateCurrentUser(Jwt principal, UpdateUserProfileRequest request) {
        User user = requireCurrentUser(principal);
        String username = request.username().trim();
        if (!username.equals(user.getUsername()) && userRepository.existsByUsernameAndIdNot(username, user.getId())) {
            throw new DuplicateUserException("Username is already in use");
        }
        user.setUsername(username);
        try {
            return userMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateUserException("Username is already in use");
        }
    }

    @Transactional
    public void deactivateCurrentUser(Jwt principal) {
        User user = requireCurrentUser(principal);
        user.setActive(false);
        user.setEmail("archived_" + user.getId() + "_" + user.getEmail());
        user.setUsername("archived_" + user.getId() + "_" + user.getUsername());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User requireCurrentUser(Jwt principal) {
        String subject = requireSubject(principal);
        User user = userRepository.findByAuth0Subject(subject)
                .orElseThrow(() -> new AccountNotFoundException("Account profile is not set up"));
        ensureActive(user);
        return user;
    }

    private String requireSubject(Jwt principal) {
        String subject = principal.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("A valid account subject is required");
        }
        return subject;
    }

    private String requireVerifiedEmail(Jwt principal) {
        String email = principal.getClaimAsString(emailClaim);
        Boolean verified = principal.getClaimAsBoolean(emailVerifiedClaim);
        if (!Boolean.TRUE.equals(verified) || email == null || email.isBlank() || email.length() > 255 || !email.contains("@")) {
            throw new EmailVerificationRequiredException("A verified email address is required to create an account.");
        }
        return email;
    }

    private void ensureActive(User user) {
        if (!user.isActive()) {
            throw new AccountDisabledException("This account is deactivated.");
        }
    }
}
