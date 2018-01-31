package ch.vd.unireg.interfaces.organisation.data;

/**
 * <p>
 *     Représentation d'une adresse pour annonce à l'IDE.
 * </p>
 *
 * <p>
 *     NOTE: Si l'adresse est à l'étranger, le npa reste vide et le npaEtranger est éventuellement renseigné, et réciproquement.
 * </p>
 * <p>
 *     NOTE: Pas de distinction en entre adresse physique et adresse postale à ce niveau.
 * </p>
 * @author Raphaël Marmier, 2016-08-26, <raphael.marmier@vd.ch>
 */
public interface AdresseAnnonceIDE {

	Integer getEgid();
	String getRue();
	String getNumero();
	String getNumeroAppartement();
	Integer getNumeroCasePostale();
	String getTexteCasePostale();
	String getVille();
	Integer getNpa();
	String getNpaEtranger();

	Integer getNumeroOrdrePostal();
	Pays getPays();

	interface Pays {

		Integer getNoOfs();
		String getCodeISO2();
		String getNomCourt();
	}

}
