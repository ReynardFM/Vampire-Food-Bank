package com.project.BloodBank.repository;

import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long>{
    List<Donation> findByDonor(User donor);
    List<Donation> findByDonor(User donor, Sort sort);
    List<Donation> findByDonorOrderByDonationDateDesc(User donor);
    @Query("SELECT COUNT(d) FROM Donation d WHERE YEAR(d.donationDate) = YEAR(CURRENT_DATE) AND MONTH(d.donationDate) = MONTH(CURRENT_DATE)")
    long countDonationsThisMonth();

    @Query("SELECT d.donor.bloodGroup, COUNT(d) FROM Donation d GROUP BY d.donor.bloodGroup")
    List<Object[]> countGroupedByDonorBloodGroup();
}
