package solvit.teachmon.domain.branch.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {
    @Query("SELECT b FROM BranchEntity b WHERE b.year = :year AND b.branch = :branch")
    Optional<BranchEntity> findByYearAndBranch(Integer year, Integer branch);

    @Query("SELECT b FROM BranchEntity b WHERE b.year = :year ORDER BY b.branch")
    List<BranchEntity> findByYearOrderByBranch(Integer year);

    @Query("SELECT b FROM BranchEntity b WHERE b.year = :year AND b.startDay <= :date AND b.endDay >= :date")
    Optional<BranchEntity> findByYearAndDate(Integer year, LocalDate date);

    @Query("""
    select b
    from BranchEntity b
    where :today between b.startDay and b.endDay
""")
    Optional<BranchEntity> findByDay(@Param("today") LocalDate today);

    @Query("SELECT b FROM BranchEntity b WHERE b.afterSchoolEndDay = :date")
    Optional<BranchEntity> findByAfterSchoolDate(@Param("date") LocalDate date);
    
    @Query("SELECT b FROM BranchEntity b WHERE :date BETWEEN b.startDay AND b.endDay")
    Optional<BranchEntity> findCurrentBranch(@Param("date") LocalDate date);
}
