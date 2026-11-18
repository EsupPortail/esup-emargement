package org.esupportail.emargement.services;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.esupportail.emargement.domain.LdapUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Récupération de la photo d'un étudiant depuis les API Pégase (Pièces / Inscriptions).
 *
 * Même principe que le RestUserInfoService d'esup-sgc :
 * <ol>
 *   <li>authentification : ticket CAS Pégase (token=true) utilisé en Bearer ;</li>
 *   <li>API inscription : retrouve l'inscription principale de la période (code cible  code formation) ;</li>
 *   <li>API pièces : photo (binaire) pour codeApprenant / codePeriode / codeChemin (formation&gt;cible).</li>
 * </ol>
 * Le codeApprenant est le supannEtuId, lu dans le LDAP à partir de l'eppn.
 */
@Service
public class PegasePhotoService {

    private static final Logger log = LoggerFactory.getLogger(PegasePhotoService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LdapService ldapService;

    @Value("${emargement.photo.pegase.token-endpoint:}")
    private String tokenEndpoint;

    @Value("${emargement.photo.pegase.username:}")
    private String username;

    @Value("${emargement.photo.pegase.password:}")
    private String password;

    /** Durée de vie du token conservé en mémoire, en minutes. */
    @Value("${emargement.photo.pegase.token-ttl-minutes:10}")
    private long tokenTtlMinutes;

    /** Variables : {etab}, {etu}. */
    @Value("${emargement.photo.pegase.inscription-url:}")
    private String inscriptionUrl;

    /** Variables : {etab}, {etu}, {periode}, {chemin}. */
    @Value("${emargement.photo.pegase.photo-url:}")
    private String photoUrl;

    @Value("${emargement.photo.pegase.etablissement:}")
    private String etablissement;

    /** Code de période (ex. 2026). Vide : année universitaire en cours. */
    @Value("${emargement.photo.pegase.periode:}")
    private String periodeConfig;

    private String token;
    private long tokenExpiry;

    public PegasePhotoService(RestTemplate restTemplate, ObjectMapper objectMapper, LdapService ldapService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.ldapService = ldapService;
    }

    /**
     * @return les octets de la photo, ou null si l'utilisateur n'a pas de photo dans Pégase
     *         (pas de supannEtuId, pas d'inscription principale, photo absente).
     * @throws RestClientException en cas d'erreur technique (Pégase indisponible, authentification refusée...)
     */
    public byte[] getPhoto(String eppn) {
        String numEtu = getNumEtudiant(eppn);
        if (numEtu == null) {
            log.debug("Pégase : pas de supannEtuId pour {}", eppn);
            return null;
        }
        String periode = getPeriode();
        String chemin = getCodeChemin(numEtu, periode);
        if (chemin == null) {
            log.debug("Pégase : pas d'inscription principale en {} pour {}", periode, eppn);
            return null;
        }
        try {
			String url = photoUrl.replace("{etab}", etablissement)
					 .replace("{supannEtuId}", numEtu)
					 .replace("{periode}", periode)
					 .replace("{chemin}", chemin);
            return getWithToken(url, byte[].class, MediaType.ALL, etablissement, numEtu, periode, chemin);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Pégase : photo introuvable pour {}", eppn);
            return null;
        }
    }

    private String getNumEtudiant(String eppn) {
        List<LdapUser> users = ldapService.getUsers(eppn);
        if (users == null || users.isEmpty()) {
            return null;
        }
        String numEtu = users.get(0).getNumEtudiant();
        return (numEtu == null || numEtu.trim().isEmpty()) ? null : numEtu.trim();
    }

    /** Inscription principale de la période -> "codeFormation>codeCible", ou null. */
    private String getCodeChemin(String numEtu, String periode) {
        String json;
        try {
        	String url = inscriptionUrl.replace("{etab}", etablissement).replace("{etu}", numEtu);
            json = getWithToken(url, String.class, MediaType.APPLICATION_JSON, etablissement, numEtu);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
        if (json == null) {
            return null;
        }
        try {
            JsonNode inscriptions = objectMapper.readTree(json).path("inscriptions");
            for (JsonNode inscription : inscriptions) {
                JsonNode cible = inscription.path("cible");
                if (inscription.path("principale").asBoolean(false)
                        && periode.equals(cible.path("periode").path("code").asText())) {
                    String codeCible = cible.path("code").asText(null);
                    String codeFormation = cible.path("formation").path("code").asText(null);
                    if (codeCible != null && codeFormation != null) {
                        return codeFormation + ">" + codeCible;
                    }
                }
            }
        } catch (IOException e) {
            throw new RestClientException("Réponse d'inscription Pégase illisible", e);
        }
        return null;
    }

    private String getPeriode() {
        if (periodeConfig != null && !periodeConfig.trim().isEmpty()) {
            return periodeConfig.trim();
        }
        LocalDate now = LocalDate.now();
        // l'année universitaire démarre en septembre
        return String.valueOf(now.getMonthValue() >= 9 ? now.getYear() : now.getYear() - 1);
    }

    /** GET authentifié ; en cas de 401, le token est renouvelé et l'appel rejoué une fois. */
    private <T> T getWithToken(String urlTemplate, Class<T> type, MediaType accept, Object... vars) {
        try {
            return exchange(urlTemplate, type, accept, getToken(false), vars);
        } catch (HttpClientErrorException.Unauthorized e) {
            return exchange(urlTemplate, type, accept, getToken(true), vars);
        }
    }

    private <T> T exchange(String urlTemplate, Class<T> type, MediaType accept, String bearer, Object... vars) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        headers.setAccept(Collections.singletonList(accept));
        ResponseEntity<T> response = restTemplate.exchange(urlTemplate, HttpMethod.GET,
                new HttpEntity<Void>(headers), type, vars);
        return response.getBody();
    }

    private synchronized String getToken(boolean forceRefresh) {
        long now = System.currentTimeMillis();
        if (!forceRefresh && token != null && now < tokenExpiry) {
            return token;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);
        body.add("token", "true");
        String result = restTemplate.postForObject(tokenEndpoint, new HttpEntity<>(body, headers), String.class);
        if (result == null || result.trim().isEmpty()) {
            throw new RestClientException("Pégase : token vide");
        }
        token = result.trim();
        tokenExpiry = now + tokenTtlMinutes * 60_000L;
        return token;
    }
}
