package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.FlagEntreprise;

public class EditFlagEntrepriseView implements DateRange {

	private long flagId;
	private RegDate dateDebut;          // la date de début est là aussi pour valider les valeurs de la date de fin
	private RegDate dateFin;

	public EditFlagEntrepriseView() {
	}

	public EditFlagEntrepriseView(FlagEntreprise flag) {
		resetNonEditableValues(flag);
		this.dateFin = flag.getDateFin();
	}

	public final void resetNonEditableValues(FlagEntreprise flag) {
		this.flagId = flag.getId();
		this.dateDebut = flag.getDateDebut();
	}

	public long getFlagId() {
		return flagId;
	}

	public void setFlagId(long flagId) {
		this.flagId = flagId;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}
}
