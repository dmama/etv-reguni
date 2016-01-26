package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeFlagEntreprise;

public class AddFlagEntrepriseView {

	private long pmId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeFlagEntreprise value;

	public AddFlagEntrepriseView() {
	}

	public AddFlagEntrepriseView(long pmId) {
		this.pmId = pmId;
	}

	public long getPmId() {
		return pmId;
	}

	public void setPmId(long pmId) {
		this.pmId = pmId;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public TypeFlagEntreprise getValue() {
		return value;
	}

	public void setValue(TypeFlagEntreprise value) {
		this.value = value;
	}
}
