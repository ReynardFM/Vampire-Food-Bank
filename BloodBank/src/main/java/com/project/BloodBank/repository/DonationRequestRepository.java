package com.project.BloodBank.repository;

import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.RequestStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DonationRequestRepository extends JpaRepository<DonationRequest, Long>
{
    List<DonationRequest> findByStatus(RequestStatus status);

    /*
     * The admin listings print requestedBy.fullName on every row. requestedBy is LAZY, so without
     * an entity graph each row costs an extra query. An @EntityGraph is used rather than a JPQL
     * JOIN FETCH because these methods also take a Sort, and Spring Data applies sorting to a
     * derived query cleanly without having to alias anything.
     */
    @EntityGraph(attributePaths = "requestedBy")
    List<DonationRequest> findByStatus(RequestStatus status, Sort sort);

    @Override
    @EntityGraph(attributePaths = "requestedBy")
    List<DonationRequest> findAll(Sort sort);
    List<DonationRequest> findByRequestedBy(User user);
    List<DonationRequest> findByRequestedBy(User user, Sort sort);
    List<DonationRequest> findByRequestedByAndStatus(User user, RequestStatus status);
    Optional<DonationRequest> findByIdAndRequestedBy(Long id, User user);
    long countByStatus(RequestStatus status);

    @Query("SELECT r.status, COUNT(r) FROM DonationRequest r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();
}
