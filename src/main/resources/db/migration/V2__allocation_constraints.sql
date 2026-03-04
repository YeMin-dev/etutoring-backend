-- V2__allocation_constraints.sql
-- Add constraints and indexes for allocations table

-- Partial unique index to ensure each student has only one active allocation
CREATE UNIQUE INDEX IF NOT EXISTS ux_allocations_active_student
ON allocations (student_id)
WHERE deleted_date IS NULL;

-- Index on allocated_date for potential queries by allocation date
CREATE INDEX IF NOT EXISTS ix_allocations_allocated_date
ON allocations (allocated_date);