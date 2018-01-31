package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.TexteCasePostale;

/**
 * Interface de base d'une adresse purement fiscale (interface en lecture seule)
 */
public interface AdresseFiscale extends Annulable, DateRange {

	/**
	 * Identifiant en base de données de l'adresse
	 */
	Long getId();

	/**
	 * Ligne libre additionnelle pour les données d'adresse supplémentaires qui ne trouvent pas leur place dans les autres champs de l'adresse (p. ex. pour la mention c/o, etc.).
	 * Longueur maximum selon eCH-0010 : 60
	 */
	String getComplement();

	/**
	 * Désignation de la rue en texte libre
	 */
	String getRue();

	/**
	 * Numéro de la maison dans l'adresse postale, y compris des indications additionnelles.
	 * Longueur maximum selon eCH-0010 : 12
	 */
	String getNumeroMaison();

	/**
	 * Texte de la case postale dans la langue voulue.
	 * Dans la plupart des cas, le texte "Case postale" ou "Boîte postale" suffit.
	 * Longueur maximum selon eCH-0010 : 15
	 */
	TexteCasePostale getTexteCasePostale();

	/**
	 * Numéro de la case postale
	 * Valeurs admises selon eCH-0010 : 0-9999
	 */
	Integer getNumeroCasePostale();

	/**
	 * Adresse permanente
	 */
	boolean isPermanente();
}
