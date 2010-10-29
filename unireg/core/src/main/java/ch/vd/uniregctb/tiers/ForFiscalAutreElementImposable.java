package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Tg9noBxAEd2SDKWRJy7Z3g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Tg9noBxAEd2SDKWRJy7Z3g"
 */
@Entity
@DiscriminatorValue("ForFiscalAutreElementImposable")
public class ForFiscalAutreElementImposable extends ForFiscalRevenuFortune {

	private static final long serialVersionUID = -2847235949313474922L;

	public ForFiscalAutreElementImposable() {
	}

	public ForFiscalAutreElementImposable(ForFiscalAutreElementImposable ffaei) {
		super(ffaei);
	}

	@Override
	public boolean isRattachementCoherent(MotifRattachement motif) {
		boolean valid = MotifRattachement.ACTIVITE_LUCRATIVE_CAS == motif
		|| MotifRattachement.ADMINISTRATEUR == motif
		|| MotifRattachement.CREANCIER_HYPOTHECAIRE == motif
		|| MotifRattachement.PRESTATION_PREVOYANCE == motif
		|| MotifRattachement.LOI_TRAVAIL_AU_NOIR == motif;
		return valid;
	}

	@Override
	public ValidationResults validate() {
		final ValidationResults results = super.validate();

		if (isAnnule()) {
			return results;
		}

		if (getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			results.addError("Par définition, le type d'autorité fiscale d'un for fiscal 'autre élément imposable' est limité à COMMUNE_OU_FRACTION_VD");
		}

		return results;
	}

	public ForFiscal duplicate() {
		return new ForFiscalAutreElementImposable(this);
	}

}
