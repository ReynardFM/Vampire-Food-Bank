package com.project.BloodBank.repository;

import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
    Page<DonationRequest> findByStatus(RequestStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "requestedBy")
    Page<DonationRequest> findAll(Pageable pageable);

    List<DonationRequest> findByRequestedBy(User user);
    Page<DonationRequest> findByRequestedBy(User user, Pageable pageable);
    long countByStatus(RequestStatus status);

    @Query("SELECT r.status, COUNT(r) FROM DonationRequest r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT COUNT(r) FROM DonationRequest r WHERE r.requestDate >= :since")
    long countRaisedSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM DonationRequest r WHERE r.decidedAt >= :since")
    long countDecidedSince(@Param("since") LocalDateTime since);

    /**
     * Requests whose status last moved since the given moment, grouped by the status they are in
     * now. COALESCE covers both cases in one query: a pending request has no decidedAt, so its
     * arrival counts as the change.
     */
    @Query("SELECT r.status, COUNT(r) FROM DonationRequest r "
            + "WHERE COALESCE(r.decidedAt, r.requestDate) >= :since GROUP BY r.status")
    List<Object[]> countGroupedByStatusChangedSince(@Param("since") LocalDateTime since);
}
