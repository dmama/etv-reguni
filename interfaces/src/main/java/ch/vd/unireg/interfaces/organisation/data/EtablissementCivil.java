package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

/**
 * Interface d'un établissement civil d'entreprise (= un établissement, au sens civil du terme)
 */
public interface EtablissementCivil {

	/**
	 * @return l'identifiant technique de l'établissement civil dans le système source, utilisé comme clé dans Unireg (= id cantonal)
	 */
	long getNumeroEtablissement();

	/**
	 * @return les fonctions associées à cet établissement
	 */
	Map<String, List<DateRanged<FonctionOrganisation>>> getFonction();

	/**
	 * @return historique des identifiants IDE
	 */
	List<DateRanged<String>> getNumeroIDE();

	String getNumeroIDE(RegDate date);

	/**
	 * @return l'historique des identifiants au RC (ancienne identification avant l'IDE, mais pratique car connue au RF)
	 */
	List<DateRanged<String>> getNumeroRC();

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
	 * @return l'historique des sièges de l'établissement civil (= commune ou pays)
	 */
	List<Domicile> getDomiciles();

	/**
	 * @return l'historique des sièges de l'établissement civil (= commune ou pays) durant la période active de l'entreprise
	 */
	List<Domicile> getDomicilesEnActivite();

	/**
	 * @return les valeurs historisées du type d'établissement civil (principal / secondaire)
	 */
	List<DateRanged<TypeEtablissementCivil>> getTypesEtablissement();

	TypeEtablissementCivil getTypeEtablissement(RegDate date);

	/**
	 * Retourne le siege correspondant à la date. Si la date est nulle, la date du jour est utilisée.
	 * @param date La date désirée
	 * @return Le siège, ou null si aucun siège valide à la date donnée
	 */
	Domicile getDomicile(RegDate date);

	boolean isSuccursale(RegDate date);

	RegDate getDateInscriptionRC(RegDate date);

	RegDate getDateInscriptionRCVd(RegDate date);

	RegDate getDateRadiationRC(RegDate date);

	RegDate getDateRadiationRCVd(RegDate date);

	List<Adresse> getAdresses();

	List<DateRanged<Long>> getIdeRemplacePar();

	Long getIdeRemplacePar(RegDate date);

	List<DateRanged<Long>> getIdeEnRemplacementDe();

	Long getIdeEnRemplacementDe(RegDate date);

	/**
	 * @return La map des publications concernant l'établissement civil, indexée par date.
	 */
	List<PublicationBusiness> getPublications();

	/**
	 * Les publications FOSC publiées à une certaine date.
	 *
	 * Attention, ne pas confondre date de publication et date de valeur.
	 *
	 * @param date la date de publication pour laquelle on cherche les publications FOSC
	 * @return la liste des publications concernant l'entité publiées à la date donnée
	 */
	List<PublicationBusiness>  getPublications(RegDate date);

	/**
	 * Détermine la date de premier snapshot de l'établissement civil. C'est à dire à partir de quand l'établissement civil
	 * est connu au civil.
	 * @return la date du premier snapshot
	 */
	RegDate connuAuCivilDepuis();

	/**
	 * @return true si l'établissement civil est inscrit au RC à une date donnée (quelle que soit la teneur de l'inscription). Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isInscritAuRC(RegDate date);

	/**
	 * @return <code>true</code> si les données connues à la date fournies présentent l'établissement civil comme inscrit au RC (quel que soit l'état de l'inscription)
	 */
	boolean isConnuInscritAuRC(RegDate date);

	/**
	 * @return true si l'établissement civil est globallement actif à à une date donnée, c'est à dire qu'il a une existence active chez au moins
	 * un fournisseur (RC, IDE, REE ...). Etre actif signifie être inscrit et non radié.
	 * .
	 * Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isActif(RegDate date);

	/**
	 * @return true si l'établissement civil est radié au RC à à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isRadieDuRC(RegDate date);

	/**
	 * @return true si l'établissement civil est radié de l'IDE à à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isRadieIDE(RegDate date);

	DonneesREE getDonneesREE();
}
