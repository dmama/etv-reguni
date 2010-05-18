package ch.vd.uniregctb.evenement.obtentionpermis;

import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;

/**
 * Bouchon pour un événement de typeNaissance
 *
 * @author Ludovic Bertin
 */
public class MockObtentionPermis extends MockEvenementCivil implements ObtentionPermis {

	private Integer numeroOfsEtenduCommunePrincipale;
	private EnumTypePermis typePermis;

	public boolean isContribuablePresentBefore() {
		return false;
	}

	public void setTypePermis(EnumTypePermis typePermis) {
		this.typePermis = typePermis;
	}

	public EnumTypePermis getTypePermis() {
		return this.typePermis;
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}

	public void setNumeroOfsEtenduCommunePrincipale(Integer numeroOfsEtenduCommunePrincipale) {
		this.numeroOfsEtenduCommunePrincipale = numeroOfsEtenduCommunePrincipale;
	}
}
