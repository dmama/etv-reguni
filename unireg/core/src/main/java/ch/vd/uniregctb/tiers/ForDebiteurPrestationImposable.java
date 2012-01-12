package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.uniregctb.type.GenreImpot;

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

	@Transient
	@Override
	public boolean isDebiteur() {
		return true;
	}

	@Override
	public ForFiscal duplicate() {
		return new ForDebiteurPrestationImposable(this);
	}
}
