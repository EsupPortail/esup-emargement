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
	
	Person findByEppnAndContext(String eppn, Context context);
	
	@Modifying
	@Query(value = "delete from person where id not in (select person_id from tag_check) and context_id = :ctxId ", nativeQuery = true)
	int cleanPersons(Long ctxId);
	
}
