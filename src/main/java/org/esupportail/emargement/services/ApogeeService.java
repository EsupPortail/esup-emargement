package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.ApogeeBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApogeeService {

	@Resource
	private JdbcTemplate apogeeJdbcTemplate;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	//Réquete 0 : Récupération de la liste des composantes 
	public List<ApogeeBean> getComposantes(){
		List<Map<String, Object>> composantes = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> abComposantes = new ArrayList<ApogeeBean>();
		
		String query = "SELECT COMPOSANTE.COD_CMP, COMPOSANTE.LIB_CMP FROM APOGEE.COMPOSANTE COMPOSANTE "
				+ "WHERE (COMPOSANTE.TEM_EN_SVE_CMP='O') ORDER BY COMPOSANTE.LIB_CMP";
		
		try {
			composantes = apogeeJdbcTemplate.queryForList(query);
			for(Map<String, Object> so : composantes) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodCmp((so.get("COD_CMP")!=null)? so.get("COD_CMP").toString(): "");				
				ab.setLibCmp((so.get("LIB_CMP")!=null)? so.get("LIB_CMP").toString(): "");
				abComposantes.add(ab);
			}
			
		} catch (Exception e) {
			log.error("Erreur lors de la récupération de la liste des composantes apogée ", e);
		}
		
		return abComposantes;
	}
	
	//Requête 1 :  Récupération de la liste des diplômes de la composante choisie en  Requete 0
	public List<ApogeeBean> getElementsPedagogiques(ApogeeBean apogeeBean){
		List<Map<String, Object>> inscrits = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> elementsPedagogiques = new ArrayList<ApogeeBean>();
		
		String query = "SELECT DISTINCT ETAPE.COD_ETP, ETAPE.LIB_ETP " + 
						"FROM APOGEE.ETAPE, APOGEE.INS_ADM_ETP " + 
						"WHERE INS_ADM_ETP.COD_ETP = ETAPE.COD_ETP " + 
						"AND INS_ADM_ETP.COD_ANU= ? " + 
						"AND INS_ADM_ETP.COD_CMP= ? " + 
						"AND ETAPE.COD_CUR In ('L','M') " +
						"ORDER BY ETAPE.LIB_ETP";
		
		try {
			inscrits = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodCmp()});
			for(Map<String, Object> so : inscrits) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodEtp((so.get("COD_ETP")!=null)? so.get("COD_ETP").toString(): "");
				ab.setLibEtp((so.get("LIB_ETP")!=null)? so.get("LIB_ETP").toString(): "");
				elementsPedagogiques.add(ab);
			}
			
		} catch (Exception e) {
			log.error("Erreur lors de la récupération de la listes des diplômes de la composante", e);
		}
		
		return elementsPedagogiques;
	}
	
	
	//Requête 2 :  Récupération des matières après avoir choisi le diplôme en Requête 1
	public List<ApogeeBean> getMatieres(ApogeeBean apogeeBean){
		List<Map<String, Object>> matieres = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> elementsPedagogiques = new ArrayList<ApogeeBean>();
		
		String query = "SELECT DISTINCT ELEMENT_PEDAGOGI.COD_ELP, ELEMENT_PEDAGOGI.LIB_ELP " +
					   "FROM APOGEE.ELEMENT_PEDAGOGI ELEMENT_PEDAGOGI, APOGEE.IND_CONTRAT_ELP IND_CONTRAT_ELP " +
					   "WHERE IND_CONTRAT_ELP.COD_ELP = ELEMENT_PEDAGOGI.COD_ELP AND ((ELEMENT_PEDAGOGI.COD_NEL LIKE 'M%' ) " +
					   "AND (IND_CONTRAT_ELP.COD_ANU= ? ) AND (IND_CONTRAT_ELP.COD_ETP= ? ) " + 
					   "OR (ELEMENT_PEDAGOGI.COD_NEL='CM') AND (IND_CONTRAT_ELP.COD_ANU= ? ) AND (IND_CONTRAT_ELP.COD_ETP= ? ) " + 
					   "OR (ELEMENT_PEDAGOGI.COD_NEL='TD') AND (IND_CONTRAT_ELP.COD_ANU= ? ) AND (IND_CONTRAT_ELP.COD_ETP= ? ))" +
					   "ORDER BY ELEMENT_PEDAGOGI.COD_ELP";
		
		try {
			matieres = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodEtp(), apogeeBean.getCodAnu(), 
																			apogeeBean.getCodEtp(), apogeeBean.getCodAnu(), apogeeBean.getCodEtp()});
			for(Map<String, Object> so : matieres) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodElp((so.get("COD_ELP")!=null)? so.get("COD_ELP").toString(): "");
				ab.setLibElp((so.get("LIB_ELP")!=null)? so.get("LIB_ELP").toString(): "");
				elementsPedagogiques.add(ab);
			}
			
		} catch (Exception e) {
			log.error("Erreur lors de la récupération des matières après avoir choisi le diplôme", e);
		}
		
		return elementsPedagogiques;
	}
	
	//Requete 3   : Comptage du nombre d’étudiants
	public int countAutorisesEpreuve(ApogeeBean apogeeBean) {
		int count = 0;
		String query = "SELECT Count(RESULTAT_ELP.COD_IND) " + 
						"FROM RESULTAT_ELP " + 
						"WHERE RESULTAT_ELP.COD_ADM='1' " + 
						"AND RESULTAT_ELP.COD_ELP= ? " + 
						"AND RESULTAT_ELP.COD_SES= ? " + 
						"AND RESULTAT_ELP.TEM_IND_CRN_ELP='CS' " + 
						"AND RESULTAT_ELP.TEM_NOT_RPT_ELP='N' " + 
						"AND RESULTAT_ELP.COD_ANU = ?";

		try {
			count =apogeeJdbcTemplate.queryForObject(
					query, new Object[] { apogeeBean.getCodElp(), apogeeBean.getCodSes(), apogeeBean.getCodAnu()}, Integer.class);
		} catch (DataAccessException e) {
			log.error("Erreur lors du comptage du nombre d'étudiants Apogée", e);
		}
		 
		return count;
					
	}
	
	//Requete 4   : Récupération de la liste étudiants
	public List<ApogeeBean> getAutorisesEpreuve(ApogeeBean apogeeBean){
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> autorisesEpreuve = new ArrayList<ApogeeBean>();
		
		String query = "SELECT INDIVIDU.COD_ETU, INDIVIDU.LIB_NOM_PAT_IND, INDIVIDU.LIB_PR1_IND, INDIVIDU.DATE_NAI_IND " + 
						"FROM INDIVIDU, RESULTAT_ELP " + 
						"WHERE RESULTAT_ELP.COD_IND = INDIVIDU.COD_IND " + 
						"AND RESULTAT_ELP.COD_ADM='1' " + 
						"AND RESULTAT_ELP.COD_ELP= ? " + 
						"AND RESULTAT_ELP.COD_SES= ? " + 
						"AND RESULTAT_ELP.TEM_IND_CRN_ELP='CS' " + 
						"AND RESULTAT_ELP.TEM_NOT_RPT_ELP='N' " + 
						"AND RESULTAT_ELP.COD_ANU = ? " +
						"ORDER BY INDIVIDU.LIB_NOM_PAT_IND";
		
		try {
			results = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodElp(), apogeeBean.getCodSes(), apogeeBean.getCodAnu()});
			for(Map<String, Object> so : results) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodAnu(apogeeBean.getCodAnu());
				ab.setCodEtp(apogeeBean.getCodEtp());
				ab.setCodElp(apogeeBean.getCodElp());				
				ab.setLibElp(apogeeBean.getLibElp());
				ab.setCodSes(apogeeBean.getCodSes());
				ab.setCodEtu((so.get("COD_ETU")!=null)? so.get("COD_ETU").toString(): "");
				ab.setLibNomPatInd((so.get("LIB_NOM_PAT_IND")!=null)? so.get("LIB_NOM_PAT_IND").toString(): "");
				ab.setLibPr1Ind((so.get("LIB_PR1_IND")!=null)? so.get("LIB_PR1_IND").toString(): "");
				ab.setDateNaiInd((so.get("DATE_NAI_IND")!=null)? so.get("DATE_NAI_IND").toString(): "");
				autorisesEpreuve.add(ab);
			}
			
		} catch (Exception e) {
			log.error("Erreur lors de la récupération de la liste étudiants", e);
		}
		
		return autorisesEpreuve;
	}
	
	//Requete 5  : Récupération  des groupes de TD d’une matière
	public List<ApogeeBean> getGroupes(ApogeeBean apogeeBean){
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> groupesTD = new ArrayList<ApogeeBean>();
		String query = "SELECT DISTINCT GROUPE.COD_EXT_GPE, GROUPE.LIB_GPE FROM APOGEE.GPE_OBJ, APOGEE.GROUPE, " +
				 		"APOGEE.IND_AFFECTE_GPE WHERE GPE_OBJ.COD_GPE = GROUPE.COD_GPE AND IND_AFFECTE_GPE.COD_GPE = GROUPE.COD_GPE " +
				 		"AND IND_AFFECTE_GPE.COD_ANU= ? AND GPE_OBJ.COD_ELP= ? ORDER BY GROUPE.COD_EXT_GPE";
		
		try {
			results = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodElp()});
			for(Map<String, Object> so : results) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodAnu(apogeeBean.getCodAnu());
				ab.setCodElp(apogeeBean.getCodElp());
				ab.setCodExtGpe((so.get("COD_EXT_GPE")!=null)? so.get("COD_EXT_GPE").toString(): "");
				ab.setLibGpe((so.get("LIB_GPE")!=null)? so.get("LIB_GPE").toString(): "");
				groupesTD.add(ab);
			}
		}catch (Exception e) {
			log.error("Erreur lors de la récupération des groupes de TD d’une matière", e);
		}
		
		return groupesTD;
	}
	
	//Requete 6 nb d'étudiant dans le groupe
	public int countAutorisesEpreuveGroupe(ApogeeBean apogeeBean) {
		int count = 0;
		String query = "SELECT  Count(*) "
                + "FROM APOGEE.GPE_OBJ GPE_OBJ, APOGEE.GROUPE GROUPE, APOGEE.IND_AFFECTE_GPE IND_AFFECTE_GPE, "
                + "APOGEE.INDIVIDU INDIVIDU WHERE INDIVIDU.COD_IND = IND_AFFECTE_GPE.COD_IND AND "
                + "GPE_OBJ.COD_GPE = IND_AFFECTE_GPE.COD_GPE AND GROUPE.COD_GPE = GPE_OBJ.COD_GPE "
                + "AND IND_AFFECTE_GPE.COD_ANU = ? AND  GROUPE.COD_EXT_GPE = ? ";

		try {
			count =apogeeJdbcTemplate.queryForObject(
					query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodExtGpe()}, Integer.class);
		} catch (DataAccessException e) {
			log.error("Erreur lors du comptage du nombre d'étudiants d'un groupe Apogée", e);
		}
		 
		 return count;
					
	}
	
	//Requete 7   : Récupération de la liste étudiants d'un groupe
	public List<ApogeeBean> getAutorisesEpreuveGroupe(ApogeeBean apogeeBean){
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> autorisesEpreuve = new ArrayList<ApogeeBean>();
		
		String query = "SELECT INDIVIDU.COD_ETU, INDIVIDU.LIB_NOM_PAT_IND, INDIVIDU.LIB_PR1_IND, INDIVIDU.DATE_NAI_IND "
				+ "FROM APOGEE.GPE_OBJ GPE_OBJ, APOGEE.GROUPE GROUPE, APOGEE.IND_AFFECTE_GPE IND_AFFECTE_GPE, "
				+ "APOGEE.INDIVIDU INDIVIDU WHERE INDIVIDU.COD_IND = IND_AFFECTE_GPE.COD_IND AND "
				+ "GPE_OBJ.COD_GPE = IND_AFFECTE_GPE.COD_GPE AND GROUPE.COD_GPE = GPE_OBJ.COD_GPE "
				+ "AND IND_AFFECTE_GPE.COD_ANU = ? AND  GROUPE.COD_EXT_GPE = ? "
				+ "ORDER BY INDIVIDU.LIB_NOM_PAT_IND";
		
		try {
			results = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodExtGpe()});
			for(Map<String, Object> so : results) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodExtGpe(apogeeBean.getCodExtGpe());
				ab.setCodAnu(apogeeBean.getCodAnu());
				ab.setCodEtp(apogeeBean.getCodEtp());
				ab.setCodElp(apogeeBean.getCodElp());				
				ab.setLibElp(apogeeBean.getLibElp());
				ab.setCodSes(apogeeBean.getCodSes());
				ab.setCodEtu((so.get("COD_ETU")!=null)? so.get("COD_ETU").toString(): "");
				ab.setLibNomPatInd((so.get("LIB_NOM_PAT_IND")!=null)? so.get("LIB_NOM_PAT_IND").toString(): "");
				ab.setLibPr1Ind((so.get("LIB_PR1_IND")!=null)? so.get("LIB_PR1_IND").toString(): "");
				ab.setDateNaiInd((so.get("DATE_NAI_IND")!=null)? so.get("DATE_NAI_IND").toString(): "");
				autorisesEpreuve.add(ab);
			}
			
		} catch (Exception e) {
			log.error("Erreur lors de la récupération de la liste étudiants d'un groupe", e);
		}
		
		return autorisesEpreuve;
	}
	
	public List<ApogeeBean> getListeFutursInscrits(ApogeeBean apogeeBean) {
		
		List<ApogeeBean> futursInscrits = new ArrayList<ApogeeBean>();
		List<ApogeeBean> autorisesEpreuve = new ArrayList<ApogeeBean>();
		int countAutorisesEpreuve = 0;
		if(apogeeBean.getCodExtGpe()==null ||apogeeBean.getCodExtGpe().isEmpty()) {
			autorisesEpreuve = this.getAutorisesEpreuve(apogeeBean);
			countAutorisesEpreuve = this.countAutorisesEpreuve(apogeeBean);
		}else {
			autorisesEpreuve = this.getAutorisesEpreuveGroupe(apogeeBean);
			countAutorisesEpreuve = this.countAutorisesEpreuveGroupe(apogeeBean);
		}
		int total =0 ;
		if(!autorisesEpreuve.isEmpty()) {
			int count = countAutorisesEpreuve;
			List<ApogeeBean> getAutorisesEpreuve = autorisesEpreuve;
			int size = getAutorisesEpreuve.size();
			if(count == size) {
				total = total + count;
				futursInscrits.addAll(getAutorisesEpreuve);
			}else {
				log.info("Erreur de comptage lors de la récupération d'inscits Apogée");
			}
		}
		return futursInscrits;
	}
	
    public  List<List<String>> getListeFutursInscritsDirectImport(ApogeeBean apogeeBean){
    	
		List<ApogeeBean> futursInscrits = getListeFutursInscrits(apogeeBean);
		List<List<String>> finalList = new ArrayList<List<String>>();
		for(ApogeeBean ab : futursInscrits ) {
			List<String> strings = new ArrayList<String>();
			strings.add(ab.getCodEtu());
			finalList.add(strings);
		}

        return finalList;
    }
}
