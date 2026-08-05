package com.codeguardian.repository;
import com.codeguardian.entity.ReviewTask;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository public interface ReviewTaskRepository extends JpaRepository<ReviewTask,Long>{
    Page<ReviewTask> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<ReviewTask> findByReviewType(String reviewType);
    List<ReviewTask> findByStatus(String status);
    @Query("select t from ReviewTask t where (:name is null or lower(t.name) like lower(concat('%',:name,'%'))) and (:reviewType is null or t.reviewType=:reviewType)")
    Page<ReviewTask> findByConditions(@Param("name") String name,@Param("reviewType") String reviewType,Pageable pageable);
}
