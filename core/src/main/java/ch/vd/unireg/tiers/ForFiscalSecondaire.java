package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@Entity
@DiscriminatorValue("ForFiscalSecondaire")
public class ForFiscalSecondaire extends ForFiscalRevenuFortune {

	public ForFiscalSecondaire() {
	}

	public ForFiscalSecondaire(RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement) {
		super(ouverture, motifOuverture, fermeture, motifFermeture, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifRattachement);
	}

	public ForFiscalSecondaire(ForFiscalSecondaire ffs) {
		super(ffs);
	}

	/* (non-Javadoc)
		 * @see ch.vd.unireg.common.Duplicable#duplicate()
		 */
	@Override
	public ForFiscal duplicate() {
		return new ForFiscalSecondaire(this);
	}

}
