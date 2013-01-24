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
@DiscriminatorValue("EvenementFiscalLR")
public class EvenementFiscalLR extends EvenementFiscal {

	private static final long serialVersionUID = 3272714271175858488L;

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

	public EvenementFiscalLR() {
	}

	public EvenementFiscalLR(Tiers tiers, RegDate dateEvenement, TypeEvenementFiscal type, RegDate dateDebutPeriode, RegDate dateFinPeriode, Long numeroTechnique) {
		super(tiers, dateEvenement, type, numeroTechnique);
		Assert.isTrue(type == TypeEvenementFiscal.ANNULATION_LR || type == TypeEvenementFiscal.LR_MANQUANTE ||
				type == TypeEvenementFiscal.OUVERTURE_PERIODE_DECOMPTE_LR || type == TypeEvenementFiscal.RETOUR_LR ||
				type == TypeEvenementFiscal.SOMMATION_LR);
		this.dateDebutPeriode = dateDebutPeriode;
		this.dateFinPeriode = dateFinPeriode;
	}
}
