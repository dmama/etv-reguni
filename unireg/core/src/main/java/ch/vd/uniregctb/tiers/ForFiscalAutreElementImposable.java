package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.utils.Assert;
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

	@Transient
	@Override
	public void setMotifRattachement(MotifRattachement motifRattachement) {
		Assert.isTrue(isRattachementCoherent(motifRattachement),
				"Par définition, le motif de rattachement d'un for fiscal 'autre élément imposable' est limité à ACTIVITE_LUCRATIVE_CAS, ADMINISTRATEUR, CREANCIER_HYPOTHECAIRE et BENEFICIAIRE_PRESTATION_PREVOYANCE.");
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
		Assert.isEqual(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, theTypeAutoriteFiscaleFiscale, "Par définition, le type d'autorité fiscale d'un for fiscal 'autre élément imposable' est limité à COMMUNE_OU_FRACTION_VD");
	}

	@Override
	public boolean isRattachementCoherent(MotifRattachement motif) {
		boolean valid = MotifRattachement.ACTIVITE_LUCRATIVE_CAS.equals(motif)
		|| MotifRattachement.ADMINISTRATEUR.equals(motif)
		|| MotifRattachement.CREANCIER_HYPOTHECAIRE.equals(motif)
		|| MotifRattachement.PRESTATION_PREVOYANCE.equals(motif)
		|| MotifRattachement.LOI_TRAVAIL_AU_NOIR.equals(motif);
		return valid;
	}

	public ForFiscal duplicate() {
		return new ForFiscalAutreElementImposable(this);
	}

}
