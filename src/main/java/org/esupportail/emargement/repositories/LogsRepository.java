package org.esupportail.emargement.repositories;

import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogsRepository extends JpaRepository<Log, Long> {
	
	List<Log> findLogByContext(Context context);
	
	Page<Log> findLogByContextIsNull(Pageable pageable);
	
	List<Log> findLogByContextAndLogDateLessThan(Context context, Date date);
}
