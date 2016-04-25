package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

@Entity
@DiscriminatorValue(value = "FormeJuridique")
public class FormeJuridiqueFiscaleEntreprise extends DonneeCivileEntreprise implements Duplicable<FormeJuridiqueFiscaleEntreprise> {

	private FormeJuridiqueEntreprise formeJuridique;

	/**
	 * Nécessaire pour Hibernate (et SuperGRA...)
	 */
	public FormeJuridiqueFiscaleEntreprise() {
	}

	public FormeJuridiqueFiscaleEntreprise(RegDate dateDebut, RegDate dateFin, FormeJuridiqueEntreprise formeJuridique) {
		super(dateDebut, dateFin);
		this.formeJuridique = formeJuridique;
	}

	public FormeJuridiqueFiscaleEntreprise(FormeJuridiqueFiscaleEntreprise source) {
		super(source.getDateDebut(), source.getDateFin());
		this.formeJuridique = source.getFormeJuridique();
	}

	@Column(name = "FJ_FORME_JURIDIQUE", length = LengthConstants.PM_FORME)
	@Enumerated(EnumType.STRING)
	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	@Override
	public FormeJuridiqueFiscaleEntreprise duplicate() {
		return new FormeJuridiqueFiscaleEntreprise(this);
	}
}
