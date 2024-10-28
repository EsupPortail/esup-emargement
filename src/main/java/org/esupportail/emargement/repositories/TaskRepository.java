package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Task;
import org.esupportail.emargement.domain.Task.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>{
	
	List<Task> findByContext(Context ctx);
	
	List<Task> findByContextAndStatusNotLike(Context ctx, Status status);
	
	List<Task> findByContextKeyAndParam(String key, String param);

}
