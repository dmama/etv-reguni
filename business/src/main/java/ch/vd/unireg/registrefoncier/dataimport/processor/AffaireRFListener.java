package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;

public interface AffaireRFListener {

	void onCreation(DroitProprieteRF droit);

	void onUpdateDateDebut(@NotNull DroitProprieteRF droit, @Nullable RegDate dateDebutMetierInitiale, @Nullable String motifDebutInitial);

	void onUpdateDateFin(@NotNull DroitProprieteRF droit, @Nullable RegDate dateFinMetierInitiale, @Nullable String motifFinInitial);

	void onOtherUpdate(@NotNull DroitProprieteRF droit);

	void onClosing(@NotNull DroitProprieteRF droit);
}
