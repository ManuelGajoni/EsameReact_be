package com.esame.EsameReact_be.repository;

import com.esame.EsameReact_be.entity.RefreshToken;
import com.esame.EsameReact_be.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(
            String tokenHash, OffsetDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.utente = :utente AND rt.revoked = false")
    void revokeAllByUtente(Utente utente);
}
