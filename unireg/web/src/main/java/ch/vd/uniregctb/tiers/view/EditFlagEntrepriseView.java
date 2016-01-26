package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;

public class EditFlagEntrepriseView {

	private long pmId;
	private long flagId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeFlagEntreprise value;

	public EditFlagEntrepriseView() {
	}

	public EditFlagEntrepriseView(FlagEntreprise flag) {
		this.flagId = flag.getId();
		this.pmId = flag.getEntreprise().getNumero();
		this.dateDebut = flag.getDateDebut();
		this.dateFin = flag.getDateFin();
		this.value = flag.getType();
	}

	public long getPmId() {
		return pmId;
	}

	public void setPmId(long pmId) {
		this.pmId = pmId;
	}

	public long getFlagId() {
		return flagId;
	}

	public void setFlagId(long flagId) {
		this.flagId = flagId;
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
