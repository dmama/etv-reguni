package ch.vd.unireg.interfaces.entreprise.data;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

public interface EntrepriseCivile {

	/**
	 * @return l'identifiant de l'entreprise dans le système source, utilisé comme clé dans Unireg (id cantonal)
	 */
	long getNumeroEntreprise();

	Capital getCapital(RegDate date);

	/**
	 * @return Liste des établissements civils de l'entreprise
	 */
	List<EtablissementCivil> getEtablissements();

	/**
	 * @return historique des formes juridiques de l'entreprise
	 */
	List<DateRanged<FormeLegale>> getFormeLegale();

	/**
	 * @return historique des identifiants IDE
	 */
	List<DateRanged<String>> getNumeroIDE();

	/**
	 * @return l'dentifiant IDE pour une date donnée, ou null si aucun
	 */
	String getNumeroIDE(RegDate date);

	/**
	 * @return l'historique des identifiants RC
	 */
	List<DateRanged<String>> getNumeroRC();

	/**
	 * @param date date de référence
	 * @return l'identifiant RC à la date donnée, ou <code>null</code> si aucun
	 */
	String getNumeroRC(RegDate date);

	/**
	 * @return historique des raisons sociales de l'entreprise
	 */
	List<DateRanged<String>> getNom();

	/**
	 * @return raison sociale de l'entreprise à une date donnée
	 */
	String getNom(RegDate date);

	/**
	 * @return historiques du nom additionnel de l'entreprise
	 */
	List<DateRanged<String>> getNomAdditionnel();

	/**
	 * @return nom additionnel de l'entreprise à une date donnée
	 */
	String getNomAdditionnel(RegDate date);

	/**
	 * @return siège principal de l'entreprise à une date donnée
	 */
	Domicile getSiegePrincipal(RegDate date);

	/**
	 * @return Forme légale de l'entreprise à une date donnée
	 */
	FormeLegale getFormeLegale(RegDate date);

	/**
	 * @return historique des capitaux de l'entreprise
	 */
	List<Capital> getCapitaux();

	/**
	 * Liste des communes de domicile des établissements principaux de l'entreprise, c'est à dire
	 * la liste des communes où l'entreprise à domicilié son siège social.
	 *
	 * @return La liste des sièges de l'entreprise
	 */
	List<Domicile> getSiegesPrincipaux();

	/**
	 * @return l'historique des adresses de l'entreprise, triées par ordre chronologique croissant.
	 */
	List<Adresse> getAdresses();

	/**
	 * @return historique des établissements civils principaux de l'entreprise
	 */
	List<DateRanged<EtablissementCivil>> getEtablissementsPrincipaux();

	/**
	 * @return établissement civil principal de l'entreprise à une date donnée
	 */
	DateRanged<EtablissementCivil> getEtablissementPrincipal(RegDate date);

	/**
	 * @return liste des établissements civils secondaires de l'entreprise à une date donnée
	 */
	List<EtablissementCivil> getEtablissementsSecondaires(RegDate date);

	/**
	 * @return l'établissement civil de l'entreprise correspondant à l'identifiant donné
	 */
	EtablissementCivil getEtablissementForNo(Long noEtablissementCivil);

	/**
	 * @return true si l'entreprise est inscrite au RC à une date donnée (= quel que soit l'état de l'inscription). Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isInscriteAuRC(RegDate date);

	/**
	 * @return <code>true</code> si les données connues à la date fournies présentent l'entreprise comme inscrite au RC (quel que soit l'état de l'inscription)
	 */
	boolean isConnueInscriteAuRC(RegDate date);

	/**
	 * @return true si l'entreprise est radiée au RC à à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isRadieeDuRC(RegDate date);

	/**
	 * @return true si l'entreprise est inscrite à l'IDE à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isInscriteIDE(RegDate date);

	/**
	 * @return true si l'entreprise est radiée de l'IDE à à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isRadieeIDE(RegDate date);

	/**
	 * @return true si l'entreprise possède son siège principal sur Vaud à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean hasEtablissementPrincipalVD(RegDate date);

	/**
	 * @return true si un établissement civil de l'entreprise est domicilié dans le canton de Vaud (principal ou secondaire), false sinon
	 */
	boolean hasEtablissementVD(RegDate date);

	/**
	 * Est-ce que l'entreprise a une forme juridique constitutive d'une société individuelle?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une société individuelle
	 */
	boolean isSocieteIndividuelle(RegDate date);

	/**
	 * Est-ce que l'entreprise a une forme juridique constitutive d'une société simple?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une société simple
	 */
	boolean isSocieteSimple(RegDate date);

	/**
	 * Est-ce que l'entreprise a une forme juridique constitutive d'une société de personnes?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une société de personnes
	 */
	boolean isSocieteDePersonnes(RegDate date);

	/**
	 * Est-ce que l'entreprise a une forme juridique d'association ou de fondation?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une assocociation ou une fondation
	 */
	boolean isAssociationFondation(RegDate date);

	/**
	 * Est-ce que l'entreprise a une forme juridique de société à inscription au RC obligatoire?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une société à inscription au RC obligatoire
	 */
	boolean isInscriptionRCObligatoire(RegDate date);

	/**
	 * @return liste des établissements civils de l'entreprise domiciliés dans le canton de Vaud (principal ou secondaire), inscrit au RC
	 * et non radiés
	 */
	List<EtablissementCivil> getSuccursalesRCVD(RegDate date);

}
