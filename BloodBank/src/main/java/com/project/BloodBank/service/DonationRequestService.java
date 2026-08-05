package com.project.BloodBank.service;

import com.project.BloodBank.dto.DonationRequestDto;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.repository.DonationRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// The request lifecycle: raising one, listing them, and moving them between statuses.
//
// The three status changes at the bottom are the whole point of this class. Each one guards what it
// is allowed to move from, which is what makes a decision final.
@Service
public class DonationRequestService {

    private final DonationRequestRepository donationRequestRepository;

    public DonationRequestService(DonationRequestRepository donationRequestRepository) {
        this.donationRequestRepository = donationRequestRepository;
    }

    // Status is not set here. The entity's lifecycle hook defaults it to PENDING, so there is no
    // way to raise a request that starts out already approved.
    @Transactional
    public DonationRequest createRequest(DonationRequestDto dto, User requester) {
        DonationRequest request = new DonationRequest();
        request.setRequestedBloodGroup(dto.getRequestedBloodGroup());
        request.setUnitsNeeded(dto.getUnitsNeeded());
        request.setHospitalName(dto.getHospitalName());
        request.setHospitalAddress(dto.getHospitalAddress());
        request.setUrgencyLevel(dto.getUrgencyLevel());
        request.setNotes(dto.getNotes());
        request.setRequestedBy(requester);

        return donationRequestRepository.save(request);
    }

    // --- Reading ---

    @Transactional(readOnly = true)
    public Page<DonationRequest> getRequestsByUser(User user, Pageable pageable) {
        return donationRequestRepository.findByRequestedBy(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<DonationRequest> getPendingRequests(Pageable pageable) {
        return donationRequestRepository.findByStatus(RequestStatus.PENDING, pageable);
    }

    // Every request regardless of status. The pending queue filters to PENDING, so this is the only
    // place a decided request stays visible.
    @Transactional(readOnly = true)
    public Page<DonationRequest> getAllRequests(Pageable pageable) {
        return donationRequestRepository.findAll(pageable);
    }

    // Approved requests a donor of this group could actually fulfil, for the record-donation
    // dropdown. Empty for a donor with no blood group on file, since nothing is known about who
    // they can give to and offering them everything would be a guess.
    @Transactional(readOnly = true)
    public List<DonationRequest> getApprovedRequestsFor(BloodGroup donorGroup) {
        if (donorGroup == null) {
            return List.of();
        }

        return donationRequestRepository.findByStatusAndRequestedBloodGroupIn(
                RequestStatus.APPROVED, donorGroup.compatibleRecipients());
    }

    @Transactional(readOnly = true)
    public DonationRequest getRequestById(Long id) {
        return donationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found: " + id));
    }

    // --- Status changes ---
    // Each of these records decidedAt as well as the status. That timestamp is what lets the
    // dashboard report what was acted on today, which requestDate cannot answer.

    // The PENDING guard is what makes approval final: a request that was already rejected, or
    // already approved by another administrator, cannot be quietly decided a second time.
    @Transactional
    public DonationRequest approveRequest(Long id) {
        DonationRequest request = getRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setDecidedAt(LocalDateTime.now());
        return donationRequestRepository.save(request);
    }

    @Transactional
    public DonationRequest rejectRequest(Long id) {
        DonationRequest request = getRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setDecidedAt(LocalDateTime.now());
        return donationRequestRepository.save(request);
    }

    // No status guard here, unlike the two above, because the caller has already made the check.
    // Only DonationService calls this, and only after confirming the request is APPROVED and the
    // blood is compatible.
    @Transactional
    public DonationRequest markAsFulfilled(Long id) {
        DonationRequest request = getRequestById(id);
        request.setStatus(RequestStatus.FULFILLED);

        // decidedAt tracks the last move away from PENDING, so fulfilment updates it too. Without
        // this the dashboard could not tell what was fulfilled today.
        request.setDecidedAt(LocalDateTime.now());
        return donationRequestRepository.save(request);
    }
}
