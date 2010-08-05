package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 *
 * @author msi
 *
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_fm5woBw-Ed2SDKWRJy7Z3g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_fm5woBw-Ed2SDKWRJy7Z3g"
 */
@Entity
@DiscriminatorValue("ForFiscalPrincipal")
public class ForFiscalPrincipal extends ForFiscalRevenuFortune {

	private static final long serialVersionUID = -4854391097242110206L;

	protected static final String[] VALIDATION_ERROR_CODES =  new String[] {
			"[ERR_VALIDATE_FOR_FISCAL_PRINCIPAL_0]",
			"[ERR_VALIDATE_FOR_FISCAL_PRINCIPAL_1]"
	};

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Rôle ordinaire (ICC/IFD)
	 * Imposé d'après la dépense (ICCD/IFDD)
	 * IS seulement
	 * IS mixte selon loi
	 * IS mixte selon pratique
	 * Taxé à zéro (indigent)
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8iVx9Edygsbnw9h5bVw"
	 */
	private ModeImposition modeImposition;

	public ForFiscalPrincipal() {
	}

	public ForFiscalPrincipal(RegDate ouverture, RegDate fermeture, Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
			MotifRattachement motifRattachement, ModeImposition modeImposition) {
		super(ouverture, fermeture, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifRattachement);
		this.modeImposition = modeImposition;
	}

	public ForFiscalPrincipal(ForFiscalPrincipal ffp) {
		super(ffp);
		this.modeImposition = ffp.getModeImposition();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the modeImposition
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8iVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "MODE_IMPOSITION", length = LengthConstants.FOR_IMPOSITION)
	@Type(type = "ch.vd.uniregctb.hibernate.ModeImpositionUserType")
	public ModeImposition getModeImposition() {
		// begin-user-code
		return modeImposition;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theModeImposition
	 *            the modeImposition to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8iVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setModeImposition(ModeImposition theModeImposition) {
		// begin-user-code
		modeImposition = theModeImposition;
		// end-user-code
	}

	@Transient
	@Override
	public boolean isPrincipal() {
		return true;
	}

	@Override
	protected void dumpForDebug(int nbTabs) {
		super.dumpForDebug(nbTabs);
		ddump(nbTabs, "Mode imposition: "+modeImposition);
	}

	@Override
	public boolean isRattachementCoherent(MotifRattachement motif) {
		boolean valid = MotifRattachement.DOMICILE.equals(motif) || MotifRattachement.DIPLOMATE_SUISSE.equals(motif) || MotifRattachement.DIPLOMATE_ETRANGER.equals(motif);
		return valid;
	}

	@Override
	public ValidationResults validate() {
		return validate(false);
	}

	ValidationResults validate(boolean appendErrorCode) {

		ValidationResults results = super.validate();
		if (modeImposition == null) {
			results.addError(
					"Le mode d'imposition est obligatoire sur un for fiscal principal."
					+ (appendErrorCode ? VALIDATION_ERROR_CODES[0] : "")
			);
		}
		if (MotifRattachement.DOMICILE.equals(getMotifRattachement())
				&& !TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(getTypeAutoriteFiscale())) {
			if (!ModeImposition.ORDINAIRE.equals(modeImposition)
					&& !ModeImposition.SOURCE.equals(modeImposition)
					&& !ModeImposition.MIXTE_137_1.equals(modeImposition)) {
				results.addError(
						"Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger, " +
						"les modes d'imposition possibles sont \"ordinaire\", \"source\" ou \"mixte 137 al1\". " +
						(appendErrorCode ? VALIDATION_ERROR_CODES[1] : "")
				);
			}
		}
		return results;
	}



	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.tiers.ForFiscalRevenuFortune#equalsTo(java.lang.Object)
	 */
	@Override
	public boolean equalsTo(Object obj) {
		if (!super.equalsTo(obj))
			return false;
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ForFiscalPrincipal other = (ForFiscalPrincipal) obj;
		if (modeImposition == null) {
			if (other.modeImposition != null)
				return false;
		} else if (!modeImposition.equals(other.modeImposition))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.common.Duplicable#duplicate()
	 */
	public ForFiscal duplicate() {
		return new ForFiscalPrincipal(this);
	}

}
