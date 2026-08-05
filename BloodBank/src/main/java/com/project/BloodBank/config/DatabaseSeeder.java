package com.project.BloodBank.config;

import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Gender;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.model.enums.UrgencyLevel;
import com.project.BloodBank.repository.DonationRepository;
import com.project.BloodBank.repository.DonationRequestRepository;
import com.project.BloodBank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Fills an empty database with a working blood bank: an administrator, donors covering every blood
// group, a request queue spanning every status, and roughly a year of donation history.
//
// This is not a convenience. Registration only creates DONOR accounts and nothing else makes an
// administrator, so without this a fresh database cannot reach /dashboard or /admin/** at all.
// Passwords are encoded here rather than written as SQL because BCrypt hashes have to be generated.
//
// CommandLineRunner means Spring calls run() once, after the application has fully started - which
// matters, because it needs the repositories and the password encoder to already exist.
//
// Safe to leave enabled, and safe against a database you have been using. Donors are matched on
// email and the history is keyed off a seed-owned account, so restarting never duplicates anything
// and never touches your own data. Turn it off with bloodbank.seed.enabled=false.
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    // How the seeder knows whether it has run before. This account only ever exists because the
    // seeder created it, so anything attached to it came from a previous run.
    //
    // Checking a marker rather than counting rows matters: one hand-made request would otherwise
    // look like "already seeded" and block the whole set.
    private static final String MARKER_EMAIL = "jane.doe@example.com";

    // Records describing the data to create, so the rosters below read as tables rather than as
    // pages of setter calls. They are private and used nowhere else.
    private record DonorSeed(String email, String name, BloodGroup group, Gender gender,
                             LocalDate born, String phone, String address, boolean active) {
    }

    // daysAgo rather than a fixed date, so seeded history is always relative to when it was
    // created and the dashboard has recent activity whenever the project is set up.
    //
    // decidedDaysAgo is separate from daysAgo, and must be smaller or equal - a request cannot be
    // decided before it arrived. Keeping them apart is what gives the daily reports something real
    // to show: a request raised on one day and approved on another is the ordinary case, and the
    // reports exist precisely to tell those two days apart. Ignored for a pending request.
    private record RequestSeed(String email, BloodGroup group, int units, String hospital,
                               String hospitalAddress, UrgencyLevel urgency, RequestStatus status,
                               int daysAgo, int decidedDaysAgo, String notes) {
    }

    // linkedRequest indexes into the saved request list, or -1 for an unlinked walk-in donation.
    private record DonationSeed(String email, int daysAgo, int units, String location,
                                int linkedRequest) {
    }

    private final UserRepository userRepository;
    private final DonationRequestRepository requestRepository;
    private final DonationRepository donationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bloodbank.seed.enabled:true}")
    private boolean enabled;

    @Value("${bloodbank.seed.admin-email:admin@lifeline.com}")
    private String adminEmail;

    @Value("${bloodbank.seed.admin-password:Admin123!}")
    private String adminPassword;

    @Value("${bloodbank.seed.donor-password:Password123!}")
    private String donorPassword;

    public DatabaseSeeder(UserRepository userRepository,
                          DonationRequestRepository requestRepository,
                          DonationRepository donationRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.donationRepository = donationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Runs once at startup. @Transactional wraps the lot, so a failure half way leaves an empty
    // database rather than a half-built one that the guards below would then treat as complete.
    //
    // Order matters: requests and donations both reference users, so people have to exist first.
    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        seedAdmin();
        seedDonors();
        seedHistory();

        log.info("Seed complete: {} accounts, {} requests, {} donations. Administrator is '{}'.",
                userRepository.count(), requestRepository.count(), donationRepository.count(),
                adminEmail);
    }

    // ------------------------------------------------------------------ people

    // Each seed method starts by checking whether its own work already exists, which is what makes
    // the whole seeder safe to run repeatedly.
    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = newUser(adminEmail, "LifeLine Administrator", Role.ADMIN, true);
        admin.setPhoneNumber("+1 416 555 0100");
        admin.setAddress("120 Front Street West, Toronto, ON");
        admin.setGender(Gender.OTHER);
        userRepository.save(admin);

        log.warn("Created administrator '{}'. Change its password before showing this to anyone.",
                adminEmail);
    }

    // Every blood group appears at least once, so donor search never comes back empty whichever
    // group is chosen - including O-, which only O- donors can serve.
    private List<DonorSeed> donorRoster() {
        return List.of(
                new DonorSeed("jane.doe@example.com", "Jane Doe", BloodGroup.A_POSITIVE, Gender.FEMALE,
                        LocalDate.of(1994, 3, 12), "+1 416 555 0123", "45 Bloor Street East, Toronto, ON", true),
                new DonorSeed("marcus.lee@example.com", "Marcus Lee", BloodGroup.O_NEGATIVE, Gender.MALE,
                        LocalDate.of(1988, 11, 2), "+1 416 555 0144", "9 Queen Street West, Toronto, ON", true),
                new DonorSeed("priya.nair@example.com", "Priya Nair", BloodGroup.B_POSITIVE, Gender.FEMALE,
                        LocalDate.of(1997, 6, 30), "+1 647 555 0188", "310 Yonge Street, Toronto, ON", true),
                new DonorSeed("tomas.silva@example.com", "Tomas Silva", BloodGroup.AB_POSITIVE, Gender.MALE,
                        LocalDate.of(1991, 1, 19), "+1 647 555 0192", "88 Dundas Street East, Toronto, ON", true),
                new DonorSeed("aisha.khan@example.com", "Aisha Khan", BloodGroup.O_POSITIVE, Gender.FEMALE,
                        LocalDate.of(2000, 9, 8), "+1 905 555 0117", "1200 Lakeshore Road, Mississauga, ON", true),
                new DonorSeed("daniel.okafor@example.com", "Daniel Okafor", BloodGroup.A_NEGATIVE, Gender.MALE,
                        LocalDate.of(1985, 4, 25), "+1 905 555 0165", "77 King Street, Hamilton, ON", true),
                new DonorSeed("rowan.avery@example.com", "Rowan Avery", BloodGroup.B_NEGATIVE, Gender.OTHER,
                        LocalDate.of(1996, 7, 14), "+1 416 555 0201", "500 College Street, Toronto, ON", true),
                new DonorSeed("mei.tanaka@example.com", "Mei Tanaka", BloodGroup.AB_NEGATIVE, Gender.FEMALE,
                        LocalDate.of(1993, 12, 5), "+1 647 555 0233", "22 Spadina Avenue, Toronto, ON", true),
                new DonorSeed("liam.murphy@example.com", "Liam Murphy", BloodGroup.O_POSITIVE, Gender.MALE,
                        LocalDate.of(1990, 2, 17), "+1 416 555 0250", "140 Danforth Avenue, Toronto, ON", true),
                new DonorSeed("sofia.rossi@example.com", "Sofia Rossi", BloodGroup.A_POSITIVE, Gender.FEMALE,
                        LocalDate.of(1999, 5, 23), "+1 905 555 0264", "3 Main Street, Brampton, ON", true),
                new DonorSeed("kofi.mensah@example.com", "Kofi Mensah", BloodGroup.O_NEGATIVE, Gender.MALE,
                        LocalDate.of(1987, 8, 9), "+1 647 555 0279", "610 Sheppard Avenue, North York, ON", true),
                new DonorSeed("hana.park@example.com", "Hana Park", BloodGroup.A_NEGATIVE, Gender.FEMALE,
                        LocalDate.of(1995, 10, 28), "+1 416 555 0288", "18 Bay Street, Toronto, ON", true),
                new DonorSeed("noah.bergman@example.com", "Noah Bergman", BloodGroup.B_POSITIVE, Gender.MALE,
                        LocalDate.of(1983, 6, 3), "+1 905 555 0295", "250 Steeles Avenue, Markham, ON", true),
                // Retired from the roster, so the soft-delete path has a real example.
                new DonorSeed("retired.donor@example.com", "Grace Whitfield", BloodGroup.O_POSITIVE,
                        Gender.FEMALE, LocalDate.of(1979, 1, 30), "+1 416 555 0300",
                        "64 Adelaide Street, Toronto, ON", false)
        );
    }

    private void seedDonors() {
        for (DonorSeed seed : donorRoster()) {
            if (userRepository.existsByEmail(seed.email())) {
                continue;
            }

            User user = newUser(seed.email(), seed.name(), Role.DONOR, seed.active());
            user.setBloodGroup(seed.group());
            user.setGender(seed.gender());
            user.setDateOfBirth(seed.born());
            user.setPhoneNumber(seed.phone());
            user.setAddress(seed.address());
            userRepository.save(user);
        }

        // Left bare on purpose so the "Profile Incomplete" state on /donor/profile is reachable
        // without registering a throwaway account.
        if (!userRepository.existsByEmail("new.donor@example.com")) {
            userRepository.save(newUser("new.donor@example.com", "Sam Rivera", Role.DONOR, true));
        }
    }

    private User newUser(String email, String name, Role role, boolean active) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setPassword(passwordEncoder.encode(role == Role.ADMIN ? adminPassword : donorPassword));
        user.setRole(role);
        // active is a plain boolean, so it defaults to false and would block sign-in.
        user.setActive(active);
        return user;
    }

    // ------------------------------------------------------- requests and donations

    private void seedHistory() {
        // Keyed off a seed-owned account rather than the table totals: requests you raise yourself
        // must not count as "already seeded", or one hand-made request would block the whole set.
        User marker = required(MARKER_EMAIL);
        boolean hasRequests = !requestRepository.findByRequestedBy(marker).isEmpty();
        boolean hasDonations = !donationRepository.findByDonor(marker).isEmpty();

        if (hasRequests || hasDonations) {
            // Logged rather than skipped in silence. This guard covers requests and donations
            // together, so a database holding one but not the other never gets the missing half -
            // and without a line here, the only symptom is charts that read zero for no visible
            // reason. If this fires when you expected a full seed, the database was not empty.
            log.info("History already present for '{}' (requests: {}, donations: {}); skipping. "
                            + "Drop the database and restart if you wanted it rebuilt.",
                    MARKER_EMAIL, hasRequests, hasDonations);
            return;
        }

        // Saved requests are collected as they are written, because the donations below link to
        // them by position in this list and need the ids the database has just assigned.
        List<DonationRequest> saved = new ArrayList<>();
        for (RequestSeed seed : requestRoster()) {
            DonationRequest request = new DonationRequest();
            request.setRequestedBy(required(seed.email()));
            request.setRequestedBloodGroup(seed.group());
            request.setUnitsNeeded(seed.units());
            request.setHospitalName(seed.hospital());
            request.setHospitalAddress(seed.hospitalAddress());
            request.setUrgencyLevel(seed.urgency());
            request.setStatus(seed.status());
            // @PrePersist only fills this when null, so the explicit date survives.
            LocalDateTime raisedAt = LocalDateTime.now().minusDays(seed.daysAgo()).withHour(9);
            request.setRequestDate(raisedAt);

            // Anything already decided needs a decision time, or the history reads as though it
            // resolved itself.
            //
            // This used to be raisedAt.plusHours(6), which meant every request was decided on the
            // day it arrived. That made the daily reports meaningless - "raised" and "decided"
            // always fell on the same day, and the waiting time the report shows was always zero.
            // Each seed now carries its own decision day.
            //
            // Afternoon rather than morning, so a request raised and decided on the same day still
            // reads in the right order.
            if (seed.status() != RequestStatus.PENDING) {
                request.setDecidedAt(
                        LocalDateTime.now().minusDays(seed.decidedDaysAgo()).withHour(14));
            }
            request.setNotes(seed.notes());
            saved.add(requestRepository.save(request));
        }

        for (DonationSeed seed : donationRoster()) {
            Donation donation = new Donation();
            donation.setDonor(required(seed.email()));
            donation.setDonationDate(LocalDate.now().minusDays(seed.daysAgo()));
            donation.setUnitsDonated(seed.units());
            donation.setLocation(seed.location());
            donation.setLinkedRequest(seed.linkedRequest() >= 0 ? saved.get(seed.linkedRequest()) : null);
            donationRepository.save(donation);
        }

        backfillLastDonationDates();
    }

    // A spread of statuses and urgencies, so every filter, badge colour and sort order has
    // something to show.
    //
    // Positions matter, because donationRoster() links to these by index: 3, 7 and 10 are the
    // FULFILLED ones, and 9 is an APPROVED request that has had part of its blood collected.
    // Reordering this list would attach donations to the wrong requests.
    //
    // The decided days are chosen to fall on different days from the raises, so the daily reports
    // show requests arriving on one day and being decided on another - which is the whole reason
    // those two figures are counted separately.
    private List<RequestSeed> requestRoster() {
        return List.of(
                new RequestSeed("jane.doe@example.com", BloodGroup.A_POSITIVE, 2, "Toronto General Hospital",
                        "200 Elizabeth Street, Toronto, ON", UrgencyLevel.HIGH, RequestStatus.PENDING,
                        1, 0, "Scheduled surgery on Friday."),
                // Deliberately not daysAgo = 0. The seeder stamps a real timestamp once and it never
                // moves again, so a request raised "today" is really raised on whatever day the
                // database was first seeded - which leaves the dashboard's today counters showing
                // activity nobody performed, and makes it impossible to tell seed data from your own.
                new RequestSeed("priya.nair@example.com", BloodGroup.O_NEGATIVE, 4, "Mount Sinai Hospital",
                        "600 University Avenue, Toronto, ON", UrgencyLevel.CRITICAL, RequestStatus.PENDING,
                        1, 0, "Trauma case, needs universal donor units."),
                new RequestSeed("noah.bergman@example.com", BloodGroup.B_NEGATIVE, 1, "Markham Stouffville Hospital",
                        "381 Church Street, Markham, ON", UrgencyLevel.MEDIUM, RequestStatus.PENDING,
                        3, 0, null),
                // Fulfilled requests are decided on the day their linked donation was collected -
                // that is what fulfilment means, so any other date would contradict the donation.
                new RequestSeed("jane.doe@example.com", BloodGroup.A_POSITIVE, 1, "Sunnybrook Hospital",
                        "2075 Bayview Avenue, Toronto, ON", UrgencyLevel.HIGH, RequestStatus.FULFILLED,
                        25, 20, "Closed out after collection."),
                new RequestSeed("marcus.lee@example.com", BloodGroup.B_POSITIVE, 1, "St. Michael's Hospital",
                        "30 Bond Street, Toronto, ON", UrgencyLevel.MEDIUM, RequestStatus.APPROVED,
                        4, 3, null),
                new RequestSeed("aisha.khan@example.com", BloodGroup.AB_POSITIVE, 3, "Trillium Health Partners",
                        "100 Queensway West, Mississauga, ON", UrgencyLevel.LOW, RequestStatus.REJECTED,
                        9, 7, "Duplicate of an earlier request."),
                new RequestSeed("hana.park@example.com", BloodGroup.A_NEGATIVE, 2, "Toronto Western Hospital",
                        "399 Bathurst Street, Toronto, ON", UrgencyLevel.HIGH, RequestStatus.APPROVED,
                        6, 5, "Patient scheduled for transfusion."),
                new RequestSeed("aisha.khan@example.com", BloodGroup.O_POSITIVE, 2, "Trillium Health Partners",
                        "100 Queensway West, Mississauga, ON", UrgencyLevel.CRITICAL, RequestStatus.FULFILLED,
                        45, 45, null),
                new RequestSeed("mei.tanaka@example.com", BloodGroup.AB_NEGATIVE, 1, "Mount Sinai Hospital",
                        "600 University Avenue, Toronto, ON", UrgencyLevel.LOW, RequestStatus.REJECTED,
                        60, 58, "Requested group not required at this time."),
                // Index 9. Approved, and one of its three units already collected - the seeded
                // example of a partly filled request.
                new RequestSeed("kofi.mensah@example.com", BloodGroup.O_NEGATIVE, 3, "Humber River Hospital",
                        "1235 Wilson Avenue, North York, ON", UrgencyLevel.CRITICAL, RequestStatus.APPROVED,
                        2, 1, "Standing order for the trauma unit."),
                new RequestSeed("sofia.rossi@example.com", BloodGroup.A_POSITIVE, 1, "William Osler Health",
                        "2100 Bovaird Drive, Brampton, ON", UrgencyLevel.MEDIUM, RequestStatus.FULFILLED,
                        90, 90, null),
                new RequestSeed("liam.murphy@example.com", BloodGroup.O_POSITIVE, 2, "Michael Garron Hospital",
                        "825 Coxwell Avenue, Toronto, ON", UrgencyLevel.LOW, RequestStatus.PENDING,
                        7, 0, "Non-urgent, flexible on timing.")
        );
    }

    /** Spread across roughly a year, with several in the current month so the dashboard is alive. */
    private List<DonationSeed> donationRoster() {
        return List.of(
                // One unit against request 9, which needs three. That request stays APPROVED, so a
                // fresh database has a worked example of a partly filled request: its detail page
                // reads "1 of 3 collected" and it is still offered for fulfilment.
                //
                // Marcus is O-, and request 9 is for an O- patient, who can receive only O-. A
                // donor of any other group here would be refused by DonationService.
                new DonationSeed("marcus.lee@example.com", 1, 1, "Humber River Hospital", 9),
                new DonationSeed("priya.nair@example.com", 2, 2, "Mount Sinai Hospital", -1),
                new DonationSeed("liam.murphy@example.com", 4, 1, "Michael Garron Hospital", -1),
                new DonationSeed("kofi.mensah@example.com", 6, 2, "Humber River Hospital", -1),
                new DonationSeed("hana.park@example.com", 8, 1, "Toronto Western Hospital", -1),
                new DonationSeed("sofia.rossi@example.com", 11, 1, "William Osler Health", -1),
                new DonationSeed("rowan.avery@example.com", 14, 2, "Toronto General Hospital", -1),
                new DonationSeed("jane.doe@example.com", 20, 1, "Sunnybrook Hospital", 3),
                new DonationSeed("mei.tanaka@example.com", 27, 1, "Mount Sinai Hospital", -1),
                new DonationSeed("tomas.silva@example.com", 33, 2, "St. Michael's Hospital", -1),
                new DonationSeed("aisha.khan@example.com", 45, 2, "Trillium Health Partners", 7),
                new DonationSeed("marcus.lee@example.com", 52, 1, "St. Michael's Hospital", -1),
                new DonationSeed("daniel.okafor@example.com", 58, 1, "Hamilton General Hospital", -1),
                new DonationSeed("noah.bergman@example.com", 66, 2, "Markham Stouffville Hospital", -1),
                new DonationSeed("liam.murphy@example.com", 74, 1, "Michael Garron Hospital", -1),
                new DonationSeed("priya.nair@example.com", 81, 1, "Mount Sinai Hospital", -1),
                new DonationSeed("kofi.mensah@example.com", 88, 2, "Humber River Hospital", -1),
                new DonationSeed("sofia.rossi@example.com", 90, 1, "William Osler Health", 10),
                new DonationSeed("jane.doe@example.com", 110, 2, "Toronto General Hospital", -1),
                new DonationSeed("retired.donor@example.com", 130, 1, "Toronto General Hospital", -1),
                new DonationSeed("hana.park@example.com", 145, 1, "Toronto Western Hospital", -1),
                new DonationSeed("mei.tanaka@example.com", 160, 2, "Mount Sinai Hospital", -1),
                new DonationSeed("rowan.avery@example.com", 178, 1, "Toronto General Hospital", -1),
                new DonationSeed("marcus.lee@example.com", 195, 2, "St. Michael's Hospital", -1),
                new DonationSeed("daniel.okafor@example.com", 220, 1, "Hamilton General Hospital", -1),
                new DonationSeed("tomas.silva@example.com", 250, 1, "St. Michael's Hospital", -1),
                new DonationSeed("aisha.khan@example.com", 290, 2, "Trillium Health Partners", -1),
                new DonationSeed("noah.bergman@example.com", 330, 1, "Markham Stouffville Hospital", -1)
        );
    }

    // Derives each donor's lastDonationDate from the donations actually written, rather than making
    // it up separately - otherwise the profile column and the history below it could disagree.
    private void backfillLastDonationDates() {
        for (User donor : userRepository.findAll()) {
            List<Donation> donations = donationRepository.findByDonorOrderByDonationDateDesc(donor);
            if (!donations.isEmpty()) {
                donor.setLastDonationDate(donations.get(0).getDonationDate());
                userRepository.save(donor);
            }
        }
    }

    // Every seeded row references a donor by email, so a typo would otherwise produce a confusing
    // constraint violation later. Failing here says exactly which address is missing.
    private User required(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed user missing: " + email));
    }
}
