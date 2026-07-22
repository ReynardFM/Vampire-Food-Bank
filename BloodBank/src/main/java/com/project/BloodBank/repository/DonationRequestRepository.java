package com.project.BloodBank.repository;

import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonationRequestRepository extends JpaRepository<DonationRequest, Long>
{
    List<DonationRequest> findByStatus(RequestStatus status);
    List<DonationRequest> findByRequestedBy(User user);
    List<DonationRequest> findByRequestedByAndStatus(User user, RequestStatus status);
    Optional<DonationRequest> findByIdAndRequestedBy(Long id, User user);
}
