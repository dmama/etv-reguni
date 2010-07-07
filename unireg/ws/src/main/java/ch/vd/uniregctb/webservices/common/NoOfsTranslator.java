package ch.vd.uniregctb.webservices.common;

/**
 * Cette permet de traduire un numéro Ofs en :
 * <ul>
 * <li>un numéro technique du host</li>
 * <li>un numéro Ofs normal (pas de traduction)</li>
 * </ul>
 * ... ceci en fonction d'un paramètre d'environnement.
 * <p>
 * L'idée est d'offrir un mode de compatabilité sur les web-services en attendant que toutes les applications soient passées aux numéros techniques.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface NoOfsTranslator {

	/**
	 * Traduit le numéro Ofs de la commune spécifiée en un numéro technique ou un numéro Ofs.
	 *
	 * @param noOfs le numéro Ofs d'une commune suisse.
	 * @return le numéro technique de la commune, ou son numéro Ofs en fonction du paramètrage de l'application.
	 */
	int translateCommune(int noOfs);
}
