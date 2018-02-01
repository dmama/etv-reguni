package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Classe de déclaration qui possède un numéro de séquence par période fiscale
 */
@Entity
public abstract class DeclarationAvecNumeroSequence extends Declaration {

	/**
	 * Numéro de séquence de la déclaration pour une période fiscale. La première déclaration prend le numéro 1.
	 */
	private Integer numero;

	@Column(name = "NUMERO")
	public Integer getNumero() {
		return numero;
	}

	public void setNumero(Integer theNumero) {
		numero = theNumero;
	}
}
