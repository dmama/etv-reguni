package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.utils.Assert;
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

	/**
	 *
	 */
	private static final long serialVersionUID = -6011848231879692380L;

	public ForDebiteurPrestationImposable() {
	}

	public ForDebiteurPrestationImposable(ForDebiteurPrestationImposable fdpi) {
		super(fdpi);
	}

	@Transient
	@Override
	public GenreImpot getGenreImpot() {
		// Par définition
		return GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE;
	}

	@Transient
	@Override
	public void setGenreImpot(GenreImpot theGenreImpot) {
		Assert.isEqual(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, theGenreImpot,
				"Par définition, le genre d'impôt d'un for fiscal 'débiteur prestation imposable' doit être DEBITEUR_PRESTATION_IMPOSABLE.");
	}


	@Transient
	@Override
	public void setTypeAutoriteFiscale(TypeAutoriteFiscale theTypeAutoriteFiscaleFiscale) {
		Assert.isTrue(theTypeAutoriteFiscaleFiscale.equals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
				|| theTypeAutoriteFiscaleFiscale.equals(TypeAutoriteFiscale.COMMUNE_HC),
				"Par définition, le type d'autorité fiscale d'un for fiscal 'débiteur prestation imposable' est limité à COMMUNE_OU_FRACTION_VD"
						+ "ou COMMUNE_HC");
		super.setTypeAutoriteFiscale(theTypeAutoriteFiscaleFiscale);
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
