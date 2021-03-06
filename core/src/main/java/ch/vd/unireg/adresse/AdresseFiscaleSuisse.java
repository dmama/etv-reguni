package ch.vd.unireg.adresse;

public interface AdresseFiscaleSuisse extends AdresseFiscale {

	/**
	 * Numéro de la rue du répertoire des rues fourni par DCL Data Care (entreprise de la Poste suisse) pour compléter l'offre NPA.
	 * Pour plus de détail, consulter le fichier "Répertoire des rues. Description de l'offre et structure des données" disponible sur le site http://www.match.ch
	 */
	Integer getNumeroRue();

	/**
	 * Le numéro d’ordre Poste constitue la partie-clé du NPA; elle est unique et ne peut être modifiée.
	 * Chaque nouveau NPA reçoit un nouveau ONRP. Le ONRP reste inchangé, même si le NPA lui-même
	 * change.
	 * Lorsqu’un NPA est mis hors service, son ONRP n’est plus utilisé.
	 * Si le NPA devait être remis en service (ce qui est très rare), ce serait avec le ONRP d’origine.
	 */
	Integer getNumeroOrdrePoste();

	/**
	 * Surcharge du npa de l'adresse avec le npa de la case postale lorsque que celui-ci diffère.
	 * Voir <a href ="http://issuetracker.etat-de-vaud.ch/jira/browse/SIFISC-143">SIFISC-143</a>
	 */
	Integer getNpaCasePostale();
}
