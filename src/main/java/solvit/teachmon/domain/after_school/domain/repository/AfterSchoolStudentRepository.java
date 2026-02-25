package solvit.teachmon.domain.after_school.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolStudentEntity;

import java.util.List;

public interface AfterSchoolStudentRepository extends JpaRepository<AfterSchoolStudentEntity, Long> {
    
    @Query("SELECT ast FROM AfterSchoolStudentEntity ast " +
           "JOIN FETCH ast.student s " +
           "WHERE ast.afterSchool.id IN :afterSchoolIds")
    List<AfterSchoolStudentEntity> findByAfterSchoolIdsWithStudent(@Param("afterSchoolIds") List<Long> afterSchoolIds);

    @Modifying
    @Query("DELETE FROM AfterSchoolStudentEntity ast WHERE ast.afterSchool.id = :afterSchoolId AND ast.student.id IN :studentIds")
    void deleteByAfterSchoolIdAndStudentIdIn(@Param("afterSchoolId") Long afterSchoolId, @Param("studentIds") List<Long> studentIds);
}