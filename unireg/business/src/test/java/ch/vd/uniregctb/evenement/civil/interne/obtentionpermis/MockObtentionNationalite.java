package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Bouchon pour un événement de obtention de nationalité
 *
 * @author Ludovic Bertin
 */
public class MockObtentionNationalite extends MockEvenementCivil implements ObtentionNationalite {

	private Integer numeroOfsEtenduCommunePrincipale;

	public MockObtentionNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Integer numeroOfsEtenduCommunePrincipale, boolean nationaliteSuisse) {
		super(individu, conjoint, nationaliteSuisse ? TypeEvenementCivil.NATIONALITE_SUISSE : TypeEvenementCivil.NATIONALITE_NON_SUISSE, date, numeroOfsCommuneAnnonce);
		this.numeroOfsEtenduCommunePrincipale = numeroOfsEtenduCommunePrincipale;
	}

	public boolean isContribuablePresentBefore() {
		return false;
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}
}
