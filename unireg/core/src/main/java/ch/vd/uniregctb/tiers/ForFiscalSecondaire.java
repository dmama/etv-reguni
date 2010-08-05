package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Nido4BxAEd2SDKWRJy7Z3g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Nido4BxAEd2SDKWRJy7Z3g"
 */
@Entity
@DiscriminatorValue("ForFiscalSecondaire")
public class ForFiscalSecondaire extends ForFiscalRevenuFortune {

	private static final long serialVersionUID = -2008360913032572211L;

	public ForFiscalSecondaire() {
	}

	public ForFiscalSecondaire(RegDate ouverture, RegDate fermeture, Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
			MotifRattachement motifRattachement) {
		super(ouverture, fermeture, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifRattachement);
	}

	public ForFiscalSecondaire(ForFiscalSecondaire ffs) {
		super(ffs);
	}

	@Override
	public boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.ACTIVITE_INDEPENDANTE.equals(motif) || MotifRattachement.IMMEUBLE_PRIVE.equals(motif)
			|| MotifRattachement.SEJOUR_SAISONNIER.equals(motif) || MotifRattachement.DIRIGEANT_SOCIETE.equals(motif);
	}

	@Override
	public ValidationResults validate() {
		final ValidationResults results = super.validate();

		if (getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			results.addError("Par définition, le type d'autorité fiscale d'un for fiscal secondaire est limité à COMMUNE_OU_FRACTION_VD");
		}

		return results;
	}

	/* (non-Javadoc)
		 * @see ch.vd.uniregctb.common.Duplicable#duplicate()
		 */
	public ForFiscal duplicate() {
		return new ForFiscalSecondaire(this);
	}

}
