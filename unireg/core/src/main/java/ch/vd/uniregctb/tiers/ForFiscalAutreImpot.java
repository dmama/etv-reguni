package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XYLmQBw_Ed2SDKWRJy7Z3g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XYLmQBw_Ed2SDKWRJy7Z3g"
 */
@Entity
@DiscriminatorValue("ForFiscalAutreImpot")
public class ForFiscalAutreImpot extends ForFiscal {

	private static final long serialVersionUID = 2472800148068209440L;

	public ForFiscalAutreImpot() {
	}

	public ForFiscalAutreImpot(ForFiscalAutreImpot ffai) {
		super(ffai);
	}

	@Override
	public ValidationResults validate() {

		ValidationResults results = super.validate();

		if (isAnnule()) {
			return results;
		}
		
		if (getGenreImpot() == GenreImpot.REVENU_FORTUNE) {
			results.addError("Par définition, le genre d'impôt d'un for fiscal 'autre impôt' doit être différent de REVENU_FORTUNE.");
		}

		if (getGenreImpot() == GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) {
			results.addError("Par définition, le genre d'impôt d'un for fiscal 'autre impôt' doit être différent de DEBITEUR_PRESTATION_IMPOSABLE.");
		}

		if (getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			results.addError("Par définition, le type d'autorité fiscale d'un for fiscal 'autre impôt' est limité à COMMUNE_OU_FRACTION_VD");
		}

		return results;
	}

	public ForFiscal duplicate() {
		return new ForFiscalAutreImpot(this);
	}

}
