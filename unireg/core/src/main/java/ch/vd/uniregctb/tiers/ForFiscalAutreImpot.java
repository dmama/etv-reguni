package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.utils.Assert;
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

	@Transient
	@Override
	public void setGenreImpot(GenreImpot theGenreImpot) {
		Assert.isTrue(!GenreImpot.REVENU_FORTUNE.equals(theGenreImpot),
			"Par définition, le genre d'impôt d'un for fiscal 'autre impôt' doit être différent de REVENU_FORTUNE.");
		Assert.isTrue(!GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE.equals(theGenreImpot),
			"Par définition, le genre d'impôt d'un for fiscal 'autre impôt' doit être différent de DEBITEUR_PRESTATION_IMPOSABLE.");
		super.setGenreImpot(theGenreImpot);
	}

	@Transient
	@Override
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		// Par définition
		return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Transient
	@Override
	public void setTypeAutoriteFiscale(TypeAutoriteFiscale theTypeAutoriteFiscaleFiscale) {
		Assert.isEqual(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, theTypeAutoriteFiscaleFiscale, "Par définition, le type d'autorité fiscale d'un for fiscal 'autre impôt' est limité à COMMUNE_OU_FRACTION_VD");
	}

	public ForFiscal duplicate() {
		return new ForFiscalAutreImpot(this);
	}

}
