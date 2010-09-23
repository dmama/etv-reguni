package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 *
 * @author msi
 *
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IJGfIF-hEdyCxumqfWBxMQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IJGfIF-hEdyCxumqfWBxMQ"
 */
@Entity
public abstract class ForFiscalRevenuFortune extends ForFiscal {

	private static final long serialVersionUID = 6699182098456063372L;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8ilx9Edygsbnw9h5bVw"
	 */
	private MotifRattachement motifRattachement;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_2oQGUPY0Edyw0I40oDFBsg"
	 */
	private MotifFor motifOuverture;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_-4CyQPY0Edyw0I40oDFBsg"
	 */
	private MotifFor motifFermeture;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_d6IWsPBVEdyG2_4LrN35Ag"
	 */
	private Boolean forGestion;

	public ForFiscalRevenuFortune() {
		setGenreImpot(GenreImpot.REVENU_FORTUNE);
	}

	public ForFiscalRevenuFortune(RegDate ouverture, RegDate fermeture, Integer numeroOfsAutoriteFiscale,
			TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement) {
		super(ouverture, fermeture, GenreImpot.REVENU_FORTUNE, numeroOfsAutoriteFiscale, typeAutoriteFiscale);
		setMotifRattachement(motifRattachement); // virtual
	}

	public ForFiscalRevenuFortune(ForFiscalRevenuFortune ffrf) {
		this(ffrf.getDateDebut(), ffrf.getDateFin(), ffrf.getNumeroOfsAutoriteFiscale(), ffrf.getTypeAutoriteFiscale(), ffrf.getMotifRattachement());
		this.setMotifOuverture(ffrf.getMotifOuverture());
		this.setMotifFermeture(ffrf.getMotifFermeture());
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the motifRattachement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8ilx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "MOTIF_RATTACHEMENT", length = LengthConstants.FOR_RATTACHEMENT)
	@Type(type = "ch.vd.uniregctb.hibernate.MotifRattachementUserType")
	public MotifRattachement getMotifRattachement() {
		// begin-user-code
		return motifRattachement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theMotifRattachement
	 *            the motifRattachement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8ilx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setMotifRattachement(MotifRattachement theMotifRattachement) {
		// begin-user-code
		motifRattachement = theMotifRattachement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the motifOuverture
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_2oQGUPY0Edyw0I40oDFBsg?GETTER"
	 */
	@Column(name = "MOTIF_OUVERTURE", length = LengthConstants.FOR_MOTIF)
	@Type(type = "ch.vd.uniregctb.hibernate.MotifForUserType")
	public MotifFor getMotifOuverture() {
		// begin-user-code
		return motifOuverture;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theMotifOuverture
	 *            the motifOuverture to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_2oQGUPY0Edyw0I40oDFBsg?SETTER"
	 */
	public void setMotifOuverture(MotifFor theMotifOuverture) {
		// begin-user-code
		motifOuverture = theMotifOuverture;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the motifFermeture
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_-4CyQPY0Edyw0I40oDFBsg?GETTER"
	 */
	@Column(name = "MOTIF_FERMETURE", length = LengthConstants.FOR_MOTIF)
	@Type(type = "ch.vd.uniregctb.hibernate.MotifForUserType")
	public MotifFor getMotifFermeture() {
		// begin-user-code
		return motifFermeture;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theMotifFermeture
	 *            the motifFermeture to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_-4CyQPY0Edyw0I40oDFBsg?SETTER"
	 */
	public void setMotifFermeture(MotifFor theMotifFermeture) {
		// begin-user-code
		motifFermeture = theMotifFermeture;
		// end-user-code
	}

	public abstract boolean isRattachementCoherent(MotifRattachement motif);

	@Override
	protected void dumpForDebug(int nbTabs) {
		super.dumpForDebug(nbTabs);

		ddump(nbTabs, "Motif rattach: "+motifRattachement);
		ddump(nbTabs, "Motif ouv: "+motifOuverture);
		ddump(nbTabs, "Motif ferm: "+motifFermeture);
	}

	@Override
	public ValidationResults validate() {

		ValidationResults results = super.validate();

		if (isAnnule()) {
			return results;
		}

		if (getGenreImpot() != GenreImpot.REVENU_FORTUNE) {
			results.addError("Par définition, le genre d'impôt d'un for fiscal 'revenu-fortune' doit être REVENU_FORTUNE.");
		}

		if (!isRattachementCoherent(motifRattachement)){
			results.addError("Le for " + toString() + " avec motif de rattachement = " + motifRattachement + " est invalide");
		}

		if (getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			if (motifOuverture == null) {
				results.addError("Le motif d'ouverture est obligatoire sur le for fiscal [" + this
						+ "] car il est rattaché à une commune vaudoise.");
			}
			if (motifFermeture == null && getDateFin() != null) {
				results.addError("Le motif de fermeture est obligatoire sur le for fiscal [" + this
						+ "] car il est rattaché à une commune vaudoise.");
			}
		}

		return results;
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.tiers.ForFiscal#equalsTo(java.lang.Object)
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
		final ForFiscalRevenuFortune other = (ForFiscalRevenuFortune) obj;
		if (forGestion == null) {
			if (other.forGestion != null)
				return false;
		} else if (!forGestion.equals(other.forGestion))
			return false;
		if (motifFermeture == null) {
			if (other.motifFermeture != null)
				return false;
		} else if (!motifFermeture.equals(other.motifFermeture))
			return false;
		if (motifOuverture == null) {
			if (other.motifOuverture != null)
				return false;
		} else if (!motifOuverture.equals(other.motifOuverture))
			return false;
		if (motifRattachement == null) {
			if (other.motifRattachement != null)
				return false;
		} else if (!motifRattachement.equals(other.motifRattachement))
			return false;
		return true;
	}


}
