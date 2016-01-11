package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

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
	Map<String, List<DateRanged<FonctionOrganisation>>> getFonction();

	/**
	 * @return historique des identifiants IDE
	 */
	List<DateRanged<String>> getNumeroIDE();

	/**
	 * @return les données historisées en provenance du registre IDE
	 */
	DonneesRegistreIDE getDonneesRegistreIDE();

	List<DateRanged<String>> getNom();

	String getNom(RegDate date);

	/**
	 * @return les données historisées en provenance du registre RC
	 */
	DonneesRC getDonneesRC();

	/**
	 * @return l'historique des sièges du site (= commune ou pays)
	 */
	List<Domicile> getDomiciles();

	/**
	 * @return les valeurs historisées du type de site (principal / secondaire)
	 */
	List<DateRanged<TypeDeSite>> getTypeDeSite();

	/**
	 * Retourne le siege correspondant à la date. Si la date est nulle, la date du jour est utilisée.
	 * @param date La date désirée
	 * @return Le siège, ou null si aucun siège valide à la date donnée
	 */
	Domicile getDomicile(RegDate date);

	RegDate getDateInscriptionRC(RegDate date);

	List<Adresse> getAdresses();

	List<DateRanged<Long>> getRemplacePar();

	Long getRemplacePar(RegDate date);

	Map<Long, List<DateRanged<Long>>> getEnRemplacementDe();

	List<Long> getEnRemplacementDe(RegDate date);
}
