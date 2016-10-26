package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("BienFond")
public class BienFondRF extends ImmeubleRF {

	/**
	 * Vrai si l'immeuble est une construction sur fond d'autrui (CFA).
	 */
	private boolean cfa;

	@Column(name = "CFA")
	public boolean isCfa() {
		return cfa;
	}

	public void setCfa(boolean cfa) {
		this.cfa = cfa;
	}
}
