package com.esame.EsameReact_be.service;

import com.esame.EsameReact_be.dto.AuthResponse;
import com.esame.EsameReact_be.dto.LoginRequest;
import com.esame.EsameReact_be.dto.RegisterRequest;
import com.esame.EsameReact_be.entity.AuthAccount;
import com.esame.EsameReact_be.entity.RefreshToken;
import com.esame.EsameReact_be.entity.Utente;
import com.esame.EsameReact_be.repository.AuthAccountRepository;
import com.esame.EsameReact_be.repository.RefreshTokenRepository;
import com.esame.EsameReact_be.repository.UtenteRepository;
import com.esame.EsameReact_be.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UtenteRepository utenteRepository;
    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public AuthResponse register(RegisterRequest request) {
        if (utenteRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email già registrata");
        }

        Utente utente = new Utente();
        utente.setEmail(request.email());
        utente.setName(request.name());
        utente = utenteRepository.save(utente);

        AuthAccount account = new AuthAccount();
        account.setUtente(utente);
        account.setProvider("credentials");
        account.setProviderAccountId(utente.getEmail());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        authAccountRepository.save(account);

        return buildAuthResponse(utente);
    }

    public AuthResponse login(LoginRequest request) {
        Utente utente = utenteRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide"));

        AuthAccount account = authAccountRepository.findByUtenteAndProvider(utente, "credentials")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
        }

        return buildAuthResponse(utente);
    }

    public AuthResponse refresh(String rawToken) {
        String hash = hashToken(rawToken);

        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(hash, OffsetDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token non valido o scaduto"));

        // Ruota il token: revoca il vecchio, emetti un nuovo
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        Utente utente = stored.getUtente();
        return buildAuthResponse(utente);
    }

    public void logout(String rawToken) {
        String hash = hashToken(rawToken);
        refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(hash, OffsetDateTime.now())
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    public Utente findOrCreateOAuth2User(String email, String name, String avatarUrl,
                                          String provider, String providerAccountId) {
        // Cerca account OAuth2 già esistente
        Optional<AuthAccount> existingAccount =
                authAccountRepository.findByProviderAndProviderAccountId(provider, providerAccountId);

        if (existingAccount.isPresent()) {
            Utente utente = existingAccount.get().getUtente();
            utente.setName(name);
            utente.setAvatarUrl(avatarUrl);
            return utenteRepository.save(utente);
        }

        // Cerca utente per email (può già esistere con credenziali o altro provider)
        Utente utente = utenteRepository.findByEmail(email).orElseGet(() -> {
            Utente nuovo = new Utente();
            nuovo.setEmail(email);
            nuovo.setName(name);
            nuovo.setAvatarUrl(avatarUrl);
            nuovo.setEmailVerified(true);
            return utenteRepository.save(nuovo);
        });

        // Collega il nuovo provider all'utente
        AuthAccount account = new AuthAccount();
        account.setUtente(utente);
        account.setProvider(provider);
        account.setProviderAccountId(providerAccountId);
        authAccountRepository.save(account);

        return utente;
    }

    public String createRefreshToken(Utente utente) {
        String rawToken = UUID.randomUUID().toString();

        RefreshToken rt = new RefreshToken();
        rt.setUtente(utente);
        rt.setTokenHash(hashToken(rawToken));
        rt.setExpiresAt(OffsetDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS));
        refreshTokenRepository.save(rt);

        return rawToken;
    }

    private AuthResponse buildAuthResponse(Utente utente) {
        String accessToken = jwtService.generateToken(utente.getEmail());
        String refreshToken = createRefreshToken(utente);
        return new AuthResponse(accessToken, refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
    }
}
