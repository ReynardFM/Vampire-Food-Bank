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

public interface DonationRepository extends JpaRepository<Donation, Long>{
    List<Donation> findByDonor(User donor);
    Page<Donation> findByDonor(User donor, Pageable pageable);
    List<Donation> findByDonorOrderByDonationDateDesc(User donor);
    @Query("SELECT COUNT(d) FROM Donation d WHERE YEAR(d.donationDate) = YEAR(CURRENT_DATE) AND MONTH(d.donationDate) = MONTH(CURRENT_DATE)")
    long countDonationsThisMonth();

    @Query("SELECT d.donor.bloodGroup, COUNT(d) FROM Donation d GROUP BY d.donor.bloodGroup")
    List<Object[]> countGroupedByDonorBloodGroup();

    @Query("SELECT d.donor.bloodGroup, COUNT(d) FROM Donation d "
            + "WHERE d.donationDate >= :since GROUP BY d.donor.bloodGroup")
    List<Object[]> countGroupedByDonorBloodGroupSince(@Param("since") LocalDate since);
}
