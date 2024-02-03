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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApogeeService {

	@Resource
	private JdbcTemplate apogeeJdbcTemplate;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${emargement.datasource.apogee.query.composantes}")
	private String queryComposantes;
	
	@Value("${emargement.datasource.apogee.query.elementsPedagogiques}")
	private String queryElementsPedagogiques;
	
	@Value("${emargement.datasource.apogee.query.matieres}")
	private String queryMatieres;
	
	@Value("${emargement.datasource.apogee.query.countAutorisesEpreuve}")
	private String queryCountAutorisesEpreuve;
	
	@Value("${emargement.datasource.apogee.query.autorisesEpreuve}")
	private String queryAutorisesEpreuve;
	
	@Value("${emargement.datasource.apogee.query.groupes}")
	private String queryGroupes;
	
	@Value("${emargement.datasource.apogee.query.countAutorisesEpreuveGroupe}")
	private String queryCountAutorisesEpreuveGroupe;
	
	@Value("${emargement.datasource.apogee.query.autorisesEpreuveGroupe}")
	private String queryAutorisesEpreuveGroupe;

	@Value("${emargement.datasource.apogee.query.autorisesEpreuveComposante}")
	private String queryAutorisesEpreuveComposante;

	@Value("${emargement.datasource.apogee.query.countAutorisesEpreuveComposante}")
	private String queryCountAutorisesEpreuveComposante;

	@Value("${emargement.datasource.apogee.query.autorisesEpreuveDiplome}")
	private String queryAutorisesEpreuveDiplome;

	@Value("${emargement.datasource.apogee.query.countAutorisesEpreuveDiplome}")
	private String queryCountAutorisesEpreuveDiplome;
	
	//Réquete 0 : Récupération de la liste des composantes 
	public List<ApogeeBean> getComposantes(){
		List<Map<String, Object>> composantes = new ArrayList<Map<String, Object>>();
		List<ApogeeBean> abComposantes = new ArrayList<ApogeeBean>();
		
		String query = queryComposantes;

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
		
		String query = queryElementsPedagogiques;
		
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
		
		String query = queryMatieres;
		
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
		String query = queryCountAutorisesEpreuve;

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
		
		String query = queryAutorisesEpreuve;
		
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
		String query = queryGroupes;
		
		try {
			results = apogeeJdbcTemplate.queryForList(query, new Object[] {apogeeBean.getCodAnu(), apogeeBean.getCodElp()});
			for(Map<String, Object> so : results) {
				ApogeeBean ab = new ApogeeBean();
				ab.setCodAnu(apogeeBean.getCodAnu());
				ab.setCodEtp(apogeeBean.getCodEtp());
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
		String query = queryCountAutorisesEpreuveGroupe;

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
		
		String query = queryAutorisesEpreuveGroupe;
		
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

                String query = queryAutorisesEpreuveComposante;

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
                String query = queryCountAutorisesEpreuveComposante;

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

                String query = queryAutorisesEpreuveDiplome;

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
                String query = queryCountAutorisesEpreuveDiplome;
                try {
			System.out.println("----------------------> codEtp : " + apogeeBean.getCodEtp() );

                        String codEtp = "";
                        String codVet = "";
                        if(!apogeeBean.getCodEtp().isEmpty()) {
                                String [] splitCodetEtp  = apogeeBean.getCodEtp().split("-");
                                codEtp = splitCodetEtp[0];
                                codVet = splitCodetEtp[1];
                        } 
			System.out.println("----------------------> codEtp : " + codEtp );
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
	

    public  List<List<String>> getListeFutursInscritsDirectImport(List<ApogeeBean> futursInscrits){

                List<List<String>> finalList = new ArrayList<List<String>>();
                for(ApogeeBean ab : futursInscrits ) {
                        List<String> strings = new ArrayList<String>();
                        strings.add(ab.getCodEtu());
                        finalList.add(strings);
                }

        return finalList;
    }
/*
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
*/
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
