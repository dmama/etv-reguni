package ch.vd.uniregctb.mandataire;

import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.TiersService;

public class MandataireCourrierEditView extends MandataireCourrierView {

	private final boolean isAnnulable;
	private final boolean isEditable;

	public MandataireCourrierEditView(Mandat mandat, TiersService tiersService, ServiceInfrastructureService infraService, boolean modifiable) {
		super(mandat, tiersService, infraService);
		this.isAnnulable = !mandat.isAnnule() && modifiable;
		this.isEditable = !mandat.isAnnule() && modifiable;
	}

	public MandataireCourrierEditView(AdresseMandataire adresse, ServiceInfrastructureService infraService, boolean modifiable) {
		super(adresse, infraService);
		this.isAnnulable = !adresse.isAnnule() && modifiable;
		this.isEditable = !adresse.isAnnule() && modifiable;
	}

	public boolean isAnnulable() {
		return isAnnulable;
	}

	public boolean isEditable() {
		return isEditable;
	}
}
