package com.project.BloodBank.repository;

import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

// Database access for blood requests. Mostly derived queries, plus a few counting queries that are
// written out because Spring Data cannot express grouping from a method name.
public interface DonationRequestRepository extends JpaRepository<DonationRequest, Long>
{
    // Backs the record-donation dropdown: approved requests a given donor could actually fulfil.
    //
    // RequestedByNot excludes the donor's own requests. Somebody who needs blood cannot supply it,
    // so offering their own request would only lead to the service refusing it.
    List<DonationRequest> findByStatusAndRequestedBloodGroupInAndRequestedByNot(
            RequestStatus status, Collection<BloodGroup> requestedBloodGroups, User requestedBy);

    // The admin listings print requestedBy.fullName on every row, and requestedBy is LAZY, so
    // without an entity graph each row costs an extra query - the N+1 problem.
    //
    // @EntityGraph rather than a JPQL JOIN FETCH because these also take a Sort, which Spring Data
    // applies to a derived query without needing anything aliased.
    @EntityGraph(attributePaths = "requestedBy")
    Page<DonationRequest> findByStatus(RequestStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "requestedBy")
    Page<DonationRequest> findAll(Pageable pageable);

    List<DonationRequest> findByRequestedBy(User user);
    Page<DonationRequest> findByRequestedBy(User user, Pageable pageable);
    long countByStatus(RequestStatus status);

    // --- Daily reports ---
    // Requests that arrived, and requests that were decided, within a window. Two methods because
    // they answer different questions: a request raised three weeks ago but approved this morning
    // belongs to this morning's report, not to the day it arrived.
    //
    // Between is inclusive at both ends, and rows with a null decidedAt are excluded automatically -
    // which is what keeps pending requests, and the legacy rows with no decision time, out of the
    // decided figures.
    //
    // The entity graph is here because the report tables print requestedBy.fullName on every row.
    @EntityGraph(attributePaths = "requestedBy")
    List<DonationRequest> findByRequestDateBetween(LocalDateTime start, LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = "requestedBy")
    List<DonationRequest> findByDecidedAtBetween(LocalDateTime start, LocalDateTime end, Sort sort);

    // --- Dashboard counts ---
    // @Query is JPQL, not SQL: it names entities and fields, not tables and columns. :since is
    // bound by @Param, which must be spelled out - without it the parameter silently fails to bind.
    //
    // These return Object[] because they select two things at once, a status and its count. The
    // service turns each row into a map entry.

    @Query("SELECT r.status, COUNT(r) FROM DonationRequest r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT COUNT(r) FROM DonationRequest r WHERE r.requestDate >= :since")
    long countRaisedSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM DonationRequest r WHERE r.decidedAt >= :since")
    long countDecidedSince(@Param("since") LocalDateTime since);

    // Requests whose status last moved since a given moment, grouped by where they are now. Pending
    // requests have no decidedAt, so their arrival counts as the change; everything else is judged
    // on decidedAt alone.
    //
    // This was once COALESCE(r.decidedAt, r.requestDate), which read better but fired for any null
    // decidedAt rather than only pending ones. Rows approved before the decided_at column existed
    // have a null there, so they fell back to their raise date and were reported as moving into
    // APPROVED on the day they were merely raised - permanently, since that date never changes.
    // A row whose decision time is unknown belongs in no day's total.
    @Query("SELECT r.status, COUNT(r) FROM DonationRequest r WHERE "
            + "(r.status = com.project.BloodBank.model.enums.RequestStatus.PENDING "
            + "  AND r.requestDate >= :since) "
            + "OR (r.status <> com.project.BloodBank.model.enums.RequestStatus.PENDING "
            + "  AND r.decidedAt >= :since) "
            + "GROUP BY r.status")
    List<Object[]> countGroupedByStatusChangedSince(@Param("since") LocalDateTime since);
}
