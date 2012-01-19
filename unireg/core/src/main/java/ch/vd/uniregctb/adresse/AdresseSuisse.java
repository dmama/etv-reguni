package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("AdresseSuisse")
public class AdresseSuisse extends AdresseSupplementaire {

	private static final long serialVersionUID = 2539958821652480740L;

	private Integer numeroRue;

	@Column(name = "NUMERO_RUE")
	public Integer getNumeroRue() {
		return numeroRue;
	}

	public void setNumeroRue(Integer theNumeroRue) {
		numeroRue = theNumeroRue;
	}


	/**
	 * Le numéro d’ordre Poste constitue la partie-clé du NPA; elle est unique et ne peut être modifiée.
	 * Chaque nouveau NPA reçoit un nouveau ONRP. Le ONRP reste inchangé, même si le NPA lui-même
	 * change.
	 * Lorsqu’un NPA est mis hors service, son ONRP n’est plus utilisé.
	 * Si le NPA devait être remis en service (ce qui est très rare), ce serait avec le ONRP d’origine.
	 */
	private Integer numeroOrdrePoste;

	@Column(name = "NUMERO_ORDRE_POSTE")
	public Integer getNumeroOrdrePoste() {
		return numeroOrdrePoste;
	}

	public void setNumeroOrdrePoste(Integer theNumeroOrdrePoste) {
		numeroOrdrePoste = theNumeroOrdrePoste;
	}

	/**
	 * Surcharge du npa de l'adresse avec le npa de la case postale lorsque que celui-ci diffère.
	 * Voir <a href ="http://issuetracker.etat-de-vaud.ch/jira/browse/SIFISC-143">SIFISC-143</a>
	 */
	private Integer NpaCasePostale;

	@Column(name = "NPA_CASE_POSTALE")
	public Integer getNpaCasePostale() {
		return NpaCasePostale;
	}

	public void setNpaCasePostale(Integer npaCasePostale) {
		NpaCasePostale = npaCasePostale;
	}
}
