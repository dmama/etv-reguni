package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@Entity
@DiscriminatorValue("ForFiscalPrincipalPM")
public class ForFiscalPrincipalPM extends ForFiscalPrincipal {

	public ForFiscalPrincipalPM() {
	}

	public ForFiscalPrincipalPM(RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, Integer numeroOfsAutoriteFiscale,
	                            TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement) {
		super(ouverture, motifOuverture, fermeture, motifFermeture, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifRattachement);
	}

	public ForFiscalPrincipalPM(ForFiscalPrincipalPM ffp) {
		super(ffp);
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.common.Duplicable#duplicate()
	 */
	@Override
	public ForFiscal duplicate() {
		return new ForFiscalPrincipalPM(this);
	}

	@Transient
	@Override
	public ContribuableImpositionPersonnesMorales getTiers() {
		return (ContribuableImpositionPersonnesMorales) super.getTiers();
	}

}
