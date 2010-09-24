package ch.vd.uniregctb.evenement.changement.nom;

import ch.vd.uniregctb.evenement.EvenementCivil;

/**
 * Modélise un événement de changement de nom.
 * 
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public interface ChangementNom extends EvenementCivil {

	/**
	 * @return Returns the nouveauNom.
	 */
	public String getNouveauNom();

	/**
	 * @return Returns the nouveauPrenom.
	 */
	public String getNouveauPrenom();
}
