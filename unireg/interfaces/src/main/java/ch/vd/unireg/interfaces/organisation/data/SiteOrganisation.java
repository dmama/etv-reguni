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

	List<DateRanged<FormeLegale>> getFormeLegale();

	FormeLegale getFormeLegale(RegDate date);

	List<DateRanged<String>> getNomAdditionnel();

	String getNomAdditionnel(RegDate date);

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

	RegDate getDateInscriptionRCVd(RegDate date);

	List<Adresse> getAdresses();

	List<DateRanged<Long>> getIdeRemplacePar();

	Long getIdeRemplacePar(RegDate date);

	List<DateRanged<Long>> getIdeEnRemplacementDe();

	Long getIdeEnRemplacementDe(RegDate date);

	/**
	 * @return La map des publications concernant le site, indexée par date.
	 */
	Map<RegDate, List<PublicationBusiness>> getPublications();

	/**
	 * @param date la date
	 * @return la liste des publications concernant l'entité à la date donnée
	 */
	List<PublicationBusiness>  getPublications(RegDate date);

	/**
	 * @return true si le site est inscrit au RC à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isInscritAuRC(RegDate date);

	/**
	 * @return true si le site est radié au RC à à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isRadieDuRC(RegDate date);

	/**
	 * @return true si le site est radié de l'IDE à à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isRadieIDE(RegDate date);
}
