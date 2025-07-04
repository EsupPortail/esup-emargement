package org.esupportail.emargement.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.Resource;

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
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
		QRCODE_BODY_MAIL, ENABLE_QRCODE, ENABLE_EMARGER_LINK, ENABLE_PHOTO_ESUPNFCTAG, ENABLE_USER_QRCODE, ENABLE_SESSION_QRCODE, ENABLE_CARD_QRCODE,
		BEFORE_START_EMARGER_LINK, ADE_SURVEILLANT, ADE_PROJET, ADE_CATEGORIES, ADE_IMPORT_MEMBERS, ADE_ENABLED, ADE_CREATE_GROUPE_AUTO, ESUPSIGNATURE_ENABLED, ESUPSIGNATURE_EMAILS, 
		ATTESTATION_TEXTE, TRI_BADGEAGE_ALPHA, QRCODE_CHANGE, DISPLAY_TAGCHECKER, SCROLL_TOP, DISPLAY_CALENDAR, ADE_MEMBER_ATTRIBUTE, DISPLAY_IMPORTEXPORT,
		LIST_IMPORTEXPORT, SURVEILLANT_TERM, ENABLE_PARTICIPANT, ENABLE_COMMUNICATION, ADE_UPDATE_CAPACITE_SALLE, ADE_LIMIT_QUERIES, ADE_SUPERGROUPE,ADE_FORMATION,
		ADE_INSTRUCTOR_MANAGER
	}
	
	public List<String> getTypes() {
		return Arrays.asList(new String[] {TypeConfig.HTML.name(), TypeConfig.TEXT.name(), TypeConfig.BOOLEAN.name()});
	}
	
	protected AppliConfig getAppliConfigByKey(AppliConfigKey appliConfigKey) {
		if(!appliConfigRepository.findAppliConfigByKey(appliConfigKey.name()).isEmpty()) {
			return	appliConfigRepository.findAppliConfigByKey(appliConfigKey.name()).get(0);
		}
		return null;
	}
	
	protected AppliConfig getAppliConfigByKeyAndContext(AppliConfigKey appliConfigKey, Context context) {
		if(!appliConfigRepository.findAppliConfigByKeyAndContext(appliConfigKey.name(), context).isEmpty()) {
			return	appliConfigRepository.findAppliConfigByKeyAndContext(appliConfigKey.name(), context).get(0);
		}
		return null;
	}
	
	private List<String> splitConfigValues(AppliConfig appliConfig) {
		String userTypeAsString = appliConfig.getValue();
		List<String> userTypes = new ArrayList<>();
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
	
	public Boolean isCardQrCodeEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ENABLE_CARD_QRCODE);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isSessionQrCodeEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ENABLE_SESSION_QRCODE);
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
	
	public Boolean isAdeCampusGroupeAutoEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_CREATE_GROUPE_AUTO);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isAdeCampusGroupeAutoEnabled(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_CREATE_GROUPE_AUTO, ctx);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isMembersAdeImport() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_IMPORT_MEMBERS);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isMembersAdeImport(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_IMPORT_MEMBERS, ctx);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public String getCategoriesAde(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_CATEGORIES, ctx);
		if(appliConfig==null) {
			return "";
		}
		if(appliConfig.getValue().contains(",")){
			String splitConfig [] = appliConfig.getValue().split(",");
			return  splitConfig[0].trim();
		}
		return appliConfig.getValue();
	}
	
	public  String getProjetAde() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_PROJET);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public Boolean isEsupSignatureEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ESUPSIGNATURE_ENABLED);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public  List<String> getEsupSignatureEmails() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ESUPSIGNATURE_EMAILS);
		return splitConfigValues(appliConfig);
	}
	
	public String getAttestationTexte() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ATTESTATION_TEXTE);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public Boolean isBadgeageSortAlpha() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.TRI_BADGEAGE_ALPHA);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public String getQrCodeChange() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.QRCODE_CHANGE);
		return appliConfig==null ? "5" : appliConfig.getValue();
	}
	
	public Boolean isTagCheckerDisplayed() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.DISPLAY_TAGCHECKER);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isScrollTopEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.SCROLL_TOP);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isAdeCampusSurveillantEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_SURVEILLANT);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isCalendarDisplayed() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.DISPLAY_CALENDAR);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isImportExportDisplayed() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.DISPLAY_IMPORTEXPORT);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public  String getAdeMemberAttribute() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_MEMBER_ATTRIBUTE);
		return appliConfig==null ? "code" : appliConfig.getValue().trim();
	}
	
	public  String getAdeMemberAttribute(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_MEMBER_ATTRIBUTE, ctx);
		return appliConfig==null ? "code" : appliConfig.getValue().trim();
	}
	
	public String getListImportExport() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.LIST_IMPORTEXPORT);
		return appliConfig==null ? "ALCG" : appliConfig.getValue();
	}
	
	public String getSurveillantTerm() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.SURVEILLANT_TERM);
		return appliConfig==null ? "Surveillant" : appliConfig.getValue();
	}
	
	public Long checkCategory(Context context, String key) {
		return appliConfigRepository.countByContextAndKeyAndCategory(context, key, null);
	}
	
	public Boolean isParticipantDisplayed() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ENABLE_PARTICIPANT);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isCommunicationDisplayed() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ENABLE_COMMUNICATION);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isAdeCampusUpdateCapaciteSalleEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_UPDATE_CAPACITE_SALLE);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isAdeCampusUpdateCapaciteSalleEnabled(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_UPDATE_CAPACITE_SALLE, ctx);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}

	public Boolean isAdeCampusLimitQueriesEnabled() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_LIMIT_QUERIES);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public Boolean isAdeCampusLimitQueriesEnabled(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_LIMIT_QUERIES, ctx);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}
	
	public  String getAdeSuperGroupe() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_SUPERGROUPE);
		return appliConfig==null ? "" : appliConfig.getValue().trim();
	}
	
	public  String getAdeSuperGroupe(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_SUPERGROUPE, ctx);
		return appliConfig==null ? "" : appliConfig.getValue().trim();
	}
	
	public  String getFormationAde() {
		AppliConfig appliConfig = getAppliConfigByKey(AppliConfigKey.ADE_FORMATION);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public  String getFormationAde(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_FORMATION, ctx);
		return appliConfig==null ? "" : appliConfig.getValue();
	}
	
	public Boolean isAdeCampusInstructorManager(Context ctx) {
		AppliConfig appliConfig = getAppliConfigByKeyAndContext(AppliConfigKey.ADE_INSTRUCTOR_MANAGER, ctx);
		return appliConfig!=null && "true".equalsIgnoreCase(appliConfig.getValue());	
	}

	public List <AppliConfigKey> checkAppliconfig(Context context) {
		List <AppliConfigKey> listKey = Arrays.asList(AppliConfigKey.values());
		List <AppliConfig> list = appliConfigRepository.findAppliConfigByContext(context);
		List <String> currentKeys = list.stream().map(o -> o.getKey()).collect(Collectors.toList());
		
		List <AppliConfigKey> newListKey = new ArrayList<>();
		
		for (AppliConfigKey key : listKey){
			if(!currentKeys.contains(key.name())) {
				newListKey.add(key);
				log.info("config manquante: " + key);
			}
		}
		return newListKey;
	}
	
	@Transactional
	public int updateCatIsMissing(Context context) {
		List<AppliConfig> list = appliConfigRepository.findAppliConfigByContextAndCategoryIsNull(context);
		int nb= 0;
		if(!list.isEmpty()) {
			for(AppliConfig appliconfig : list) {
				appliconfig.setCategory(messageSource.getMessage("config.cat.".concat(appliconfig.getKey().toLowerCase()), null, null));
				appliConfigRepository.save(appliconfig);
				nb++;
			}
			log.info("Mise à jour des configs " +list + " pour le contexte : " + context.getKey());
			logService.log(ACTION.UPDATE_CONFIG, RETCODE.SUCCESS, "Mise à jour de configs " + list , null,  null, context.getKey(), null);
		}
		return nb;
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
				appliconfig.setCategory(messageSource.getMessage("config.cat.".concat(suffixe), null, null));
				appliconfig.setDescription(messageSource.getMessage("config.desc.".concat(suffixe), null, null));
				appliconfig.setKey(messageSource.getMessage("config.key.".concat(suffixe), null, null));
				TypeConfig typeConfig = TypeConfig.valueOf(messageSource.getMessage("config.type.".concat(suffixe), null, null));
				appliconfig.setType(typeConfig);
				String value;
				try {
				    value = messageSource.getMessage("config.value.".concat(suffixe), null, null);
				} catch (NoSuchMessageException e) {
					if (TypeConfig.BOOLEAN.equals(typeConfig)) {
						value = "false";
					}else {
						value = "";
					}
					log.info("Valeur vide pour la configuration, on met une valeur par défaut (" + suffixe + ")", e);
				}
				appliconfig.setValue(value);
				appliConfigRepository.save(appliconfig);
				nb++;
			}
			log.info("Ajout des configs " +list + " pour le contexte : " + context.getKey());
			logService.log(ACTION.AJOUT_CONFIG, RETCODE.SUCCESS, "Ajout de configs " + list , null,  null, context.getKey(), null);
		}
		Long test = checkCategory(context, "ADE_ENABLED");
		if(test>0) {
			List <AppliConfig> updateList = appliConfigRepository.findAppliConfigByContext(context);
			for(AppliConfig app : updateList) {
				String suffixe = app.getKey().toLowerCase();
				app.setCategory(messageSource.getMessage("config.cat.".concat(suffixe), null, null));
				nb++;
			}
			log.info("Modification des configs " +list + " pour le contexte : " + context.getKey());
			logService.log(ACTION.UPDATE_CONFIG, RETCODE.SUCCESS, "Modification de configs : catégories " , null,  null, context.getKey(), null);
		}
		
		return nb;
	}
	
	@Transactional
	public int updateDescription(Context context) {
		int nb= 0;
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");

        Properties properties = new Properties();
        try (InputStream input = AppliConfigService.class.getClassLoader().getResourceAsStream("messages.properties")) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Map<Object, Object> filteredMessages = properties.entrySet().stream()
                .filter(entry -> entry.getKey().toString().startsWith("config.version"))
                .filter(entry -> !"0".equals(entry.getValue().toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<Object, Object> entry : filteredMessages.entrySet()) {
        	Object key = entry.getKey();
        	Object value = entry.getValue();
            String keyConfig = key.toString().substring(key.toString().lastIndexOf('.') + 1).toUpperCase();
            List<AppliConfig> list = appliConfigRepository.findAppliConfigByKeyAndContext(keyConfig, context);
            if(!list.isEmpty()) {
            	AppliConfig appliconfig = list.get(0);
            	if(appliconfig.getVersion() == null || appliconfig.getVersion()<Integer.valueOf(value.toString())){
            		appliconfig.setDescription(messageSource.getMessage("config.desc.".concat(keyConfig.toLowerCase()), null, null));
            		appliconfig.setVersion(Integer.valueOf(value.toString()));
            		appliConfigRepository.save(appliconfig);
            		log.info("Modification description config " + appliconfig.getKey() + " pour le contexte : " + context.getKey());
            		logService.log(ACTION.UPDATE_CONFIG, RETCODE.SUCCESS, "Modification de configs : description" + appliconfig.getKey(), null,  null, context.getKey(), null);
            	}
            }
        }
        
		return nb;
	}
	
}
