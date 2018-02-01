package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.AllegementFiscal;

public class EditAllegementFiscalView implements DateRange {

	// pour le moment, seule la date de fin est éditable

	private long afId;
	private RegDate dateDebut;      // la date de début n'est pas éditable mais nécessaire pour la validation de la vue en retour
	private RegDate dateFin;

	public EditAllegementFiscalView() {
	}

	public EditAllegementFiscalView(AllegementFiscal af) {
		resetNonEditableValues(af);
		this.dateFin = af.getDateFin();
	}

	public final void resetNonEditableValues(AllegementFiscal af) {
		this.afId = af.getId();
		this.dateDebut = af.getDateDebut();
	}

	public long getAfId() {
		return afId;
	}

	public void setAfId(long afId) {
		this.afId = afId;
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