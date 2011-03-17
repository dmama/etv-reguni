package ch.vd.uniregctb.evenement.obtentionpermis;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Bouchon pour un événement de typeNaissance
 *
 * @author Ludovic Bertin
 */
public class MockObtentionPermis extends MockEvenementCivil implements ObtentionPermis {

	private Integer numeroOfsEtenduCommunePrincipale;
	private TypePermis typePermis;

	public boolean isContribuablePresentBefore() {
		return false;
	}

	public void setTypePermis(TypePermis typePermis) {
		this.typePermis = typePermis;
	}

	public TypePermis getTypePermis() {
		return this.typePermis;
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}

	public void setNumeroOfsEtenduCommunePrincipale(Integer numeroOfsEtenduCommunePrincipale) {
		this.numeroOfsEtenduCommunePrincipale = numeroOfsEtenduCommunePrincipale;
	}
}
