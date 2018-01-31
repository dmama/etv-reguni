package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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
		 * @see ch.vd.uniregctb.common.Duplicable#duplicate()
		 */
	@Override
	public ForFiscal duplicate() {
		return new ForFiscalSecondaire(this);
	}

}
