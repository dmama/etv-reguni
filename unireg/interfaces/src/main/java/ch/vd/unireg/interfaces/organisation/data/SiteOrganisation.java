package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

/**
 * Interface d'un site d'organisation (= un établissement, au sens civil du terme)
 */
public interface SiteOrganisation {

	/**
	 * @return l'identifiant technique du site dans le système source, utilisé comme clé dans Unireg (= id cantonal)
	 */
	long getNumeroSite();

	/**
	 * @return les fonctions associées à cet établissement
	 */
	List<DateRanged<FonctionOrganisation>> getFonction();

	/**
	 * @return historique des identifiants IDE
	 */
	List<DateRanged<String>> getNumeroIDE();

	/**
	 * @return les données historisées en provenance du registre IDE
	 */
	DonneesRegistreIDE getDonneesRegistreIDE();

	/**
	 * @return les données historisées en provenance du registre RC
	 */
	DonneesRC getDonneesRC();

	/**
	 * @return les identifiants historisés des sièges du site (= numéro OFS de la commune)
	 */
	List<DateRanged<Integer>> getSiege();

	/**
	 * @return les valeurs historisées du type de site (principal / secondaire)
	 */
	List<DateRanged<TypeDeSite>> getTypeDeSite();
}
