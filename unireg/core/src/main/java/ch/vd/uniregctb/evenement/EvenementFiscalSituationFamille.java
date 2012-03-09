package ch.vd.uniregctb.evenement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EvenementFiscalSituationFamille")
public class EvenementFiscalSituationFamille extends EvenementFiscal{

	private static final long serialVersionUID = 2038644150813882372L;

	public EvenementFiscalSituationFamille() {
	}

	public EvenementFiscalSituationFamille(Tiers tiers, RegDate dateEvenement, Long numeroTechnique) {
		super(tiers, dateEvenement, TypeEvenementFiscal.CHANGEMENT_SITUATION_FAMILLE, numeroTechnique);
	}
}
