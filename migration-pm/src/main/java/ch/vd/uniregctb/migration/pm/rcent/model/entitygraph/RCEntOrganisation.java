package ch.vd.uniregctb.migration.pm.rcent.model.entitygraph;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntHistoryList;
import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntOrganisationHistoryElement;

public class RCEntOrganisation extends RCEntEntity implements RCEntEntityHistory {

	private final RCEntHistoryList<RCEntOrganisationHistoryElement> history;

	private final Long cantonalId;

	/**
	 * Commune de siège légal
	 */
	private final RCEntSwissMunicipality seat;

	private final RCEntOrganisationLocation etablissementPrincipal;
	private final List<RCEntOrganisationLocation> etablissementsSecondaires;

	public RCEntOrganisation(RCEntHistoryList<RCEntOrganisationHistoryElement> history, Long cantonalId, RCEntSwissMunicipality seat,
	                         RCEntOrganisationLocation etablissementPrincipal, List<RCEntOrganisationLocation> etablissementsSecondaires) {
		this.history = history;
		this.cantonalId = cantonalId;
		this.seat = seat;
		this.etablissementPrincipal = etablissementPrincipal;
		this.etablissementsSecondaires = etablissementsSecondaires;
	}

	@NotNull
	@Override
	public RCEntHistoryList<RCEntOrganisationHistoryElement> getHistory() {
		return history;
	}

	@Override
	public Object getCurrent() {
		return null;
	}


}
