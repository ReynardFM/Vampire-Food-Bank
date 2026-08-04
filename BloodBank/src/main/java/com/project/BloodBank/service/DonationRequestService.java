package com.project.BloodBank.service;

import com.project.BloodBank.dto.DonationRequestDto;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.repository.DonationRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DonationRequestService {

    private final DonationRequestRepository donationRequestRepository;

    public DonationRequestService(DonationRequestRepository donationRequestRepository) {
        this.donationRequestRepository = donationRequestRepository;
    }

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

    @Transactional(readOnly = true)
    public Page<DonationRequest> getRequestsByUser(User user, Pageable pageable) {
        return donationRequestRepository.findByRequestedBy(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<DonationRequest> getPendingRequests(Pageable pageable) {
        return donationRequestRepository.findByStatus(RequestStatus.PENDING, pageable);
    }

    /** Every request regardless of status; the pending queue only ever shows PENDING. */
    @Transactional(readOnly = true)
    public Page<DonationRequest> getAllRequests(Pageable pageable) {
        return donationRequestRepository.findAll(pageable);
    }

    /** Approved requests are the only ones a donation may be linked to. */
    @Transactional(readOnly = true)
    public List<DonationRequest> getApprovedRequests() {
        return donationRequestRepository.findByStatus(RequestStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public DonationRequest getRequestById(Long id) {
        return donationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found: " + id));
    }

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

    @Transactional
    public DonationRequest markAsFulfilled(Long id) {
        DonationRequest request = getRequestById(id);
        request.setStatus(RequestStatus.FULFILLED);
        // decidedAt tracks the last move away from PENDING, so fulfilment updates it too.
        // Without this the dashboard could not tell what was fulfilled today.
        request.setDecidedAt(LocalDateTime.now());
        return donationRequestRepository.save(request);
    }
}

