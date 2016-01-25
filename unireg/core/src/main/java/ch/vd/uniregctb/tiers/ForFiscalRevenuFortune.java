package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
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
public abstract class ForFiscalRevenuFortune extends ForFiscalAvecMotifs {

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8ilx9Edygsbnw9h5bVw"
	 */
	private MotifRattachement motifRattachement;

	public ForFiscalRevenuFortune() {
		setGenreImpot(GenreImpot.REVENU_FORTUNE);
	}

	public ForFiscalRevenuFortune(RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture,
	                              Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement) {
		super(ouverture, motifOuverture, fermeture, motifFermeture, GenreImpot.REVENU_FORTUNE, numeroOfsAutoriteFiscale, typeAutoriteFiscale);
		setMotifRattachement(motifRattachement); // virtual
	}

	public ForFiscalRevenuFortune(ForFiscalRevenuFortune ffrf) {
		super(ffrf);
		setMotifRattachement(ffrf.getMotifRattachement()); // virtual
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


	@Override
	protected void dumpForDebug(int nbTabs) {
		super.dumpForDebug(nbTabs);
		ddump(nbTabs, "Motif rattach: "+motifRattachement);
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
		if (motifRattachement == null) {
			if (other.motifRattachement != null)
				return false;
		} else if (motifRattachement != other.motifRattachement)
			return false;
		return true;
	}
}
