package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.ApogeeBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(name = "apogeeJdbcTemplate")
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
		String query = "SELECT DISTINCT (IAE.COD_ETP||'-'||IAE.COD_VRS_VET) as code, vet.lib_web_vet as lib " + 
						"FROM ETAPE ETP , INS_ADM_ETP IAE INNER JOIN VERSION_ETAPE VET " +
						"ON (IAE.COD_ETP = VET.COD_ETP AND IAE.COD_VRS_VET = VET.COD_VRS_VET) " + 
						"WHERE IAE.COD_ETP = ETP.COD_ETP " + 
						"AND IAE.COD_ANU= ? " + 
						"AND IAE.COD_CMP= ? " + 
						"AND etp.cod_cur IN ('L','M') " +
						"ORDER BY lib";
		
		try {
			inscrits = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodCmp()});
			for(Map<String, Object> so : inscrits) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodEtp((so.get("code")!=null)? so.get("code").toString(): "");
				ab.setLibEtp((so.get("lib")!=null)? so.get("lib").toString(): "");
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
					   "ORDER BY ELEMENT_PEDAGOGI.LIB_ELP";
		
		try {
			
			String [] splitCodetEtp  = apogeeBean.getCodEtp().split("-");
			matieres = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), splitCodetEtp[0], apogeeBean.getCodAnu(), 
					splitCodetEtp[0], apogeeBean.getCodAnu(), splitCodetEtp[0]});
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
					query, Integer.class, apogeeBean.getCodElp(), apogeeBean.getCodSes(), apogeeBean.getCodAnu());
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
		String query = "SELECT DISTINCT GROUPE.COD_GPE, GROUPE.LIB_GPE FROM APOGEE.GPE_OBJ, APOGEE.GROUPE, " +
				 		"APOGEE.IND_AFFECTE_GPE WHERE GPE_OBJ.COD_GPE = GROUPE.COD_GPE AND IND_AFFECTE_GPE.COD_GPE = GROUPE.COD_GPE " +
				 		"AND IND_AFFECTE_GPE.COD_ANU= ? AND GPE_OBJ.COD_ELP= ? ORDER BY GROUPE.COD_GPE";
		
		try {
			results = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodElp()});
			for(Map<String, Object> so : results) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodAnu(apogeeBean.getCodAnu());
				ab.setCodElp(apogeeBean.getCodElp());
				ab.setCodExtGpe((so.get("COD_GPE")!=null)? so.get("COD_GPE").toString(): "");
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
                + "AND IND_AFFECTE_GPE.COD_ANU = ? AND  GROUPE.COD_GPE = ? ";

		try {
			count =apogeeJdbcTemplate.queryForObject(
					query, Integer.class, apogeeBean.getCodAnu(), apogeeBean.getCodExtGpe());
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
				+ "AND IND_AFFECTE_GPE.COD_ANU = ? AND  GROUPE.COD_GPE = ? AND GPE_OBJ.COD_ELP = ?"
				+ "ORDER BY INDIVIDU.LIB_NOM_PAT_IND";
		
		try {
			results = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodExtGpe(),apogeeBean.getCodElp()});
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
	
	//Requete 8  : Récupération des étudiants d'une composante
	public List<ApogeeBean> getAutorisesEpreuveComposante(ApogeeBean apogeeBean){
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> autorisesEpreuve = new ArrayList<ApogeeBean>();
		
		String query = "SELECT DISTINCT INDIVIDU.COD_ETU, INDIVIDU.LIB_NOM_PAT_IND, INDIVIDU.LIB_PR1_IND, INDIVIDU.DATE_NAI_IND " + 
				"FROM INDIVIDU INNER JOIN INS_ADM_ETP IAE ON (INDIVIDU.COD_IND = IAE.COD_IND) " + 
				"WHERE IAE.COD_ANU = ? " + 
				"AND IAE.COD_CMP = ? " + 
				"AND IAE.ETA_IAE = 'E' " +
				"AND IAE.ETA_PMT_IAE in ('P','A') " +
				"ORDER BY 2";
		
		try {
			results = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodCmp()});
			for(Map<String, Object> so : results) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodExtGpe(apogeeBean.getCodExtGpe());
				ab.setCodAnu(apogeeBean.getCodAnu());
				ab.setCodSes(apogeeBean.getCodSes());
				ab.setCodCmp(apogeeBean.getCodCmp());
				ab.setCodEtu((so.get("COD_ETU")!=null)? so.get("COD_ETU").toString(): "");
				ab.setLibNomPatInd((so.get("LIB_NOM_PAT_IND")!=null)? so.get("LIB_NOM_PAT_IND").toString(): "");
				ab.setLibPr1Ind((so.get("LIB_PR1_IND")!=null)? so.get("LIB_PR1_IND").toString(): "");
				ab.setDateNaiInd((so.get("DATE_NAI_IND")!=null)? so.get("DATE_NAI_IND").toString(): "");
				autorisesEpreuve.add(ab);
			}
			
		} catch (Exception e) {
			log.error("Erreur lors de la récupération de la liste étudiants d'une composante", e);
		}
		
		return autorisesEpreuve;
	}
	
	//Requete 9 nb d'étudiant dans une composante
	public int countAutorisesEpreuveComposante(ApogeeBean apogeeBean) {
		int count = 0;
		String query = "SELECT  Count(DISTINCT INDIVIDU.COD_ETU) " +
                "FROM INDIVIDU INNER JOIN INS_ADM_ETP IAE ON (INDIVIDU.COD_IND = IAE.COD_IND) " +
                "WHERE IAE.COD_ANU = ? " + 
                "AND IAE.COD_CMP = ? " +
                "AND IAE.ETA_IAE = 'E' " +
				"AND IAE.ETA_PMT_IAE in ('P','A') ";

		try {
			count =apogeeJdbcTemplate.queryForObject(
					query, Integer.class, apogeeBean.getCodAnu(), apogeeBean.getCodCmp());
		} catch (DataAccessException e) {
			log.error("Erreur lors du comptage du nombre d'étudiants d'une composante", e);
		}
		 
		 return count;
	}
	
	//Requete 10  : Récupération de la liste étudiants d'une étape(diplome)
	public List<ApogeeBean> getAutorisesEpreuveDiplome(ApogeeBean apogeeBean){
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> autorisesEpreuve = new ArrayList<ApogeeBean>();
		
		String query = "SELECT DISTINCT INDIVIDU.COD_ETU, INDIVIDU.LIB_NOM_PAT_IND, INDIVIDU.LIB_PR1_IND, INDIVIDU.DATE_NAI_IND " + 
				"FROM INDIVIDU, RESULTAT_VET " + 
				"WHERE RESULTAT_VET.COD_IND = INDIVIDU.COD_IND " + 
				"AND RESULTAT_VET.COD_ADM='1' " + 
				"AND RESULTAT_VET.COD_ETP = ? " + 
				"AND RESULTAT_VET.COD_VRS_VET = ? " + 
				"AND RESULTAT_VET.COD_SES= ? " + 
				"AND RESULTAT_VET.TEM_IND_CRN_VET='CS' " + 
				"AND RESULTAT_VET.TEM_TRT_RPT_VET='N' " + 
				"AND RESULTAT_VET.COD_ANU = ? " + 
				"ORDER BY INDIVIDU.LIB_NOM_PAT_IND";
		
		try {
			String [] splitCodetEtp  = apogeeBean.getCodEtp().split("-");
			results = apogeeJdbcTemplate.queryForList(query, new Object[] {splitCodetEtp[0], splitCodetEtp[1], apogeeBean.getCodSes(), apogeeBean.getCodAnu()});
			for(Map<String, Object> so : results) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodExtGpe(apogeeBean.getCodExtGpe());
				ab.setCodAnu(apogeeBean.getCodAnu());
				ab.setCodEtp(splitCodetEtp[0]);
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
	
	
	//Requete 11 nb d'étudiant dans une étape
	public int countAutorisesEpreuveDiplome(ApogeeBean apogeeBean) {
		int count = 0;
		String query = "SELECT count(*) " + 
				"FROM INDIVIDU, RESULTAT_VET " + 
				"WHERE RESULTAT_VET.COD_IND = INDIVIDU.COD_IND " + 
				"AND RESULTAT_VET.COD_ADM='1' " + 
				"AND RESULTAT_VET.COD_ETP = ? " + 
				"AND RESULTAT_VET.COD_VRS_VET = ? " + 
				"AND RESULTAT_VET.COD_SES= ? " + 
				"AND RESULTAT_VET.TEM_IND_CRN_VET='CS' " + 
				"AND RESULTAT_VET.TEM_TRT_RPT_VET='N' " + 
				"AND RESULTAT_VET.COD_ANU = ? ";
		try {
			String codEtp = "";
			String codVet = "";
			if(!apogeeBean.getCodEtp().isEmpty()) {
				String [] splitCodetEtp  = apogeeBean.getCodEtp().split("-");
				codEtp = splitCodetEtp[0];
				codVet = splitCodetEtp[1];
			}
			count =apogeeJdbcTemplate.queryForObject(
					query, Integer.class, codEtp, codVet, apogeeBean.getCodSes(), apogeeBean.getCodAnu());
		} catch (DataAccessException e) {
			log.error("Erreur lors du comptage du nombre d'étudiants d'une étape", e);
		}
		 
		 return count;
	}
	
	public List<ApogeeBean> getListeFutursInscrits(ApogeeBean apogeeBean) {
		
		List<ApogeeBean> futursInscrits = new ArrayList<ApogeeBean>();
		List<ApogeeBean> autorisesEpreuve = new ArrayList<ApogeeBean>();
		int countAutorisesEpreuve = 0;
		if(apogeeBean.getCodEtp()==null || apogeeBean.getCodEtp().isEmpty()) {
			if(apogeeBean.getCodCmp()!=null || !apogeeBean.getCodCmp().isEmpty()) {
				autorisesEpreuve = this.getAutorisesEpreuveComposante(apogeeBean);
				countAutorisesEpreuve = this.countAutorisesEpreuveComposante(apogeeBean);
			}
		}else if(apogeeBean.getCodElp()==null || apogeeBean.getCodElp().isEmpty()) {
			if(apogeeBean.getCodEtp()!=null || !apogeeBean.getCodEtp().isEmpty()) {
				autorisesEpreuve = this.getAutorisesEpreuveDiplome(apogeeBean);
				countAutorisesEpreuve = this.countAutorisesEpreuveDiplome(apogeeBean);
			}
		}else if(apogeeBean.getCodExtGpe()==null || apogeeBean.getCodExtGpe().isEmpty()) {
			if(apogeeBean.getCodElp()!=null || !apogeeBean.getCodElp().isEmpty()) {
				autorisesEpreuve = this.getAutorisesEpreuve(apogeeBean);
				countAutorisesEpreuve = this.countAutorisesEpreuve(apogeeBean);
			}
		}else if(apogeeBean.getCodExtGpe() !=null || !apogeeBean.getCodExtGpe().isEmpty()) {
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
	
    public  List<List<String>> getListeFutursInscritsDirectImport(List<ApogeeBean> futursInscrits){
    	
		List<List<String>> finalList = new ArrayList<List<String>>();
		for(ApogeeBean ab : futursInscrits ) {
			List<String> strings = new ArrayList<String>();
			strings.add(ab.getCodEtu());
			finalList.add(strings);
		}

        return finalList;
    }
    
    public int countAutorises(String param, ApogeeBean apogeeBean) {
    	int count = 0;
    	switch(param){
	    	case "composante": 
	    		count= countAutorisesEpreuveComposante(apogeeBean);
	    		break;
	    	case "diplome":
	    		count = countAutorisesEpreuveDiplome(apogeeBean);
	    		break;
	    	case "matiere": 
	    		count = countAutorisesEpreuve(apogeeBean);
	    		break;
	    	case "groupe":
	    		count =countAutorisesEpreuveGroupe(apogeeBean);
	    		break;
    	}

    	return count;
    }
    
    public List<ApogeeBean> searchList(String param, ApogeeBean apogeeBean) {
    	List<ApogeeBean> list= new ArrayList<ApogeeBean>();
    	switch(param){
	    	case "diplome":
	    		list =  getElementsPedagogiques(apogeeBean);
	    		break;
	    	case "matiere": 
	    		list =  getMatieres(apogeeBean);
	    		break;
	    	case "groupe":
	    		list = getGroupes(apogeeBean);
	    		break;
    	}

    	return list;
    }
    
    public Map<String,String> getMapEtapes(ApogeeBean apogeebean, List<ApogeeBean> futursInscrits){
    	Map<String,String> mapEtapes = new HashMap<String,String>();
    	List<ApogeeBean> list = getElementsPedagogiques(apogeebean);
    	Map<String, String> mapEtp = list.stream().collect(
    			Collectors.toMap(ApogeeBean::getCodEtp, ApogeeBean::getLibEtp));
    	for(ApogeeBean apogeeBean : futursInscrits) {
    		mapEtapes.put(apogeeBean.getCodEtu(),  apogeeBean.getCodEtp() + " - " + mapEtp.get(apogeeBean.getCodEtp()));
    	}
    	return mapEtapes;
    }
}
