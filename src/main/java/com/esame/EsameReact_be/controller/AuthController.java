package com.esame.EsameReact_be.controller;

import com.esame.EsameReact_be.dto.AuthResponse;
import com.esame.EsameReact_be.dto.LoginRequest;
import com.esame.EsameReact_be.dto.RegisterRequest;
import com.esame.EsameReact_be.entity.Utente;
import com.esame.EsameReact_be.repository.UtenteRepository;
import com.esame.EsameReact_be.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController 
{

    private final AuthService authService;
    private final UtenteRepository utenteRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody @Valid RegisterRequest request) 
    {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest request) 
    {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody Map<String, String> body) 
    {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) 
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken obbligatorio");
        }
        return authService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody Map<String, String> body) 
    {
        String refreshToken = body.get("refreshToken");
        if (refreshToken != null && !refreshToken.isBlank()) 
        {
            authService.logout(refreshToken);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Utente> getCurrentUser(Principal principal) 
    {
        return utenteRepository.findByEmail(principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }






}
