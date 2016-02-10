package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
@DiscriminatorValue(value = "QSNC")
public class QuestionnaireSNC extends Declaration {

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

	@Transient
	@Override
	public boolean isSommable() {
		return false;
	}

	@Transient
	@Override
	public boolean isRappelable() {
		return true;
	}
}
