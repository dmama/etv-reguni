package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
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


	@Transient
	@Override
	public void setMotifRattachement(MotifRattachement motifRattachement) {
		Assert.isTrue(isRattachementCoherent(motifRattachement),
				"Par définition, le motif de rattachement d'un for fiscal secondaire est limité à " +
				"ACTIVITE_INDEPENDANTE, IMMEUBLE_PRIVE, SEJOUR_SAISONNIER et DIRIGEANT_SOCIETE.");
		super.setMotifRattachement(motifRattachement);
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
		Assert.isEqual(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, theTypeAutoriteFiscaleFiscale,
				"Par définition, le type d'autorité fiscale d'un for fiscal secondaire est limité à COMMUNE_OU_FRACTION_VD");
		super.setTypeAutoriteFiscale(theTypeAutoriteFiscaleFiscale);
	}

	@Override
	public boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.ACTIVITE_INDEPENDANTE.equals(motif) || MotifRattachement.IMMEUBLE_PRIVE.equals(motif)
			|| MotifRattachement.SEJOUR_SAISONNIER.equals(motif) || MotifRattachement.DIRIGEANT_SOCIETE.equals(motif);
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.common.Duplicable#duplicate()
	 */
	public ForFiscal duplicate() {
		return new ForFiscalSecondaire(this);
	}

}
