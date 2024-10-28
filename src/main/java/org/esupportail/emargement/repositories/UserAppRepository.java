package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserApp.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAppRepository extends JpaRepository<UserApp, Long>{
	
	UserApp findByEppnAndContext(String eppn, Context context);
	
	Page<UserApp> findByEppnAndContext(String eppn, Context contex, Pageable pageablet);
	
	Long countByEppnAndContext(String eppn, Context context);
	
	Page<UserApp> findByUserRoleAndContextKey(Role role, String key, Pageable pageable);
	
	List<UserApp> findByContext(Context context);
	
	@Query(value = "select * from user_app where eppn=:eppn and context_id=:contexId", nativeQuery = true)
	UserApp findByEppnContext(String eppn, Long contexId);
	
	@Query(value = "select * from user_app order by eppn", nativeQuery = true)
	List<UserApp> findAll();
	
	//STATS
	@Query(value = "select key, user_role, count(*) as count from user_app, context where user_app.context_id=context.id group by key, user_role order by  key, user_role, count desc", nativeQuery = true)
	List<Object[]> countUserAppsByContext();
	
}
