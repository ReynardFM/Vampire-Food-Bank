package com.project.BloodBank.repository;

import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
