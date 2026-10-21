package org.esupportail.emargement.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.services.AbsenceEtudiantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

// Le dépassement est détecté avant le choix du contrôleur : seul un résolveur global peut le traiter.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MaxUploadSizeExceptionResolver implements HandlerExceptionResolver {

	private static final Pattern DECLARATION_ABSENCE_URI = Pattern.compile("^/([^/]+)/user/absence$");

	@Autowired
	AbsenceEtudiantService absenceEtudiantService;

	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		if(!(ex instanceof MaxUploadSizeExceededException) || !"POST".equals(request.getMethod())) {
			return null;
		}
		Matcher matcher = DECLARATION_ABSENCE_URI.matcher(request.getRequestURI().substring(request.getContextPath().length()));
		if(!matcher.matches()) {
			return null;
		}
		String taille = absenceEtudiantService.getTailleMaxFichierLibelle();
		RequestContextUtils.getOutputFlashMap(request).put("absenceDeclareError",
				"Un justificatif dépasse la taille maximale autorisée" + (taille.isEmpty() ? "." : " (" + taille + " par fichier)."));
		return new ModelAndView(new RedirectView("/" + matcher.group(1) + "/user", true));
	}
}
