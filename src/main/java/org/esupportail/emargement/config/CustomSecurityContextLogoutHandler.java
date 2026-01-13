package org.esupportail.emargement.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomSecurityContextLogoutHandler extends SecurityContextLogoutHandler {

	
    private final SessionRegistry sessionRegistry;

    public CustomSecurityContextLogoutHandler(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Invalidate the session
        HttpSession session = request.getSession(false);
        if (session != null) {
            // Remove the session from SessionRegistry
            sessionRegistry.removeSessionInformation(session.getId());

            // Invalidate the session
            session.invalidate();
        }

        // Perform the rest of the logout logic (e.g., clearing the SecurityContext)
        super.logout(request, response, authentication);
    }
}
