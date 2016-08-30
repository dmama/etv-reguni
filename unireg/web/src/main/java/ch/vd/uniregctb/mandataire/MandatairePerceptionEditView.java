package ch.vd.uniregctb.mandataire;

import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.TiersService;

public class MandatairePerceptionEditView extends MandatairePerceptionView {

	private final boolean isAnnulable;
	private final boolean isEditable;

	public MandatairePerceptionEditView(Mandat mandat, TiersService tiersService, boolean modifiable) {
		super(mandat, tiersService);
		this.isAnnulable = !mandat.isAnnule() && modifiable;
		this.isEditable = !mandat.isAnnule() && modifiable;
	}

	public boolean isAnnulable() {
		return isAnnulable;
	}

	public boolean isEditable() {
		return isEditable;
	}
}
