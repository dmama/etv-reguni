package ch.vd.uniregctb.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@DiscriminatorValue(value = "Capital")
public class CapitalFiscalEntreprise extends DonneeCivileEntreprise {

	private MontantMonetaire montant;

	/**
	 * Nécessaire pour Hibernate (et SuperGRA...)
	 */
	public CapitalFiscalEntreprise() {
	}

	public CapitalFiscalEntreprise(RegDate dateDebut, RegDate dateFin, MontantMonetaire montant) {
		super(dateDebut, dateFin);
		this.montant = montant;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "montant", column = @Column(name = "CAP_MONTANT")),
			@AttributeOverride(name = "monnaie", column = @Column(name = "CAP_MONNAIE", length = LengthConstants.MONNAIE_ISO))})
	public MontantMonetaire getMontant() {
		return montant;
	}

	public void setMontant(MontantMonetaire montant) {
		this.montant = montant;
	}
}
