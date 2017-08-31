package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;

public interface AffaireRFAudit {

	void addUntouched(@NotNull DroitProprieteRF droit);

	void addCreated(DroitProprieteRF droit);

	void addUpdated(@NotNull DroitProprieteRF droit, @Nullable RegDate dateDebutMetierPrecedente, @Nullable String motifDebutPrecedent);
}
