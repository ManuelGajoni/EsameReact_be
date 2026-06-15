package com.esame.EsameReact_be.repository;

import com.esame.EsameReact_be.entity.AuthAccount;
import com.esame.EsameReact_be.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

    Optional<AuthAccount> findByUtenteAndProvider(Utente utente, String provider);

    Optional<AuthAccount> findByProviderAndProviderAccountId(String provider, String providerAccountId);
}
