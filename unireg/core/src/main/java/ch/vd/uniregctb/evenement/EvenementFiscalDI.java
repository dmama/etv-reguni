package ch.vd.uniregctb.evenement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeEvenementFiscal;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

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
		Assert.isTrue(type.equals(TypeEvenementFiscal.ANNULATION_DI) || type.equals(TypeEvenementFiscal.ECHEANCE_DI) || type.equals(TypeEvenementFiscal.ENVOI_DI) ||
				type.equals(TypeEvenementFiscal.RETOUR_DI) || type.equals(TypeEvenementFiscal.SOMMATION_DI) || type.equals(TypeEvenementFiscal.TAXATION_OFFICE));
		this.dateDebutPeriode = dateDebutPeriode;
		this.dateFinPeriode = dateFinPeriode;
	}
}
