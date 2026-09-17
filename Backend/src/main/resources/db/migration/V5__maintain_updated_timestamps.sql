CREATE OR REPLACE FUNCTION maintain_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER makes_maintain_updated_at
    BEFORE UPDATE ON makes
    FOR EACH ROW EXECUTE FUNCTION maintain_updated_at();

CREATE TRIGGER vehicle_models_maintain_updated_at
    BEFORE UPDATE ON vehicle_models
    FOR EACH ROW EXECUTE FUNCTION maintain_updated_at();

CREATE TRIGGER vehicle_generations_maintain_updated_at
    BEFORE UPDATE ON vehicle_generations
    FOR EACH ROW EXECUTE FUNCTION maintain_updated_at();

CREATE TRIGGER engines_maintain_updated_at
    BEFORE UPDATE ON engines
    FOR EACH ROW EXECUTE FUNCTION maintain_updated_at();

CREATE TRIGGER transmissions_maintain_updated_at
    BEFORE UPDATE ON transmissions
    FOR EACH ROW EXECUTE FUNCTION maintain_updated_at();

CREATE TRIGGER vehicle_trims_maintain_updated_at
    BEFORE UPDATE ON vehicle_trims
    FOR EACH ROW EXECUTE FUNCTION maintain_updated_at();

CREATE TRIGGER vehicle_specifications_maintain_updated_at
    BEFORE UPDATE ON vehicle_specifications
    FOR EACH ROW EXECUTE FUNCTION maintain_updated_at();
