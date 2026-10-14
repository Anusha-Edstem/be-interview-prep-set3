CREATE TABLE doctor (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    specialty VARCHAR(120) NOT NULL
);

CREATE TABLE slot (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(16) NOT NULL,
    patient_id VARCHAR(64),
    hold_expires_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_slot_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT uk_slot_doctor_start UNIQUE (doctor_id, start_time),
    CONSTRAINT ck_slot_status CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED'))
);

CREATE INDEX idx_slot_status_hold_expires ON slot (status, hold_expires_at);

INSERT INTO doctor (id, name, specialty) VALUES
    ('6f1c2a3e-4b5d-4e6f-8a7b-0c1d2e3f4a51', 'Dr. Asha Menon', 'General Practice'),
    ('6f1c2a3e-4b5d-4e6f-8a7b-0c1d2e3f4a52', 'Dr. Rahul Varma', 'Cardiology'),
    ('6f1c2a3e-4b5d-4e6f-8a7b-0c1d2e3f4a53', 'Dr. Meera Nair', 'Dermatology');
