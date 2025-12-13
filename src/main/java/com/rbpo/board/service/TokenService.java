package com.rbpo.board.service;

import com.rbpo.board.jwt.JwtTokenProvider;
import com.rbpo.board.model.SessionStatus;
import com.rbpo.board.model.User;
import com.rbpo.board.model.UserSession;
import com.rbpo.board.repository.UserRepository;
import com.rbpo.board.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    public TokenService(JwtTokenProvider jwtTokenProvider, UserRepository userRepository, UserSessionRepository userSessionRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional
    public Map<String, String> generateTokenPair(User user) {
        String sessionId = UUID.randomUUID().toString();
        
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, sessionId);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionId(sessionId);
        session.setRefreshToken(refreshToken);
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        
        userSessionRepository.save(session);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("tokenType", "Bearer");
        
        return tokens;
    }

    @Transactional
    public Map<String, String> refreshTokenPair(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new RuntimeException("Session is not active");
        }

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(SessionStatus.EXPIRED);
            userSessionRepository.save(session);
            throw new RuntimeException("Refresh token expired");
        }

        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(session);

        User user = session.getUser();
        return generateTokenPair(user);
    }

    @Transactional
    public void revokeSession(String refreshToken) {
        userSessionRepository.findByRefreshToken(refreshToken)
                .ifPresent(session -> {
                    session.setStatus(SessionStatus.REVOKED);
                    session.setRevokedAt(LocalDateTime.now());
                    userSessionRepository.save(session);
                });
    }
}

