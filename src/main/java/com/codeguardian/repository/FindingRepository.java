package com.codeguardian.repository;
import com.codeguardian.entity.Finding; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.stereotype.Repository; import java.util.List;
@Repository public interface FindingRepository extends JpaRepository<Finding,Long>{ List<Finding> findByTaskId(Long taskId); List<Finding> findBySeverity(String severity); List<Finding> findByCategory(String category); }
