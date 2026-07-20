package com.project.BloodBank.model.enums;

public enum RequestStatus {
    PENDING {
        @Override
        public RequestStatus transitionTo(RequestStatus next) {
            if (next == APPROVED || next == REJECTED) {
                return next;
            }
            throw new IllegalStateException(
                    "PENDING can only transition to APPROVED or REJECTED, not " + next
            );
        }
    },
    APPROVED {
        @Override
        public RequestStatus transitionTo(RequestStatus next) {
            if (next == FULFILLED) {
                return next;
            }
            throw new IllegalStateException(
                    "APPROVED can only transition to FULFILLED, not " + next
            );
        }
    },
    REJECTED {
        @Override
        public RequestStatus transitionTo(RequestStatus next) {
            throw new IllegalStateException("REJECTED is terminal—no transitions allowed");
        }
    },
    FULFILLED {
        @Override
        public RequestStatus transitionTo(RequestStatus next) {
            throw new IllegalStateException("FULFILLED is terminal—no transitions allowed");
        }
    };

    public abstract RequestStatus transitionTo(RequestStatus next);
}
