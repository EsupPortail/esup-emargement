package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long>{
	
	List<Person> findByContext(Context context);

	List<Person> findByEppn(String eppn);
	
	List<Person> findByNumIdentifiant(String num);
	
	List<Person> findByEppnAndContext(String eppn, Context context);
	
	@Modifying
	@Query(value = "delete from person where context_id = :ctxId and id not in (select person_id from tag_check where context_id = :ctxId and person_id is not null) "
			+ "and id not in (select proxy_person_id from tag_check where context_id = :ctxId and person_id is not null) "
			+ "and id not in (select person_id from groupe, groupe_person where context_id = :ctxId and person_id is not null and groupe.id=groupe_person.groupe_id)", nativeQuery = true)
	int cleanPersons(Long ctxId);

}
