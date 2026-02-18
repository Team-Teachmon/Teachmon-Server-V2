package solvit.teachmon.domain.place.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;

import org.springframework.stereotype.Repository;

@Repository
public interface PlaceRepository extends JpaRepository<PlaceEntity, Long>, PlaceQueryDslRepository {
}
