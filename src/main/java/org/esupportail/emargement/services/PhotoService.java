package org.esupportail.emargement.services;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

    private final RestTemplate restTemplate;

	@Value("${emargement.wsrest.photo.prefixe}")
	private String photoPrefixe;
	
	@Value("${emargement.wsrest.photo.suffixe}")
	private String photoSuffixe;

    private byte[] defaultPhoto;

    public PhotoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void init() throws IOException {
        defaultPhoto = IOUtils.toByteArray(new ClassPathResource("nophoto.png").getInputStream());
    }

    @Cacheable(value = "photosCache", key = "#eppn")
    public byte[] getPhoto(String eppn) {
        if ("inconnu".equals(eppn)) {
            return defaultPhoto;
        }

        String uri = photoPrefixe.concat(eppn).concat(photoSuffixe);
        try {
            byte[] photo = restTemplate.getForObject(uri, byte[].class);
            return (photo != null) ? photo : defaultPhoto;
        } catch (RestClientException e) {
            log.warn("Erreur récupération photo pour eppn={}, photo par défaut non mise en cache", eppn, e);
            throw e; // relancée pour éviter la mise en cache par @Cacheable
        }
    }

    public byte[] getDefaultPhoto() {
        return defaultPhoto;
    }
}