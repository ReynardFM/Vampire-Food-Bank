package com.project.BloodBank.repository;

import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long>
{
    List<Donation> findByDonor(User donor);
    List<Donation> findByDonorOrderByDonationDateDesc(User donor);
}
