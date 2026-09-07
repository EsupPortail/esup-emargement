package org.esupportail.emargement.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Help;
import org.esupportail.emargement.repositories.HelpRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class HelpService {
	
	@Autowired
	HelpRepository helpRepository;
	
	@Autowired
    private MessageSource messageSource;
	
	@Resource
	LogService logService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private Properties getHelpProperties() {
		Properties properties = new Properties();
		try (InputStream input = HelpService.class.getClassLoader()
				.getResourceAsStream("messages.properties")) {
			if (input == null) {
				log.error("Impossible de trouver messages.properties");
				return null;
			}
			properties.load(input);
		} catch (IOException e) {
			log.error("Erreur lors du chargement de messages.properties", e);
			return null;
		}
		return properties;
	}
	
	public List<String> getHelpCategories() {
		return getHelpProperties().stringPropertyNames().stream()
				.filter(key -> key.startsWith("help."))
				.filter(key -> key.endsWith(".version"))
				.map(key -> key.substring(
						"help.".length(),
						key.length() - ".version".length()
				))
				.sorted()
				.collect(Collectors.toList());
	}
	
	public Help getValueOfKey(String key) {
		Help help= null;
		if(!helpRepository.findByKey(key).isEmpty()){
			help = helpRepository.findByKey(key).get(0);
		}
		return help;
	}
	
	public int updateHelpList() {
		int nb = 0;
		Properties properties = new Properties();
		try (InputStream input = HelpService.class.getClassLoader()
				.getResourceAsStream("messages.properties")) {
			if (input == null) {
				log.error("Impossible de trouver messages.properties");
				return 0;
			}
			properties.load(input);
		} catch (IOException e) {
			log.error("Erreur lors du chargement de messages.properties", e);
			return 0;
		}
		for (String propertyKey : properties.stringPropertyNames()) {

			/*
			 * On ne traite que les propriétés :
			 * help.<clé>.version
			 */
			if (!propertyKey.startsWith("help.") || !propertyKey.endsWith(".version")) {
				continue;
			}
			String versionValue = properties.getProperty(propertyKey);
			if (StringUtils.isBlank(versionValue)) {
				continue;
			}
			int proposedVersion;
			try {
				proposedVersion = Integer.parseInt(versionValue.trim());
			} catch (NumberFormatException e) {
				log.warn("Version d'aide invalide pour {} : {}", propertyKey, versionValue);
				continue;
			}
			/*
			 * Une version 0 dans properties signifie :
			 * ne pas synchroniser cette aide.
			 */
			if (proposedVersion == 0) {
				continue;
			}
			/*
			 * help.sessionEpreuve.version
			 *              ↓
			 * sessionEpreuve
			 */
			String helpKey = propertyKey.substring(
					"help.".length(),
					propertyKey.length() - ".version".length()
			);
			List<Help> list = helpRepository.findByKey(helpKey);
			if (list.isEmpty()) {
				log.warn("Aucune aide trouvée en base pour la clé {}", helpKey);
				continue;
			}
			Help help = list.get(0);
			Integer currentVersion = help.getVersion();
			/*
			 * On initialise / met à jour si :
			 * - version BDD = null
			 * - version BDD = 0
			 * - version BDD < version proposée
			 *
			 * Sinon, aucune modification.
			 */
			if (currentVersion != null && currentVersion >= proposedVersion) {
				continue;
			}
			help.setTitle(
					properties.getProperty("help." + helpKey + ".title")
			);
			help.setDescription(
					properties.getProperty("help." + helpKey + ".description")
			);
			help.setIntro(
					properties.getProperty("help." + helpKey + ".intro")
			);
			help.setActions(
					properties.getProperty("help." + helpKey + ".actions")
			);

			help.setImportant(
					properties.getProperty("help." + helpKey + ".important")
			);
			help.setWarning(
					properties.getProperty("help." + helpKey + ".warning")
			);
			help.setVersion(proposedVersion);
			help.setDateModification(new Date());
			helpRepository.save(help);
			nb++;
			log.info("Mise à jour de l'aide {} : version {} -> {}", helpKey, currentVersion, proposedVersion);

			logService.log(ACTION.UPDATE_HELP, RETCODE.SUCCESS,
					"Mise à jour de l'aide " + helpKey + " : version " + currentVersion + " -> " + proposedVersion,
					null, null, "all", null);
		}

		return nb;
	}

	public String getHelpProperty(String word, String keyConfig) {
		String keyConfigLowerCase = keyConfig.toLowerCase();
		String concatWord = (".").concat(word).concat(".");
		return messageSource.getMessage(
				"help.".concat(keyConfigLowerCase).concat(concatWord).concat(keyConfigLowerCase), null, null);
	}
	
	public int addMissingHelp() {
		int nb = 0;
		Properties properties = getHelpProperties();
		if (properties == null) {
			return 0;
		}
		// Clés déjà présentes en base
		Set<String> currentKeys = helpRepository.findAll().stream().map(Help::getKey).collect(Collectors.toSet());

		for (String propertyKey : properties.stringPropertyNames()) {
			/*
			 * On ne traite que : help.<clé>.version
			 */
			if (!propertyKey.startsWith("help.") || !propertyKey.endsWith(".version")) {
				continue;
			}
			String versionValue = properties.getProperty(propertyKey);
			if (StringUtils.isBlank(versionValue)) {
				continue;
			}
			int version;
			try {
				version = Integer.parseInt(versionValue.trim());
			} catch (NumberFormatException e) {
				log.warn("Version d'aide invalide pour {} : {}", propertyKey, versionValue);
				continue;
			}
			// Version 0 = aide désactivée / non initialisée
			if (version == 0) {
				continue;
			}
			/*
			 * help.sessionEpreuve.version ↓ sessionEpreuve
			 */
			String helpKey = propertyKey.substring("help.".length(), propertyKey.length() - ".version".length());

			if (currentKeys.contains(helpKey)) {
				continue;
			}
			Help help = new Help();
			help.setKey(helpKey);
			help.setTitle(properties.getProperty("help." + helpKey + ".title"));
			help.setDescription(properties.getProperty("help." + helpKey + ".description"));
			help.setIntro(properties.getProperty("help." + helpKey + ".intro"));
			help.setActions(properties.getProperty("help." + helpKey + ".actions"));
			help.setImportant(properties.getProperty("help." + helpKey + ".important"));
			help.setWarning(properties.getProperty("help." + helpKey + ".warning"));
			help.setVersion(version);
			help.setDateModification(new Date());
			helpRepository.save(help);
			nb++;

			log.info("Nouvelle rubrique d'aide ajoutée : {}", helpKey);
			logService.log(ACTION.CREATE_HELP, RETCODE.SUCCESS, "Création de l'aide " + helpKey, null, null, "all",
					null);
		}
		return nb;
	}

	public int cleanObsoleteHelp() {
		int nb = 0;
		Set<String> propertyKeys = getHelpCategories().stream().collect(Collectors.toSet());
		List<Help> helps = helpRepository.findAll();
		for (Help help : helps) {
			if (!propertyKeys.contains(help.getKey())) {
				log.info("Suppression de la rubrique d'aide obsolète : {}", help.getKey());
				logService.log(ACTION.DELETE_HELP, RETCODE.SUCCESS, "Suppression de l'aide obsolète " + help.getKey(),
						null, null, "all", null);
				helpRepository.delete(help);
				nb++;
			}
		}
		return nb;
	}
}
