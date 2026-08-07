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

    // Approved requests this donor could actually fulfil, for the record-donation dropdown.
    //
    // Two things narrow it: compatibility, and the donor's own requests being excluded. Empty for a
    // donor with no blood group on file, since nothing is known about who they can give to and
    // offering them everything would be a guess.
    @Transactional(readOnly = true)
    public List<DonationRequest> getApprovedRequestsFor(User donor) {
        BloodGroup donorGroup = donor.getBloodGroup();

        if (donorGroup == null) {
            return List.of();
        }

        return donationRequestRepository.findByStatusAndRequestedBloodGroupInAndRequestedByNot(
                RequestStatus.APPROVED, donorGroup.compatibleRecipients(), donor);
    }

    @Transactional(readOnly = true)
    public DonationRequest getRequestById(Long id) {
        return donationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found: " + id));
    }

    // --- Status changes ---
    // Each of these records who acted and when, alongside the status itself. The timestamp is what
    // lets the dashboard report what was done today, which requestDate cannot answer; the name is
    // what makes it an audit trail rather than a bare fact.
    //
    // The acting administrator is passed in rather than read from the security context here, so
    // these stay callable from the seeder and the tests, where there is nobody signed in.

    // The PENDING guard is what makes approval final: a request that was already rejected, or
    // already approved by another administrator, cannot be quietly decided a second time.
    @Transactional
    public DonationRequest approveRequest(Long id, User decidedBy) {
        DonationRequest request = getRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved. Current status: " + request.getStatus());
        }

        return decide(request, RequestStatus.APPROVED, decidedBy);
    }

    @Transactional
    public DonationRequest rejectRequest(Long id, User decidedBy) {
        DonationRequest request = getRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected. Current status: " + request.getStatus());
        }

        return decide(request, RequestStatus.REJECTED, decidedBy);
    }

    // No status guard here, unlike the two above, because the caller has already made the check.
    // Only DonationService calls this, and only after confirming the request is APPROVED and the
    // blood is compatible.
    //
    // The name recorded is whoever entered the donation - fulfilment is not a decision somebody
    // sits down to make, it is the side effect of recording blood arriving.
    @Transactional
    public DonationRequest markAsFulfilled(Long id, User recordedBy) {
        DonationRequest request = getRequestById(id);
        return decide(request, RequestStatus.FULFILLED, recordedBy);
    }

    // The one place a status moves off PENDING, so the timestamp and the name can never be set by
    // one path and forgotten by another.
    private DonationRequest decide(DonationRequest request, RequestStatus status, User actor) {
        request.setStatus(status);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(actor);
        return donationRequestRepository.save(request);
    }
}
