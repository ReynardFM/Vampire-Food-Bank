package com.project.BloodBank.repository;

import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

// Database access for donations: a donor's history, plus the counts the dashboard charts.
public interface DonationRepository extends JpaRepository<Donation, Long>{

    // Donation history. The paged version backs the profile table; the other two are used where the
    // whole list is wanted, such as the seeder working out each donor's most recent donation.
    List<Donation> findByDonor(User donor);
    Page<Donation> findByDonor(User donor, Pageable pageable);
    List<Donation> findByDonorOrderByDonationDateDesc(User donor);

    // Donations collected within a window, for the daily reports.
    //
    // Both associations are fetched because the report prints the donor's name and, where there is
    // one, the request the donation closed - and both are LAZY, so each row would otherwise cost
    // two more queries.
    @EntityGraph(attributePaths = {"donor", "linkedRequest"})
    List<Donation> findByDonationDateBetween(LocalDate start, LocalDate end, Sort sort);

    // How many units have been collected against one request so far, across every donation linked
    // to it. Used to decide whether the request is finished.
    //
    // Returns null rather than zero when nothing is linked yet, because SQL's SUM over no rows is
    // null. COALESCE could hide that here, but the types it would have to reconcile are awkward in
    // JPQL, so the caller handles the null instead - which is at least impossible to overlook.
    @Query("SELECT SUM(d.unitsDonated) FROM Donation d WHERE d.linkedRequest.id = :requestId")
    Long sumUnitsCollectedFor(@Param("requestId") Long requestId);

    // CURRENT_DATE is evaluated by the database on every call, so this follows the calendar without
    // the application having to pass a date in.
    @Query("SELECT COUNT(d) FROM Donation d WHERE YEAR(d.donationDate) = YEAR(CURRENT_DATE) AND MONTH(d.donationDate) = MONTH(CURRENT_DATE)")
    long countDonationsThisMonth();

    // Donations grouped by the donor's blood group, for the dashboard bars. Note d.donor.bloodGroup
    // - JPQL walks the relationship, and Hibernate turns that into a join.
    //
    // Donors with no blood group land under a null key, which the service drops.
    @Query("SELECT d.donor.bloodGroup, COUNT(d) FROM Donation d GROUP BY d.donor.bloodGroup")
    List<Object[]> countGroupedByDonorBloodGroup();

    // The same thing limited to recent donations, which is how each bar shows a "today" segment.
    @Query("SELECT d.donor.bloodGroup, COUNT(d) FROM Donation d "
            + "WHERE d.donationDate >= :since GROUP BY d.donor.bloodGroup")
    List<Object[]> countGroupedByDonorBloodGroupSince(@Param("since") LocalDate since);
}
