package cz.csas.eligibility.repository;

import cz.csas.eligibility.entity.Eligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EligibilityRepository extends JpaRepository<Eligibility, Long> {

    List<Eligibility> findByClientId(String clientId);

    List<Eligibility> findByCorrelationId(String correlationId);

    List<Eligibility> findAllByResult(Eligibility.EligibilityResultEnum result);
}
