package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_QGWdIEE7Ed2XrapGHNAWZw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_QGWdIEE7Ed2XrapGHNAWZw"
 */
@Entity
@DiscriminatorValue("ForDebiteurPrestationImposable")
public class ForDebiteurPrestationImposable extends ForFiscal {

	private static final long serialVersionUID = -6011848231879692380L;

	public ForDebiteurPrestationImposable() {
		setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
	}

	public ForDebiteurPrestationImposable(ForDebiteurPrestationImposable fdpi) {
		super(fdpi);
	}

	@Override
	public ValidationResults validate() {

		ValidationResults results = super.validate();

		if (getGenreImpot() != GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) {
			results.addError("Par définition, le genre d'impôt d'un for fiscal 'débiteur prestation imposable' doit être DEBITEUR_PRESTATION_IMPOSABLE.");
		}

		if (getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_HC) {
			results.addError("Par définition, le type d'autorité fiscale d'un for fiscal 'débiteur prestation imposable' est limité à COMMUNE_OU_FRACTION_VD ou COMMUNE_HC");
		}

		return results;
	}

	@Transient
	@Override
	public boolean isDebiteur() {
		return true;
	}

	public ForFiscal duplicate() {
		return new ForDebiteurPrestationImposable(this);
	}
}
