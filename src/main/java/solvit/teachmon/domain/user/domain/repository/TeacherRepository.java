package solvit.teachmon.domain.user.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.repository.querydsl.TeacherQueryDslRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<TeacherEntity, Long>, TeacherQueryDslRepository {
    @Query("select t from TeacherEntity t where t.mail = :mail")
    Optional<TeacherEntity> findByMail(String mail);

    @Query("select t from TeacherEntity t where t.providerId = :providerId and t.oAuth2Type = :oAuth2Type")
    Optional<TeacherEntity> findByProviderIdAndOAuth2Type(String providerId, OAuth2Type oAuth2Type);

    @Query("select t from TeacherEntity t where t.name like concat('%', :keyword, '%')")
    List<TeacherEntity> queryTeachersByName(@Param("keyword") String keyword);
}
