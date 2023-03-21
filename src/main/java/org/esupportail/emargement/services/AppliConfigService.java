package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.AppliConfig.TypeConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class AppliConfigService {
	
	@Autowired
	AppliConfigRepository appliConfigRepository;
	
	@Autowired
    private MessageSource messageSource;
	
	@Resource
	LogService logService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final String DELIMITER_MULTIPLE_VALUES = ",";

	enum AppliConfigKey {
		CONVOC_TYPE, CONVOC_SUJET_MAIL, CONVOC_BODY_MAIL, CONSIGNE_TYPE, CONSIGNES_ENABLED,
		CONSIGNE_SUJET_MAIL, CONSIGNE_BODY_MAIL, LISTE_GESTIONNAIRES, AUTO_CLOSE_SESSION, SEND_EMAILS, TEST_EMAIL, RETENTION_LOGS,
		PROCURATION_MAX, CONVOC_ENABLED, EMAIL_LINK_EMARGER, EMAIL_SUJET_LINK_EMARGER, QRCODE_SUJET_MAIL,
		QRCODE_BODY_MAIL, ENABLE_QRCODE, ENABLE_EMARGER_LINK, ENABLE_PHOTO_ESUPNFCTAG, ENABLE_USER_QRCODE, BEFORE_START_EMARGER_LINK,
		ADE_CATEGORIES, ADE_ENABLED, ESUPSIGNATURE_ENABLED, ESUPSIGNATURE_EMAILS
	}
	
	public List<String> getTypes() {
		return Arrays.asList(new String[] {TypeConfig.HTML.name(), TypeConfig.TEXT.name(), TypeConfig.BOOLEAN.name()});
	}
	
	protected AppliConfig getAppliConfigByKey(AppliConfigKey appliConfigKey) {
		if(!appliConfigRepository.findAppliConfigByKey(appliConfigKey.name()).isEmpty()) {
			return	appliConfigRepository.findAppliConfigByKey(appliConfigKey.name()).get(0);
		}else {
			return null;
		}
	}
	
	protected AppliConfig getAppliConfigByKeyAndContext(AppliConfigKey appliConfigKey, Context context) {
		if(!appliConfigRepository.findAppliConfigByKeyAndContext(appliConfigKey.name(), context).isEmpty()) {
			return	appliConfigRepository.findAppliConfigByKeyAndContext(appliConfigKey.name(), context).get(0);
		}else {
			return null;
		}
	}
	
	private List<String> splitConfigValues(AppliConfig appliConfig) {
		String userTypeAsString = appliConfig.getValue();
		List<String> userTypes = new ArrayList<String>();
		if(userTypeAsString.contains(DELIMITER_MULTIPLE_VALUES)) {
			userTypes = Arrays.asList(userTypeAsString.split(DELIMITER_MULTIPLE_VALUES));
		}else {
			userTypes.add(userTypeAsString);
		}
		return userTypes;
	}
	
	public String getConvocationContenu() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.CONVOC_TYPE);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public String getConvocationSujetMail() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.CONVOC_SUJET_MAIL);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public String getConvocationBodyMail() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.CONVOC_BODY_MAIL);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public String getConsigneType() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.CONSIGNE_TYPE);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public String getConsigneSujetMail() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.CONSIGNE_SUJET_MAIL);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public String getConsigneBodyMail() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.CONSIGNE_BODY_MAIL);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public  List<String> getListeGestionnaires() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.LISTE_GESTIONNAIRES);
		return splitConfigValues(appliConfig);
	}

	public Boolean getAutoCloseSession() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.AUTO_CLOSE_SESSION);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public String getTestEmail() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.TEST_EMAIL);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public Boolean isSendEmails() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.SEND_EMAILS);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public int getRetentionLogs(Context context) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.RETENTION_LOGS, context);
		return appliConfig==null ? 36000 : Integer.parseInt(appliConfig.getValue());
	}
	
	public int getMaxProcurations() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.PROCURATION_MAX);
		return appliConfig==null ? 2 : Integer.parseInt(appliConfig.getValue());
	}
	
	public Boolean isConvocationEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.CONVOC_ENABLED);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public String getLinkEmailEmarger() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.EMAIL_LINK_EMARGER);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public String getLinkSujetEmailEmarger() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.EMAIL_SUJET_LINK_EMARGER);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public String getQrCodeSujetMail() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.QRCODE_SUJET_MAIL);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public String getQrCodebodyMail() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.QRCODE_BODY_MAIL);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public Boolean isQrCodeEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ENABLE_QRCODE);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isSendLinkEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ENABLE_EMARGER_LINK);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isPhotoEsupNfcTagEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ENABLE_PHOTO_ESUPNFCTAG);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isUserQrCodeEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ENABLE_USER_QRCODE);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean beforeStartEmargerLink() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.BEFORE_START_EMARGER_LINK);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isConsignesEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.CONSIGNES_ENABLED);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isAdeCampusEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_ENABLED);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public  List<String> getCategoriesAde() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_CATEGORIES);
		return splitConfigValues(appliConfig);
	}
	
	public Boolean isEsupSignatureEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ESUPSIGNATURE_ENABLED);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public  List<String> getEsupSignatureEmails() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ESUPSIGNATURE_EMAILS);
		return splitConfigValues(appliConfig);
	}

	public List <AppliConfigKey> checkAppliconfig(Context context) {
		List <AppliConfigKey> listKey = Arrays.asList(AppliConfigKey.values());
		List <AppliConfig> list = appliConfigRepository.findAppliConfigByContext(context);
		List <String> currentKeys = list.stream().map(o -> o.getKey()).collect(Collectors.toList());
		
		List <AppliConfigKey> newListKey = new ArrayList<AppliConfigKey>();
		
		for (AppliConfigKey key : listKey){
			if(!currentKeys.contains(key.name())) {
				newListKey.add(key);
				log.info("config manquante: " + key);
			}
		}
		return newListKey;
	}
	
	@Transactional
	public int updateAppliconfig(Context context) {
		List <AppliConfigKey> list =  checkAppliconfig(context);
		int nb= 0;
		if(!list.isEmpty()) {
			for(AppliConfigKey key : list) {
				AppliConfig appliconfig = new AppliConfig();
				String suffixe = key.name().toLowerCase();
				appliconfig.setContext(context);
				appliconfig.setDescription(messageSource.getMessage("config.desc.".concat(suffixe), null, null));
				appliconfig.setKey(messageSource.getMessage("config.key.".concat(suffixe), null, null));
				appliconfig.setType(TypeConfig.valueOf(messageSource.getMessage("config.type.".concat(suffixe), null, null)));
				appliconfig.setValue(messageSource.getMessage("config.value.".concat(suffixe), null, null));
				appliConfigRepository.save(appliconfig);
				nb++;
			}
			log.info("Ajout des configs " +list + " pour le contexte : " + context.getKey());
			logService.log(ACTION.AJOUT_CONFIG, RETCODE.SUCCESS, "Ajout de configs " + list , null,  null, context.getKey(), null);
		}
		return nb;
	}
}
