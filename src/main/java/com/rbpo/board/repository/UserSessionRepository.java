package com.rbpo.board.repository;

import com.rbpo.board.model.SessionStatus;
import com.rbpo.board.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionId(String sessionId);
    Optional<UserSession> findByRefreshToken(String refreshToken);
    List<UserSession> findByUserId(Long userId);
    List<UserSession> findByUserIdAndStatus(Long userId, SessionStatus status);
    List<UserSession> findByStatusAndExpiresAtBefore(SessionStatus status, LocalDateTime now);
    void deleteByExpiresAtBefore(LocalDateTime now);
}

