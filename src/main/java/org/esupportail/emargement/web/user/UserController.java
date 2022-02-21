package org.esupportail.emargement.web.user;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.UserService;
import org.esupportail.emargement.utils.ToolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.zxing.WriterException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor() or  @userAppService.isUser()")
public class UserController {
	
	@Autowired
	UserService userService;
	
	@Autowired
	HelpService helpService;
	
	@Resource
	LogService logService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Autowired
    LdapUserRepository ldapUserRepository;

	@Autowired
	LdapService ldapService;

	@Autowired	
	TagCheckRepository tagCheckRepository;
	
	@Autowired	
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired	
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	ToolUtil toolUtil;
	
	private final static String ITEM = "user";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/user")
	public String list(@PathVariable String emargementContext, Model model, @PageableDefault(size = 20, direction = Direction.DESC, sort = "sessionEpreuve.dateExamen")  Pageable pageable, 
			@RequestParam(value="sessionToken", required = false) String sessionToken) throws ParseException{
		if(sessionToken!=null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			boolean isAlreadyBadged = false;
			boolean isSessionExpired = true;
			boolean isHourOk = false;
			if(auth != null) {
				String eppn = auth.getName();
				TagCheck tc = tagCheckRepository.findTagCheckBysessionTokenEqualsAndPersonEppnEquals(sessionToken, eppn);
				model.addAttribute("se", tc.getSessionEpreuve());
				if(tc.getSessionLocationBadged()!=null) {
					isAlreadyBadged = true;
				}
				LocalTime now = LocalTime.now();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				Date date = dateFormat.parse(dateFormat.format(new Date()));
				Long verifySe = null;
				Long seId = tc.getSessionEpreuve().getId();
				Date dateFin = tc.getSessionEpreuve().getDateFin();
				if(appliConfigService.beforeStartEmargerLink()) {
					if(dateFin != null) {
						verifySe = sessionEpreuveRepository.checkIsBeforeSessionWithDateFin(eppn, date, date, now, seId);
					}else {
						verifySe = sessionEpreuveRepository.checkIsBeforeSession(eppn, date, now, seId);
					}
				}else {
					if(dateFin != null) {
						verifySe = sessionEpreuveRepository.checkIsBeforeFinWithDateFin(eppn, date, date, now, seId);
					}else {
						verifySe = sessionEpreuveRepository.checkIsBeforeFin(eppn, date, now, seId);
					}
				}
				
				if(verifySe >0) {
					isSessionExpired = false;
				}else {
					Long check = null;
					if(dateFin != null) {
						check = sessionEpreuveRepository.checkIsBeforeConvocationWithDateFin(eppn, date, date, now, seId);
					}else {
						check = sessionEpreuveRepository.checkIsBeforeConvocation(eppn, date, now, seId);
					}
					if(check > 0) {
						isHourOk = true;
						logService.log(ACTION.REPORT_LINK_TOO_SOON, RETCODE.SUCCESS, "Session : " + tc.getSessionEpreuve().getNomSessionEpreuve(), eppn, null, emargementContext, null);
					}else {
						logService.log(ACTION.REPORT_LINK_TOO_LATE, RETCODE.SUCCESS, "Session : " + tc.getSessionEpreuve().getNomSessionEpreuve(), eppn, null, emargementContext, null);
					}
				}
			}
			model.addAttribute("isAlReadybadged", isAlreadyBadged);
			model.addAttribute("sessionToken", sessionToken);
			model.addAttribute("isSessionExpired", isSessionExpired);
			model.addAttribute("isHourOk", isHourOk);
		}
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("tagChecksPage", userService.getTagChecks(pageable));
		model.addAttribute("today", new Date());
		model.addAttribute("isUserQrCodeEnabled", appliConfigService.isUserQrCodeEnabled());
		return "user/list";
	}
	
	@PostMapping(value = "/user/isPresent")
	public String isPresent(@PathVariable String emargementContext, @RequestParam(value="sessionToken", required = true)  
			 String sessionToken, final RedirectAttributes redirectAttributes) throws ParseException {
		
		if(sessionToken != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if(auth != null) {
				String eppn = auth.getName();
				if(tagCheckRepository.countTagCheckBysessionTokenEqualsAndPersonEppnEquals(sessionToken, eppn) == 1){
					TagCheck tc = tagCheckRepository.findTagCheckBysessionTokenEqualsAndPersonEppnEquals(sessionToken, eppn);
					//On regarde l'heure
					LocalTime now = LocalTime.now();
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					Date date = dateFormat.parse(dateFormat.format(new Date()));
					Long verifySe = null;
					Long seId = tc.getSessionEpreuve().getId();
					Date dateFin = tc.getSessionEpreuve().getDateFin();
					if(appliConfigService.beforeStartEmargerLink()) {
						if(dateFin != null) {
							verifySe = sessionEpreuveRepository.checkIsBeforeSessionWithDateFin(eppn, date, date, now, seId);
						}else {
							verifySe = sessionEpreuveRepository.checkIsBeforeSession(eppn, date, now, seId);
						}
					}else {
						if(dateFin != null) {
							verifySe = sessionEpreuveRepository.checkIsBeforeFinWithDateFin(eppn, date, date, now, seId);
						}else {
							verifySe = sessionEpreuveRepository.checkIsBeforeFin(eppn, date, now, seId);
						}
					}
					if(verifySe >0) {
						if (tc.getSessionLocationBadged()!=null) {
							redirectAttributes.addFlashAttribute("alReadyPresent", "alReadyPresent");
						}else {
							String [] splitToken = sessionToken.split("token");
							if(splitToken.length > 0) {
								TagChecker tcer  = tagCheckerRepository.findById(Long.valueOf(splitToken[0])).get();
								tc.setTagChecker(tcer);
							}
							tc.setTypeEmargement(TypeEmargement.LINK);
							tc.setTagDate(new Date());
							tc.setSessionLocationBadged(tc.getSessionLocationExpected());
							tagCheckRepository.save(tc);
							redirectAttributes.addFlashAttribute("msgTokenOk", "msgTokenOk");
						}
					}else {
						redirectAttributes.addFlashAttribute("msgTokenNotOk", "msgTokenNotOk");
					}
				}
			}
		}
		
		return String.format("redirect:/%s/user" , emargementContext);
	}
	
	@GetMapping(value = "/user/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("tc",  tagCheckRepository.findById(id).get());
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "user/show";
    }
	
	@RequestMapping(value = "/user/qrCode/{eppn}/{id}")
    @ResponseBody
    public void getQrCode(@PathVariable("eppn") String eppn, @PathVariable("id") Long id, HttpServletResponse response) throws WriterException, IOException {
        List <TagCheck> tcs =  tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(id, eppn);
        String qrCodeString = "true," + eppn + "," + id + "," + eppn + ",qrcode";
        InputStream inputStream = toolUtil.generateQRCodeImage(qrCodeString, 350, 350);
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        IOUtils.copy(inputStream, response.getOutputStream());

        IOUtils.copy(inputStream, response.getOutputStream());
    }
	
}
