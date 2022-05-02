package org.esupportail.emargement.security;

import org.esupportail.emargement.services.UserAppService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AuthenticationSuccessEventHandler {

    @Resource
    UserAppService userAppService;

    @EventListener({AuthenticationSuccessEvent.class, InteractiveAuthenticationSuccessEvent.class})
    public void processAuthenticationSuccessEvent(AbstractAuthenticationEvent e) {
        Authentication authentication = e.getAuthentication();
        userAppService.setDateConnexion(authentication.getName());
    }
}
