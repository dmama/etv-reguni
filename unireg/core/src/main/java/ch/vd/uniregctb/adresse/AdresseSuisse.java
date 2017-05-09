package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("AdresseSuisse")
public class AdresseSuisse extends AdresseSupplementaire implements AdresseFiscaleSuisse {

	/**
	 * Numéro de la rue du répertoire des rues fourni par DCL Data Care (entreprise de la Poste suisse) pour compléter l'offre NPA.
	 * Pour plus de détail, consulter le fichier "Répertoire des rues. Description de l'offre et structure des données" disponible sur le site http://www.match.ch
	 */
	private Integer numeroRue;

	@Override
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

	@Override
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
	private Integer npaCasePostale;

	@Override
	@Column(name = "NPA_CASE_POSTALE")
	public Integer getNpaCasePostale() {
		return npaCasePostale;
	}

	public void setNpaCasePostale(Integer npaCasePostale) {
		this.npaCasePostale = npaCasePostale;
	}

	public AdresseSuisse() {
	}

	protected AdresseSuisse(AdresseSuisse src) {
		super(src);
		this.numeroRue = src.numeroRue;
		this.numeroOrdrePoste = src.numeroOrdrePoste;
		this.npaCasePostale = src.npaCasePostale;
	}

	@Override
	public AdresseSuisse duplicate() {
		return new AdresseSuisse(this);
	}
}
