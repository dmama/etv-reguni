package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@Entity
@DiscriminatorValue(value = "AbstractForFiscalPrincipal")       // nécessaire tant que le nom de cette classe est utilisé comme discriminant pour une autre classe
public abstract class ForFiscalPrincipal extends ForFiscalRevenuFortune {

	public ForFiscalPrincipal() {
	}

	public ForFiscalPrincipal(RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, Integer numeroOfsAutoriteFiscale,
	                          TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement) {
		super(ouverture, motifOuverture, fermeture, motifFermeture, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifRattachement);
	}

	public ForFiscalPrincipal(ForFiscalPrincipal ffp) {
		super(ffp);
	}

	@Transient
	@Override
	public boolean isPrincipal() {
		return true;
	}

	@Transient
	@Override
	public Contribuable getTiers() {
		return (Contribuable) super.getTiers();
	}
}
