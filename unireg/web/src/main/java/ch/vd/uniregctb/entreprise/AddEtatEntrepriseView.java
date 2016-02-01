package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * @author Raphaël Marmier, 2016-01-22, <raphael.marmier@vd.ch>
 */
public class AddEtatEntrepriseView {

	private Long tiersId;
	private TypeEtatEntreprise type;
	private TypeEtatEntreprise previousType;
	private RegDate dateObtention;
	private RegDate previousDate;

	public AddEtatEntrepriseView() {}
	
	public AddEtatEntrepriseView(Entreprise entreprise) {
		tiersId = entreprise.getNumero();
		final EtatEntreprise etatActuel = entreprise.getEtatActuel();
		if (etatActuel != null) {
			previousType = etatActuel.getType();
			previousDate = etatActuel.getDateObtention();
		}
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public TypeEtatEntreprise getType() {
		return type;
	}

	public void setType(TypeEtatEntreprise type) {
		this.type = type;
	}

	public TypeEtatEntreprise getPreviousType() {
		return previousType;
	}

	public RegDate getDateObtention() {
		return dateObtention;
	}

	public void setPreviousType(TypeEtatEntreprise previousType) {
		this.previousType = previousType;
	}

	public void setDateObtention(RegDate dateObtention) {
		this.dateObtention = dateObtention;
	}

	public void setPreviousDate(RegDate previousDate) {
		this.previousDate = previousDate;
	}

	public RegDate getPreviousDate() {
		return previousDate;
	}
}
