package ch.vd.uniregctb.evenement.obtentionpermis;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;

/**
 * Bouchon pour un événement de typeNaissance
 *
 * @author Ludovic Bertin
 */
public class MockObtentionNationalite extends MockEvenementCivil implements ObtentionNationalite {

	private Integer numeroOfsEtenduCommunePrincipale;

	public boolean isContribuablePresentBefore() {
		return false;
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}

	public void setNumeroOfsEtenduCommunePrincipale(Integer numeroOfsEtenduCommunePrincipale) {
		this.numeroOfsEtenduCommunePrincipale = numeroOfsEtenduCommunePrincipale;
	}
}
