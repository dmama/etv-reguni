package ch.vd.uniregctb.evenement.changement.identificateur;

import ch.vd.uniregctb.evenement.EvenementCivil;
/**
 * Modèlise un évènement de changement d'identificateur
 * @author <a href="mailto:baba-issa.ngom@vd.ch">Baba Issa NGOM </a>
 *
 */
public interface ChangementIdentificateur extends EvenementCivil {
/**
 *
 * @return le nouvel identificateur de l'individu, NAVS, NAVS13,SYMIC
 */
	public String getNouvelIdentificateur();
}
