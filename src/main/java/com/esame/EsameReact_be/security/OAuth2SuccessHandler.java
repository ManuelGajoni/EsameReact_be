package com.esame.EsameReact_be.security;

import com.esame.EsameReact_be.entity.Utente;
import com.esame.EsameReact_be.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google" | "github"

        String email;
        String name;
        String avatarUrl;
        String providerAccountId;

        // Google usa OIDC, GitHub usa OAuth2 puro
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
            name = oidcUser.getFullName();
            avatarUrl = oidcUser.getPicture();
            providerAccountId = oidcUser.getSubject();
        } else {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            // GitHub espone "avatar_url", Google "picture" (già gestito sopra)
            avatarUrl = oauth2User.getAttribute("avatar_url");
            Object id = oauth2User.getAttribute("id");
            providerAccountId = id != null ? id.toString() : null;

            // GitHub può non esporre l'email se impostata come privata
            if (email == null) {
                String login = oauth2User.getAttribute("login");
                email = (login != null ? login : providerAccountId) + "@github.noemail";
            }
        }

        Utente utente = authService.findOrCreateOAuth2User(email, name, avatarUrl, provider, providerAccountId);

        String accessToken = jwtService.generateToken(utente.getEmail());
        String refreshToken = authService.createRefreshToken(utente);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token", accessToken)
                .queryParam("refresh", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
