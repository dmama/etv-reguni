package ch.vd.uniregctb.evenement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

@Entity
@DiscriminatorValue("EvenementFiscalDI")
public class EvenementFiscalDI extends EvenementFiscal {

	private static final long serialVersionUID = -7310875537029248059L;

	private RegDate dateDebutPeriode;
	private RegDate dateFinPeriode;

	@Column(name = "DATE_DEBUT_PERIODE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebutPeriode() {
		return dateDebutPeriode;
	}

	public void setDateDebutPeriode(RegDate dateDebutPeriode) {
		this.dateDebutPeriode = dateDebutPeriode;
	}

	@Column(name = "DATE_FIN_PERIODE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFinPeriode() {
		return dateFinPeriode;
	}

	public void setDateFinPeriode(RegDate dateFinPeriode) {
		this.dateFinPeriode = dateFinPeriode;
	}

	public EvenementFiscalDI() {
	}

	public EvenementFiscalDI(Tiers tiers, RegDate dateEvenement, TypeEvenementFiscal type, RegDate dateDebutPeriode, RegDate dateFinPeriode, Long numeroTechnique) {
		super(tiers, dateEvenement, type, numeroTechnique);
		Assert.isTrue(type == TypeEvenementFiscal.ANNULATION_DI || type == TypeEvenementFiscal.ECHEANCE_DI || type == TypeEvenementFiscal.ENVOI_DI ||
				type == TypeEvenementFiscal.RETOUR_DI || type == TypeEvenementFiscal.SOMMATION_DI || type == TypeEvenementFiscal.TAXATION_OFFICE);
		this.dateDebutPeriode = dateDebutPeriode;
		this.dateFinPeriode = dateFinPeriode;
	}
}
