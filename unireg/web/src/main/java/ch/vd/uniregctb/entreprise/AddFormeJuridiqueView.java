package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

public class AddFormeJuridiqueView implements DateRange {

	private Long tiersId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private FormeJuridiqueEntreprise formeJuridique;

	public AddFormeJuridiqueView() {}

	public AddFormeJuridiqueView(Long tiersId, RegDate dateDebut, RegDate dateFin, FormeJuridiqueEntreprise formeJuridique) {
		this.tiersId = tiersId;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.formeJuridique = formeJuridique;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
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

	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

}
