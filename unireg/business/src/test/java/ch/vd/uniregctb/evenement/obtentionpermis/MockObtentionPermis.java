package ch.vd.uniregctb.evenement.obtentionpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Bouchon pour un événement de typeNaissance
 *
 * @author Ludovic Bertin
 */
public class MockObtentionPermis extends MockEvenementCivil implements ObtentionPermis {

	private Integer numeroOfsEtenduCommunePrincipale;
	private TypePermis typePermis;

	public MockObtentionPermis(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Integer numeroOfsEtenduCommunePrincipale,
	                           TypePermis typePermis) {
		super(individu, conjoint, TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, date, numeroOfsCommuneAnnonce);
		this.numeroOfsEtenduCommunePrincipale = numeroOfsEtenduCommunePrincipale;
		this.typePermis = typePermis;
	}

	public boolean isContribuablePresentBefore() {
		return false;
	}

	public TypePermis getTypePermis() {
		return this.typePermis;
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}
}
