package ch.vd.uniregctb.tiers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.tiers.rattrapage.ancienshabitants.RecuperationDonneesAnciensHabitantsResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantResults;
import ch.vd.uniregctb.tiers.rattrapage.origine.RecuperationOriginesNonHabitantsResults;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.StatutMenageCommun;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Fournit les differents services d'accès aux données du Tiers.
 */
@SuppressWarnings({"JavadocReference"})
public interface TiersService {

    /**
     * Recherche les Tiers correspondants aux critères dans le data model de Unireg
     *
     * @param tiersCriteria les critères de recherche
     * @return la liste des tiers correspondants aux criteres.
     * @throws IndexerException en cas d'impossibilité d'exécuter la recherche
     */
    List<TiersIndexedData> search(TiersCriteria tiersCriteria) throws IndexerException;

    /**
     * Renvoie la personne physique correspondant au numéro d'individu passé en paramètre.
     *
     * @param numeroIndividu le numéro de l'individu.
     * @return la personne physique (tiers non-annulé) correspondante au numéro d'individu passé en paramètre, ou <b>null</b>.
     */
    PersonnePhysique getPersonnePhysiqueByNumeroIndividu(long numeroIndividu);

    /**
     * Renvoie l'entreprise (fiscale, donc) correspondant au numéro d'organisation (civile) passé en paramètre.
     *
     * @param numeroOrganisation le numéro de l'organisation
     * @return l'entreprise (tiers non-annulé) correspondante au numéro d'organisation passé en paramètre, ou <b>null</b>.
     */
    Entreprise getEntrepriseByNumeroOrganisation(long numeroOrganisation);

    /**
     * @param entreprise entreprise
     * @return la liste des établissements principaux (avec leurs dates de validité) associés à l'entreprise
     */
    List<DateRanged<Etablissement>> getEtablissementsPrincipauxEntreprise(Entreprise entreprise);

    /**
     * <p>
     * Calcul la liste avec périodes des établissements secondaires d'une entreprise.
     * </p>
     * <p>
     * NOTE: Un établissement qui aurait été cédé puis repris apparaîtra deux fois car il est rattaché sur deux
     *       périodes différentes.
     * </p>
     * @param entreprise l'entreprise ciblée
     * @return la liste des établissements secondaires (avec leurs dates de validité) associés à l'entreprise
     */
    List<DateRanged<Etablissement>> getEtablissementsSecondairesEntreprise(Entreprise entreprise);

	/**
	 * Retourne la liste des établissements présents et passés de l'entreprise, c'est-à-dire INDEPENDAMENT
	 * de/des plage(s) de validité de rapport économique.
	 * @param entreprise l'entreprise dont on veut connaître les établissements.
	 * @return
	 */
	List<Etablissement> getEtablissementsSecondairesEntrepriseSansRange(Entreprise entreprise);

	/**
	 * @param entreprise l'entreprise ciblée
	 * @param date la date de valeur, si null la date du jour est utilisée
	 * @return la liste des établissements secondaires associés à l'entreprise et valides à la date de valeur
	 */
	List<Etablissement> getEtablissementsSecondairesEntreprise(Entreprise entreprise, RegDate date);

	Entreprise createEntreprisePourEvenementOrganisation(EvenementOrganisation evt) throws TiersException;

    /**
     * Créer une entreprise pour le numéro d'organisation fourni. La méthode refuse de la créer si une entreprise est déjà associée à l'organisation.
     *
     * @param noOrganisation
     * @return L'entreprise créée.
     */
    @NotNull
    Entreprise createEntreprise(long noOrganisation);

    /**
     * Créer un établissement pour le numéro de site fourni. La méthode refuse de le créer si un établissement est déjà associé au site.
     *
     * @param numeroSite
     * @return L'établissement créé.
     */
    @NotNull
    Etablissement createEtablissement(Long numeroSite);

    /**
     * @param etablissement établissement ciblé
     * @return la liste des entités juridiques (personnes physiques, entreprises...) liées à l'établissement donné
     */
    List<DateRanged<Contribuable>> getEntitesJuridiquesEtablissement(Etablissement etablissement);

    /**
     * Retourne un tiers en fonction de son numéro de tiers.
     *
     * @param numeroTiers le numéro de tiers (= numéro de contribuable, sauf dans le cas du débiteur prestation imposable)
     * @return le tiers trouvé, ou null si aucun tiers ne possède ce numéro.
     */
    Tiers getTiers(long numeroTiers);

    /**
     * Ré-initialise les champs NAVS11 et NumRCE du non-habitant donné
     *
     * @param nonHabitant le tiers non-habitant sur lequel les identifiants vont être assignés
     * @param navs11      NAVS11 (potentiellement avec points...)
     * @param numRce      numéro du registre des étrangers
     */
    void setIdentifiantsPersonne(PersonnePhysique nonHabitant, String navs11, String numRce);

    /**
     * Ré-initialise les champs IDE
     *
     * @param contribuable  le contribuable sur lequel changer cette valeur
     * @param ide           le numéro IDE (potentiellement avec des points, tirets...)
     */
    void setIdentifiantEntreprise(Contribuable contribuable, String ide) throws TiersException;

	/**
	 * Vérifie que l'édition des données civiles du contribuable visé est permise d'un point de vue métier
	 * @param contribuable le contribuable visé (une entreprise ou un établissement)
	 */
	void checkEditionCivileAutorisee(Contribuable contribuable);

	/**
     * Change un non Habitant (qui n'a jamais été habitant) en ménage. Méthode a utiliser qu'en cas de strict necessité
     *
     * @param numeroTiers le numéro de tiers (= numéro de contribuable de la PP non habitant)
     */
    void changeNHenMenage(long numeroTiers);

	/**
	 * Crée un non-habitant lié vers un individu. Cette méthode est utile dans le cas très rare où l'on apprend l'existence d'un habitant avant d'avoir traité son événement d'arrivée et que l'on a
	 * besoin immédiatement de la personne physique correspondante.
	 *
	 * @param numeroIndividu un numéro d'individu
	 * @return un nouveau non-habitant lié à un individu
	 */
	@NotNull
	PersonnePhysique createNonHabitantFromIndividu(long numeroIndividu);

	/**
	 * @return  le statut du ménage commun
	 */
	StatutMenageCommun getStatutMenageCommun(MenageCommun menageCommun);

	List<FormeLegaleHisto> getFormesLegales(@NotNull Entreprise entreprise, boolean aussiAnnule);

	List<RaisonSocialeHisto> getRaisonsSociales(@NotNull Entreprise entreprise, boolean avecAnnulees);

	/**
	 * @return la liste des sièges de l'entreprise, c'est-à-dire la liste des lieux de domicile des établissements PRINCIPAUX successifs.
	 */
	List<DomicileHisto> getSieges(@NotNull Entreprise entreprise, boolean aussiAnnules);

	/**
	 * @return la liste des lieux successifs de domicile d'un établissement
	 */
	List<DomicileHisto> getDomiciles(@NotNull Etablissement etablissement, boolean aussiAnnules);

	/**
	 * Calcul la liste des lieux successifs de domicile d'un établissement durant sa période d'activité, en prenant soin
	 * de marquer comme CIVILs les périodes qui sont des surcharges correctives des données civiles.
	 */
	List<DomicileHisto> getDomicilesEnActiviteSourceReelle(@NotNull Etablissement etablissement, boolean aussiAnnules);

	/**
	 * @param entreprise une entreprise
	 * @param dateReference une date de référence (si <code>null</code>, on prendra la dernier état connu)
	 * @return si oui ou non cette entreprise est inscrite au RC à la date de référence donnée (dans le sens de <i>possède une inscription</i>)
	 */
	boolean isInscriteRC(@NotNull Entreprise entreprise, RegDate dateReference);

	/**
	 * @param entreprise une entreprise
	 * @param dateReference une date de référence (si <code>null</code>, on prendra la dernier état connu)
	 * @return si oui ou non cette entreprise est inscrite au RC à la date de référence donnée (dans le sens de <i>possède une inscription <strong>active</strong></i>, i.e. <i>non-radiée</i>)
	 */
	boolean hasInscriptionActiveRC(@NotNull Entreprise entreprise, RegDate dateReference);

	/**
	 * Renvoie la liste des types de changements d'état d'entreprise disponibles pour une date donnée en fonction des conditions règnant à la date,
	 * et si l'historique existant le permet.
	 *
	 * @param entreprise l'entreprise visée par le changement d'état
	 * @param date la date à laquelle doit avoir lieu la transition
	 * @param generation le type de génération prévu
	 * @return la liste des types d'etats disponibles, vide si aucun.
	 */
	List<TypeEtatEntreprise> getTransitionsEtatEntrepriseDisponibles(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation);

	/**
	 * Effectue une transition d'état sur une entreprise.
	 *
	 * <h4>Note:</h4>
	 * <ul>
	 *     <li>
	 *         La transition ne peut être effectuée que sur le dernier état de l'entreprise. Cela veut dire que la date souhaité doit
	 *         se trouver au plus tôt le jour de la date de ce dernier état.
	 *     </li>
	 *     <li>La transition n'aura lieu que si toutes les conditions sont réunies et vérifiées pour la date, et si l'historique existant le permet.</li>
	 * </ul>
	 *
	 * @param type l'état souhaité
	 * @param entreprise l'entreprise visée par le changement d'état
	 * @param date la date à laquelle doit avoir lieu la transition
	 * @param generation le type de génération prévu
	 * @return le nouvel état, ou null si la transition ne peut être effectuée
	 */
	EtatEntreprise changeEtatEntreprise(TypeEtatEntreprise type, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation);

	/**
	 * Annule l'état d'entreprise, ce qui revient à ramener l'entreprise dans l'état précédent.
	 *
	 * <p>
	 *     <strong>Attention</strong>: il s'agit d'une annulation pure et simple et non d'une transition.
	 *     La seule condition pour annuler un état est que l'entreprise possède au moins un état antérieur.
	 * </p>
	 *
	 * @param etatEntreprise l'état d'entreprise à annuler
	 * @return true si l'etat a bien été annulé, false autrement
	 */
	void annuleEtatEntreprise(EtatEntreprise etatEntreprise);

    /**
     * Retourne le type de régime fiscal par défaut en fonction d'une catégorie d'entreprise
     * Peut retourner null si :
     * - la catégorie d'entreprise n'est pas prise en compte dans le mapping de la méthode (pour le moment, PP, SP et AUTRE ne sont pas gérés)
     * - aucun régime fiscal n'est renvoyé par le service infra
     *
     * @param categorieEntreprise la catégorie de l'entreprise
     * @return le type de régime fiscal pour autant que la catégorie soit prise en compte dans le mapping (sinon NULL)
     */
    TypeRegimeFiscal getTypeRegimeFiscalParDefault(CategorieEntreprise categorieEntreprise);

	Entreprise getEntreprise(Etablissement etablissement, RegDate date);

	/**
	 * @param entreprise l'entreprise concernée
	 * @param date la date
	 * @return l'établissement principal à la date mentionnée, ou <code>null</code> s'il y en a aucun à cette date.
	 */
	Etablissement getEtablissementPrincipal(Entreprise entreprise, RegDate date);

	/**
	 * Associe une entreprise à une organisation civile. Ce faisant, ferme ou annule les surcharges civiles à la veille du début des données civiles,
	 * et supprime le numéro IDE du registre fiscal.
	 * @param entreprise
	 * @param organisation
	 */
	void apparier(Entreprise entreprise, Organisation organisation);

	/**
	 * Associe un établissement à un site du registre civil. Ce faisant, ferme ou annule les surcharges civiles à la veille du début des données civiles,
	 * et supprime le numéro IDE du registre fiscal.
	 * @param etablissement l'établissement à apparier
	 * @param site le site cible de l'appariement
	 */
	void apparier(Etablissement etablissement, SiteOrganisation site);

	/**
	 * Ferme ou annule les surcharges civiles à la date fournie.
	 * @param entreprise l'entreprise visée
	 * @param finFiscale la date du dernier jour des données fiscales (la veille du début des données civiles)
	 */
	void fermeSurchargesCiviles(Entreprise entreprise, RegDate finFiscale);

	/**
	 * Ferme ou annule les surcharges civiles à la date fournie.
	 * @param etablissement l'établissement visé
	 * @param finFiscale la date du dernier jour des données fiscales (la veille du début des données civiles)
	 */
	void fermeSurchargesCiviles(Etablissement etablissement, RegDate finFiscale);


	/**
	 * <p>
	 *     Détermine le degré d'association d'un tiers entreprise avec le registre civil. Il existe en effet un degré intermédiaire
	 *     entre "connu" et "inconnu" au civil: connu au civil avec Unireg responsable des données civiles du tiers en tant que
	 *     service à obligations étendues. Dans ce niveau intermédiaire, certaines données sont établies par Unireg et sont
	 *     modifiables dans l'IHM, tandis que d'autre restent strictement gouvernées par le registre civil.
	 * </p>
	 * <p>
	 *     Exemple de données non modifiable: le numéro IDE.
	 * </p>
 	 * @param entreprise l'entreprise dont on veut connaître le degré d'association
	 * @param date la date pour laquelle on veut connaître le degré.
	 * @return le degré d'association de l'entreprise avec le registre civil
	 */
	DegreAssociationRegistreCivil determineDegreAssociationCivil(Entreprise entreprise, RegDate date);

	/**
	 * <p>
	 *     Détermine le degré d'association d'un tiers etablissement avec le registre civil. Il existe en effet un degré intermédiaire
	 *     entre "connu" et "inconnu" au civil: connu au civil avec Unireg responsable des données civiles du tiers en tant que
	 *     service à obligations étendues. Dans ce niveau intermédiaire, certaines données sont établies par Unireg et sont
	 *     modifiables dans l'IHM, tandis que d'autre restent strictement gouvernées par le registre civil.
	 * </p>
	 * <p>
	 *     Exemple de données non modifiable: le numéro IDE.
	 * </p>
 	 * @param etablissement l'établissement dont on veut connaître le degré d'association
	 * @param date la date pour laquelle on veut connaître le degré.
	 * @return le degré d'association de l'établissement avec le registre civil
	 */
	DegreAssociationRegistreCivil determineDegreAssociationCivil(Etablissement etablissement, RegDate date);

	enum UpdateHabitantFlagResultat {
		PAS_DE_CHANGEMENT,
		CHANGE_EN_HABITANT,
		CHANGE_EN_NONHABITANT
	}

	/**
	 * Mis-à-jour le flag habitant en fonction de l'état de l'individu dans le registre civil.
	 * Seul le flag est mis à jour (et les éventuelles données civiles de l'individu qui doivent être recopiées chez nous pour un non-habitant).
	 * Il s'agit en effet d'un simple recalcul.
	 *
	 * @param pp              la personne physique à mettre-à-jour
	 * @param noInd           le numéro d'individu correspondant
	 * @param numeroEvenement le numéro de l'événement civil qui a provoqué ce recalcul
	 * @throws TiersException s'il n'est pas possible de déterminer si le domicile du contribuable est vaudois ou pas
	 */
	UpdateHabitantFlagResultat updateHabitantFlag(@NotNull PersonnePhysique pp, long noInd, @Nullable Long numeroEvenement) throws TiersException;

	/**
	 * Changement de statut habitant/non-habitant (et vice-versa) à une date donnée en fonction de l'état de l'individu dans le registre civil
	 * à cette date. Notons que le flag habitant (voir {@link #updateHabitantFlag(PersonnePhysique, long, Long)}) est re-calculé à partir
	 * des adresses actuelles du contribuable
	 *
	 * @param pp              la personne physique à mettre-à-jour
	 * @param noInd           le numéro d'individu correspondant
	 * @param date            la date de valeur à utiliser
	 * @param numeroEvenement le numéro de l'événement civil qui a provoqué ce changement
	 * @throws TiersException s'il n'est pas possible de déterminer si le domicile du contribuable est vaudois ou pas
	 */
	UpdateHabitantFlagResultat updateHabitantStatus(@NotNull PersonnePhysique pp, long noInd, @Nullable RegDate date, @Nullable Long numeroEvenement) throws TiersException;

	/**
	 * @param pp personne physique à tester
	 * @param date date de référence pour le test
	 * @return <b>true</b> si l'adresse de domicile (= résidence principale seulement) de la personne donnée à la date donnée est dans le canton&nbsp;;
	 *         <b>false</b> si elle est hors-canton où hors-Suisse. Retourne <code>null</code> si on ne sait pas répondre de manière définitive
	 *         (pas d'adresse de domicile connue, erreurs...)
	 */
	Boolean isHabitantResidencePrincipale(@NotNull PersonnePhysique pp, RegDate date);

    /**
     * Permet de recupérer la liste des enfants à faire figurer sur la DI  d'un contribuable
     *
     * @param ctb                  dont on recherche les enfants
     * @param finPeriodeImposition
     * @return la liste des enfants, vide sinon.
     */
    List<PersonnePhysique> getEnfantsForDeclaration(Contribuable ctb, RegDate finPeriodeImposition);

    /**
     * Détermine et retourne le contribuable (la mère ou ménage-commun de la mère) qui possède l'autorité parentale du contribuable spécifié [UNIREG-3244].
     * <b>Note:</b> le contribuable est supposé non-majeur à la date de validité spécifiée
     *
     * @param contribuableEnfant un contribuable enfant
     * @param dateValidite       une date de validité
     * @return le contribuable personne physique ou ménage-commun qui possède l'autorité parentale; ou <b>null</b> si la mère est inconnue.
     */
    ContribuableImpositionPersonnesPhysiques getAutoriteParentaleDe(PersonnePhysique contribuableEnfant, RegDate dateValidite);

    /**
     * Renvoie la collectivité administrative rattachée au numero de collectivité donné.
     *
     * @param noTechnique le numero de la collectivité
     * @return le tiers représentant la collectivité administrative correspondant
     */
    CollectiviteAdministrative getCollectiviteAdministrative(int noTechnique);

    CollectiviteAdministrative getCollectiviteAdministrative(int noTechnique, boolean doNotAutoFlush);

    /**
     * Récupère le tiers correspondant à la collectivite administrative avec un numéro donné (crée le tiers s'il n'existe pas).
     *
     * @param noTechnique le numero technique de la collectivite administrative
     * @return le tiers correspondant à la collectivite administrative
     */
    CollectiviteAdministrative getOrCreateCollectiviteAdministrative(int noTechnique);

    CollectiviteAdministrative getOrCreateCollectiviteAdministrative(int noTechnique, boolean doNotAutoFlush);

    /**
     * Recupere l'individu correspondant à une personne physique
     *
     * @param personne la personne physique en question.
     * @return un individu, ou <i>null</i> si la personne physique est un non-habitant.
     */
    Individu getIndividu(@NotNull PersonnePhysique personne);

    /**
     * Recupere l'individu correspondant à une personne physique avec l'état valide pour une année donnée.
     *
     * @param personne   la personne physique en question.
     * @param date       la date de valeur de l'individu
     * @param attributes les attributs renseignés sur l'individu.  @return un individu, ou <i>null</i> si la personne physique est un non-habitant.
     * @return un individu; ou <b>null</b> si la personne physique spécifiée n'habite pas dans le canton.
     */
    Individu getIndividu(PersonnePhysique personne, RegDate date, AttributeIndividu... attributes);

    /**
     * Détermine si une personne physique est suisse.
     *
     * @param pp   la personne physique.
     * @param date la date à laquelle on désire se placer
     * @return true si la personne physique a la nationalité suisse à la date donnée.
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    boolean isSuisse(PersonnePhysique pp, RegDate date) throws TiersException;

    /**
     * Détermine si une personne physique est suisse ou possède un permis C
     *
     * @param pp            une personne physique
     * @param dateEvenement la date à laquelle on désire connaître cette information
     * @return <b>true</b> si la personne physique possède la nationalité suisse ou si elle possède un permis C; <b>false</b> autrement.
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    boolean isSuisseOuPermisC(PersonnePhysique pp, RegDate dateEvenement) throws TiersException;

    /**
     * Détermine si un individu est suisse.
     *
     * @param individu un individu
     * @param date     la date à laquelle on désire se placer
     * @return <b>vrai</b> si l'individu possède la nationalité Suisse à la date spécifiée; <b>faux</b> autrement.
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    boolean isSuisse(Individu individu, @Nullable RegDate date) throws TiersException;

    /**
     * L'individu est-t-il avec permis C en cours de validité ?
     *
     * @param individu l'individu
     * @return true si l'individu n'a pas de permis C en cours
     */
    boolean isAvecPermisC(Individu individu);

    /**
     * L'individu est-t-il avec permis C en cours de validité ?
     *
     * @param individu l'individu
     * @param date     la date à laquelle on désire se placer
     * @return true si l'individu n'a pas de permis C en cours
     */
    boolean isAvecPermisC(Individu individu, RegDate date);

    /**
     * Détermine si un habitant avec permis C.
     *
     * @param habitant l'habitant
     * @param date     la date à laquelle on souhaite se placer
     * @return true si l'habitant est étrangère avec permis C à la date donnée
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    boolean isHabitantEtrangerAvecPermisC(PersonnePhysique habitant, RegDate date) throws TiersException;

    /**
     * Détermine si une personne physique est étrangère sans permis C.
     *
     * @param pp   la personne physique
     * @param date la date à laquelle on souhaite se placer
     * @return true si la personne physique est étrangère sans permis C à la date donnée
     * @throws TiersException si la nationalite ne peut être déterminée
     */
    boolean isEtrangerSansPermisC(PersonnePhysique pp, @Nullable RegDate date) throws TiersException;

    /**
     * Détermination de l'individidu secondaire <ul> <li>2 personnes de meme sexe : le deuxieme dans l'ordre alphabétique est le secondaire</li> <li>2 personnes de sexe different : la femme est le
     * secondaire</li> </ul>
     *
     * @param tiers1 une personne physique.
     * @param tiers2 une autre personne physique.
     * @return la personne physique principale.
     */
    PersonnePhysique getPrincipal(@Nullable PersonnePhysique tiers1, @Nullable PersonnePhysique tiers2);

    /**
     * Détermination de l'individidu principal du ménage <ul> <li>2 personnes de meme sexe : le premier dans l'ordre alphabétique est le principal</li> <li>2 personnes de sexe different : l'homme est le
     * principal</li> </ul>
     *
     * @param menageCommun un ménage commun.
     * @return la personne physique principale du ménage.
     */
    PersonnePhysique getPrincipal(MenageCommun menageCommun);

    /**
     * Recherche le ménage commun d'une personne physique à une date donnée.
     *
     * @param personne la personne dont on recherche le ménage.
     * @param date     la date de référence, ou null pour obtenir le ménage courant.
     * @return le ménage common dont la personne est membre à la date donnée, ou <b>null<b> si aucun ménage n'a été trouvé.
     */
    MenageCommun findMenageCommun(PersonnePhysique personne, @Nullable RegDate date);

    /**
     * Recherche le dernier ménage commun d'une personne physique.
     *
     * @param personne la personne dont on recherche le ménage.
     * @return le dernier ménage common dont la personne est membre, ou <b>null<b> si aucun ménage n'a été trouvé.
     */
    MenageCommun findDernierMenageCommun(PersonnePhysique personne);

    /**
     * Détermine si une personne physique fait partie d'un ménage commun à une date donnée.
     *
     * @param personne la personne physique.
     * @param date     la date de référence, ou null pour obtenir le ménage courant.
     * @return true si la personne physique est membre d'un ménage commun à la date donnée.
     */
    boolean isInMenageCommun(PersonnePhysique personne, @Nullable RegDate date);

    /**
     * Contruit l'ensemble des tiers individuels et tiers menage à partir du tiers ménage-commun.
     *
     * @param menageCommun le tiers ménage-commun du menage
     * @param date         la date de référence, ou null pour obtenir tous les composants connus dans l'histoire du ménage.
     * @return un objet EnsembleTiersCouple regroupant l'ensemble des tiers individuels et tiers menage.
     */
    EnsembleTiersCouple getEnsembleTiersCouple(MenageCommun menageCommun, @Nullable RegDate date);

    /**
     * Contruit l'ensemble des tiers individuels et tiers menage à partir du tiers ménage-commun.
     *
     * @param menageCommun le tiers ménage-commun du menage
     * @param anneePeriode la période fiscale considérée pour déterminer les composants du couple. Chacun des composants du couple est pris en compte pour autant qu'il soit valide durant la période.
     * @return un objet EnsembleTiersCouple regroupant l'ensemble des tiers individuels et tiers menage.
     */
    EnsembleTiersCouple getEnsembleTiersCouple(MenageCommun menageCommun, int anneePeriode);

    /**
     * Contruit l'ensemble des tiers individuels et tiers menage à partir d'un habitant membre du menage.
     *
     * @param personne le tiers membre du menage
     * @param date     la date de référence, ou null pour obtenir l'ensemble actif
     * @return un objet EnsembleTiersCouple regroupant l'ensemble des tiers individuels et tiers menage, ou null si la personne n'appartient pas à un ménage.
     */
    EnsembleTiersCouple getEnsembleTiersCouple(PersonnePhysique personne, @Nullable RegDate date);

	/**Construit la liste des ensembleTiersCouple à partir du tiers ménage-commun.
	 *
	 * @param personne le tiers membre du menage
	 * @param anneePeriode la période fiscale considérée pour déterminer les composants du couple. Chacun des composants du couple est pris en compte pour autant qu'il soit valide durant la période
	 * @return la liste des ensembleTiersCouple valide sur la période donnée
	 */
	List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique personne, int anneePeriode);

    /**
     * Ajoute l'individu spécifié en tant que tiers du ménage commun, à partir de la date spécifiée.
     * <b>Attention : le menage et le tiers spécifiés seront automatiques sauvés !</b>
     *
     * @param menage    le ménage sur lequel le tiers doit être ajouté
     * @param tiers     le tiers à ajouter au ménage
     * @param dateDebut la date de début de validité de la relation entre tiers
     * @param dateFin   la date de fin de validité de la relation entre tiers (peut être nulle)
     * @return le rapport-entre-tiers avec les références mises-à-jour des objets sauvés
     */
    RapportEntreTiers addTiersToCouple(MenageCommun menage, PersonnePhysique tiers, RegDate dateDebut, @Nullable RegDate dateFin);

    /**
     * Clôt l'appartenance menageCommun entre les 2 tiers à la date donnée.
     *
     * @param pp            la pp
     * @param menage        le menage
     * @param dateFermeture la date de fermeture du rapport
     * @throws RapportEntreTiersException si la date de fermeture demandée n'est pas cohérente avec le rapport existant
     */
    void closeAppartenanceMenage(PersonnePhysique pp, MenageCommun menage, RegDate dateFermeture) throws RapportEntreTiersException;

    /**
     * Clôt tous les rapports du tiers.
     *
     * @param pp            la pp
     * @param dateFermeture la date de fermeture du rapport
     */
    void closeAllRapports(PersonnePhysique pp, RegDate dateFermeture);

    /**
     * Ajoute un rapport prestation imposable
     *
     *
     * @param sourcier     le sourcier sur lequel le debiteur doit être ajouté
     * @param debiteur     le debiteur à ajouter au sourcier
     * @param dateDebut    la date de début de validité de la relation entre tiers
     * @param dateFin      la date de fin de validité de la relation entre tiers (peut être nulle)
     * @return le rapport-prestation-imposable avec les références mises-à-jour des objets sauvés
     */
    RapportPrestationImposable addRapportPrestationImposable(PersonnePhysique sourcier, DebiteurPrestationImposable debiteur,
                                                             RegDate dateDebut, RegDate dateFin);

    /**
     * Ajout d'un rapport de type contact impôt source entre le débiteur et le contribuable
     *
     * @param debiteur     un débiteur
     * @param contribuable un contribuable
     * @return le rapport
     */
    RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable);

    /**
     * Ajout d'un rapport de type contact impôt source entre le débiteur et le contribuable avec une date de début
     *
     * @param debiteur     un débiteur
     * @param contribuable un contribuable
     * @param dateDebut    la date de début du rapport
     * @return le rapport
     */
    RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable, RegDate dateDebut);

    /**
     * Ajout d'un rapport de type activité économique entre l'établissement et le contribuable avec une date de début
     *
     * @param etablissement un établissement
     * @param contribuable un contribuable
     * @param dateDebut    la date de début du rapport
     * @return le rapport
     */
    RapportEntreTiers addActiviteEconomique(Etablissement etablissement, Contribuable contribuable, RegDate dateDebut, boolean principal);

    /**
     * Crée et sauvegarde en base un ménage-commun avec ces deux parties.
     *
     * @param tiers1    un tiers du ménage-commun
     * @param tiers2    l'autre tiers du ménage-commun (peut être nul)
     * @param dateDebut la date de début de validité de la relation entre tiers
     * @param dateFin   la date de fin de validité de la relation entre tiers (peut être nulle)
     * @return l'ensemble tiers-couple sauvé en base avec les références mises-à-jour des objets sauvés.
     */
    EnsembleTiersCouple createEnsembleTiersCouple(PersonnePhysique tiers1, @Nullable PersonnePhysique tiers2, RegDate dateDebut,
                                                  @Nullable RegDate dateFin);

    /**
     * Etabli et sauve en base un rapport entre deux tiers.
     *
     * @param rapport le rapport à sauver
     * @param sujet   le tiers sujet considéré
     * @param objet   le tiers objet considéré
     * @return le rapport sauvé en base
     */
    RapportEntreTiers addRapport(RapportEntreTiers rapport, Tiers sujet, Tiers objet);

    /**
     * @param pp le personne dont on veut connaître le sexe.
     * @return le sexe de la personne spécifiée, ou <b>null</b> si cette information n'est pas disponible.
     */
    Sexe getSexe(PersonnePhysique pp);

    /**
     * Détermine si les deux personnes physiques sont de même sexe.
     *
     * @param pp1 une personne physique.
     * @param pp2 une autre personne physique.
     * @return true si les personnes sont de même sexe.
     */
    boolean isMemeSexe(PersonnePhysique pp1, PersonnePhysique pp2);

    /**
     * Ouvre un nouveau for fiscal principal sur un contribuable soumis au régime des personnes physiques
     * <b>Note:</b> pour ajouter un for fiscal fermé voir la méthode {@link #addForPrincipal(ContribuableImpositionPersonnesPhysiques, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor,
     * ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor, ch.vd.uniregctb.type.MotifRattachement, int, ch.vd.uniregctb.type.TypeAutoriteFiscale, ch.vd.uniregctb.type.ModeImposition)}
     *
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @param modeImposition           le mode d'imposition du for fiscal principal
     * @param motifOuverture           le motif d'ouverture du for fiscal principal
     * @return le nouveau for fiscal principal
     */
    ForFiscalPrincipalPP openForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
                                              TypeAutoriteFiscale typeAutoriteFiscale, ModeImposition modeImposition, MotifFor motifOuverture);

    /**
     * Ouvre un nouveau for fiscal principal sur un contribuable soumis au régime des personnes physiques // FIXME: Réparer le commentaire copié-collé
     * <b>Note:</b> pour ajouter un for fiscal fermé voir la méthode {@link #addForPrincipal(ContribuableImpositionPersonnesPhysiques, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor,
     * ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor, ch.vd.uniregctb.type.MotifRattachement, int, ch.vd.uniregctb.type.TypeAutoriteFiscale, ch.vd.uniregctb.type.ModeImposition)}
     *
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @param motifOuverture           le motif d'ouverture du for fiscal principal
     * @param genreImpot               le genre d'impôt du for fiscal principal
     * @return le nouveau for fiscal principal
     */
    ForFiscalPrincipalPM openForFiscalPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
                                              TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture, GenreImpot genreImpot);

    /**
     * Ouvre un nouveau for fiscal secondaire sur un contribuable.
     * <b>Note:</b> pour ajouter un for fiscal fermé voir la méthode {@link #addForSecondaire(Contribuable, ch.vd.registre.base.date.RegDate, ch.vd.registre.base.date.RegDate,
     * ch.vd.uniregctb.type.MotifRattachement, int, ch.vd.uniregctb.type.TypeAutoriteFiscale, ch.vd.uniregctb.type.MotifFor, ch.vd.uniregctb.type.MotifFor)}
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale
     * @param motifOuverture           le motif d'ouverture
     * @param genreImpot               le genre d'impôt
     * @return le nouveau for fiscal secondaire
     */
    ForFiscalSecondaire openForFiscalSecondaire(Contribuable contribuable, final RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
                                                TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture, GenreImpot genreImpot);

	/**
     * Ouvre un nouveau for fiscal autre élément imposable sur un contribuable.
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param genreImpot               le genre d'impot
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param motifOuverture           le motif d'ouverture
     * @return le nouveau for fiscal autre élément imposable
     */
    ForFiscalAutreElementImposable openForFiscalAutreElementImposable(Contribuable contribuable, GenreImpot genreImpot,
                                                                      final RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
                                                                      MotifFor motifOuverture);

	/**
	 * Ouvre un nouveau for fiscal autre élément imposable sur un contribuable.
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param motifOuverture           le motif d'ouverture
	 * @param dateFermeture            la date à laquelle le nouveau for est fermé
	 * @param motifFermeture           motif de fermeture
	 * @param motifRattachement        le motif de rattachement du nouveau for
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @return le nouveau for fiscal autre élément imposable
	 */
	ForFiscalAutreElementImposable openForFiscalAutreElementImposable(Contribuable contribuable, RegDate dateOuverture, MotifFor motifOuverture, @Nullable RegDate dateFermeture,
	                                                                  @Nullable MotifFor motifFermeture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale);

	/**
     * Ouvre un nouveau for fiscal autre impot sur un contribuable.
     * <b>Note:</b> un for autre impôt possède une validité de 1 jour, il n'y a donc pas de méthode pour créer un for fiscal autre impôt fermé.
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param genreImpot               le genre d'impot
     * @param dateImpot                la date à laquelle le nouveau for est valide
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @return le nouveau for autre impot
     */
    ForFiscalAutreImpot openForFiscalAutreImpot(Contribuable contribuable, GenreImpot genreImpot, RegDate dateImpot, int numeroOfsAutoriteFiscale);

    /**
     * Ouvre un nouveau for fiscal debiteur sur un contribuable.
     * <b>Note:</b> pour ajouter un for fiscal fermé voir la méthode {@link #addForDebiteur(DebiteurPrestationImposable, ch.vd.registre.base.date.RegDate, ch.vd.registre.base.date.RegDate,
     * ch.vd.uniregctb.type.TypeAutoriteFiscale, int)}
     *
     * @param debiteur                 le debiteur sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifOuverture           le motif d'ouverture du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @return le nouveau for debiteur
     */
    ForDebiteurPrestationImposable openForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, MotifFor motifOuverture,
                                                                      int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale);

    /**
     * Ré-ouvre les rapports de prestation imposables du débiteur qui ont été fermés à la date de désactivation avec une nouvelle date d'ouverture à la réactivation
     *
     * @param debiteur          le débiteur sur lequel les rapports de prestation imposable doivent être ré-ouverts
     * @param dateDesactivation la date à laquelle les rapports avaient été fermés
     * @param dateReactivation  la date à laquelle les rapports doivent être ré-ouverts
     */
    void reopenRapportsPrestation(DebiteurPrestationImposable debiteur, RegDate dateDesactivation, RegDate dateReactivation);

	/**
	 * Ré-ouvre les rapports entre tiers fermés à la date donnée et de types donnés
	 *
	 * @param tiers le tiers sujet ou objet des rapports à ré-ouvrir
	 * @param dateFermeture date de fermeture critique
	 * @param typesRapportsSujet types des rapports à ré-ouvrir pour lesquels le tiers est sujet
	 * @param typesRapportsObjet types des rapports à ré-ouvrir pour lesquels le tiers est objet
	 */
	void reopenRapportsEntreTiers(Tiers tiers, RegDate dateFermeture, Set<TypeRapportEntreTiers> typesRapportsSujet, Set<TypeRapportEntreTiers> typesRapportsObjet);

    /**
     * Ferme le for fiscal principal d'un contribuable.
     *
     * @param contribuable   le contribuable concerné
     * @param dateFermeture  la date de fermeture du for
     * @param motifFermeture le motif de fermeture
     * @return le for fiscal principal fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    ForFiscalPrincipal closeForFiscalPrincipal(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture);

    /**
     * Ferme le for fiscal principal d'un contribuable.
     *
     * @param forFiscalPrincipal le for fiscal principal concerné
     * @param dateFermeture      la date de fermeture du for
     * @param motifFermeture     le motif de fermeture
     * @return le for fiscal principal fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    <F extends ForFiscalPrincipal> F closeForFiscalPrincipal(F forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture);

	/**
	 * Ferme la décision d'un contribuable
	 * @param decision a fermer
	 * @param dateFin date de fermeture
	 * @return la décision fermée
	 */
	DecisionAci closeDecisionAci(DecisionAci decision, RegDate dateFin);

	/**
     * Ferme le for fiscal secondaire d'un contribuable.
     *
     * @param contribuable        le contribuable concerné
     * @param forFiscalSecondaire le for fiscal secondaire concerné
     * @param dateFermeture       la date de fermeture du for
     * @param motifFermeture      la motif de fermeture du for
     * @return le for fiscal secondaire fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    ForFiscalSecondaire closeForFiscalSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscalSecondaire,
                                                 RegDate dateFermeture, MotifFor motifFermeture);

    /**
     * Ferme le for fiscal autre élément imposable d'un contribuable.
     *
     * @param contribuable                   le contribuable concerné
     * @param forFiscalAutreElementImposable le for à fermer
     * @param dateFermeture                  la date de fermeture du for
     * @param motifFermeture                 la motif de fermeture du for
     * @return le for fiscal autre élément imposable fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    ForFiscalAutreElementImposable closeForFiscalAutreElementImposable(Contribuable contribuable,
                                                                       ForFiscalAutreElementImposable forFiscalAutreElementImposable, RegDate dateFermeture, MotifFor motifFermeture);

    /**
     * Ferme le for debiteur d'un contribuable.
     *
     * @param debiteur                       le debiteur concerné
     * @param forDebiteurPrestationImposable le for débiteur concerné
     * @param dateFermeture                  la date de fermeture du for
     * @param motifFermeture                 la motif de fermeture du for
     * @param fermerRapportsPrestation       <code>true</code> s'il faut fermer les rapports "prestation" du débiteur, <code>false</code> s'il faut les laisser ouverts
     * @return le for debiteur fermé, ou <b>null</b> si le contribuable n'en possédait pas.
     */
    ForDebiteurPrestationImposable closeForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur,
                                                                       ForDebiteurPrestationImposable forDebiteurPrestationImposable,
                                                                       RegDate dateFermeture, MotifFor motifFermeture, boolean fermerRapportsPrestation);

    /**
     * Ferme le for autre impôt d'un tiers.
     *
     * @param autre         le for autre impôt à fermer
     * @param dateFermeture la date de fermeture du for
     */
    void closeForAutreImpot(ForFiscalAutreImpot autre, RegDate dateFermeture);

    /**
     * Ferme tous les fors fiscaux d'un contribuable.
     *
     * @param contribuable   le contribuable concerné.
     * @param dateFermeture  la date de fermeture des fors.
     * @param motifFermeture le motif de fermeture
     */
    void closeAllForsFiscaux(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture);

    /**
     * Change le mode d'imposition
     *
     * @param contribuable                 le contribuable sur lequel le mode d'imposition doit être changé.
     * @param dateChangementModeImposition la date de changement
     * @param modeImposition               le nouveau mode d'imposition
     * @param motifFor                     le motif de changement du mode d'imposition
     * @return le nouveau for principal créé
     */
    ForFiscalPrincipalPP changeModeImposition(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateChangementModeImposition,
                                              ModeImposition modeImposition, MotifFor motifFor);

	/**
     * Ajoute un for fiscal principal sur un contribuable soumis au régime des personnes physiques. Le for fiscal principal courant est fermé si nécessaire.
     *
     * @param contribuable        un contribuable
     * @param dateDebut           la date d'ouverture du for à créer
     * @param motifOuverture      le motif d'ouverture du for à créer
     * @param dateFin             la date de fermeture du for à créer (peut être nulle)
     * @param motifFermeture      le motif de fermeture du for à créer (peut être nul)
     * @param motifRattachement   le motif de rattachement du for à créer
     * @param autoriteFiscale     le numéro de l'autorité fiscale du for à créer
     * @param typeAutoriteFiscale le type de l'autorité fiscale du for à créer
     * @param modeImposition      le mode d'imposition du for à créer
     * @return le nouveau for fiscal principal.
     */
    ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateDebut, MotifFor motifOuverture, @Nullable RegDate dateFin, @Nullable MotifFor motifFermeture,
                                         MotifRattachement motifRattachement,
                                         int autoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, ModeImposition modeImposition);

	/**
     * Ajoute un for fiscal principal sur un contribuable soumis au régime des personnes morales. Le for fiscal principal courant est fermé si nécessaire.
     *
     * @param contribuable        un contribuable
     * @param dateDebut           la date d'ouverture du for à créer
     * @param motifOuverture      le motif d'ouverture du for à créer
     * @param dateFin             la date de fermeture du for à créer (peut être nulle)
     * @param motifFermeture      le motif de fermeture du for à créer (peut être nul)
     * @param motifRattachement   le motif de rattachement du for à créer
     * @param autoriteFiscale     le numéro de l'autorité fiscale du for à créer
     * @param typeAutoriteFiscale le type de l'autorité fiscale du for à créer
     * @param genreImpot          le genre d'impôt du for à créer
     * @return le nouveau for fiscal principal.
     */
    ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate dateDebut, MotifFor motifOuverture, @Nullable RegDate dateFin, @Nullable MotifFor motifFermeture,
                                         MotifRattachement motifRattachement, int autoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, GenreImpot genreImpot);

	@Nullable
	ForFiscalSecondaire updateForSecondaire(ForFiscalSecondaire ffs, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale);

	@Nullable
	ForFiscalPrincipal updateForPrincipal(ForFiscalPrincipal ffp, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale);

	@Nullable
	ForFiscalAutreElementImposable updateForAutreElementImposable(ForFiscalAutreElementImposable ffaei, RegDate dateFermeture, MotifFor motifFermeture, Integer noOfsAutoriteFiscale);

	@Nullable
	ForDebiteurPrestationImposable updateForDebiteur(ForDebiteurPrestationImposable fdpi, RegDate dateFermeture, MotifFor motifFermeture);

	/**
     * Annule tous les fors ouverts à la date spécifiée (et qui ne sont pas fermés) sur le contribuable donné et dont le motif d'ouverture correspond à ce qui est indiqué
     *
     * @param contribuable   contribuable visé
     * @param dateOuverture  date d'ouverture des fors à annuler
     * @param motifOuverture motif d'ouverture des fors à annuler (<code>null</code> possible si tout motif convient)
     */
    void annuleForsOuvertsAu(Contribuable contribuable, RegDate dateOuverture, MotifFor motifOuverture);

    /**
     * Permet de rajouter une nouvelle périodicité sur un débiteur verifie si il en existe une pour la même période, et l'annule avant d'ajouter la nouvelle
     *
     * @param debiteur
     * @param periodiciteDecompte
     * @param periodeDecompte
     * @param dateDebut
     * @param dateFin
     * @return la periodicité ajoutés
     */
    Periodicite addPeriodicite(DebiteurPrestationImposable debiteur, PeriodiciteDecompte periodiciteDecompte, @Nullable PeriodeDecompte periodeDecompte, RegDate dateDebut, @Nullable RegDate dateFin);

    /**
     * Ajoute un for fiscal secondaire sur un contribuable.
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param dateFermeture            la date de fermeture du for à créer (peut être nulle)
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale
     * @param motifOuverture           le motif d'ouverture
     * @param motifFermeture           le motif de fermeture du for à créer (peut être nul)
     * @param genreImpot               le genre d'impôt du for à créer
     * @return le nouveau for fiscal secondaire.
     */
    ForFiscalSecondaire addForSecondaire(Contribuable contribuable, RegDate dateOuverture, RegDate dateFermeture, MotifRattachement motifRattachement,
                                         int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture, MotifFor motifFermeture,
                                         GenreImpot genreImpot);

    /**
     * Ajoute un for fiscal autre élément imposable sur un contribuable.
     *
     * @param contribuable        un contribuable
     * @param dateDebut           la date d'ouverture du for à créer
     * @param motifOuverture      le motif d'ouverture du for à créer
     * @param dateFin             la date de fermeture du for à créer (peut être nulle)
     * @param motifFermeture      le motif de fermeture du for à créer (peut être nul)
     * @param motifRattachement   le motif de rattachement du for à créer
     * @param autoriteFiscale     le numéro de l'autorité fiscale du for à créer
     * @return le nouveau for fiscal.
     */
    ForFiscalAutreElementImposable addForAutreElementImposable(Contribuable contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture,
                                                               MotifRattachement motifRattachement, int autoriteFiscale);

    /**
     * Ajoute un for fiscal débiteur sur un débiteur de prestation imposable.
     *
     * @param debiteur            un débiteur de prestations imposables
     * @param dateDebut           la date d'ouverture du for à créer
     * @param motifOuverture      le motif d'ouverture du for à créer
     * @param dateFin             la date de fermeture du for à créer (peut être nulle)
     * @param motifFermeture      le motif de fermeture du for à créer (peut être nul si la date de fermeture est nulle)
     * @param typeAutoriteFiscale le type d'autorité fiscale du for à créer
     * @param autoriteFiscale     le numéro de l'autorité fiscale du for à créer
     * @return le nouveau for fiscal.
     */
    ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable debiteur, RegDate dateDebut, MotifFor motifOuverture,
                                                  RegDate dateFin, MotifFor motifFermeture, TypeAutoriteFiscale typeAutoriteFiscale, int autoriteFiscale);

    /**
     * Fusionne un non habitant avec un habitant
     *
     * @param habitant    un habitant
     * @param nonHabitant un non-habitant
     */
    void fusionne(PersonnePhysique habitant, PersonnePhysique nonHabitant);

	/**
	 * @param tiers un tiers
	 * @return sur une ligne, le nom ou la raison sociale du tiers à la date demandée
	 */
	String getNomRaisonSociale(Tiers tiers);

    /**
     * Retourne les nom et prénoms de l'individu spécifié
     *
     * @param individu un individu
     * @return une pair composée du (ou des) prénom(s) (premier élément) et du nom (deuxième élément) de la personne physique ( ou {@link NomPrenom.VIDE} si la donnée est inconnue)
     */
    NomPrenom getDecompositionNomPrenom(Individu individu);

    /**
     * Retourne les nom et prénoms pour l'adressage de l'individu spécifié.
     *
     * @param individu un individu
     * @return le prénom + le nom du l'individu
     */
    String getNomPrenom(Individu individu);

    /**
     * Retourne les nom et prénoms de la personne physique spécifiée
     *
     * @param pp personne physique dont on veut le nom
     * @param tousPrenoms <code>true</code> si tous les prénoms du tiers doivent être utilisés, <code>false</code> si seul le prénom usuel doit être pris
     * @return une pair composée du (ou des) prénom(s) (premier élément) et du nom (deuxième élément) de la personne physique ( ou {@link NomPrenom.VIDE} si la donnée est inconnue)
     */
    NomPrenom getDecompositionNomPrenom(PersonnePhysique pp, boolean tousPrenoms);

    /**
     * Retourne les nom et prénoms pour l'adressage de la personne physique spécifiée.
     *
     * @param personne une personne physique
     * @return le prénom + le nom de la personne
     */
    String getNomPrenom(PersonnePhysique personne);

    /**
     * @param pp une personne physique
     * @return la date de naissance de la personne spécifiée.
     */
    RegDate getDateNaissance(PersonnePhysique pp);

    /**
     * @param pp   personne physique dont on veut connaître si oui ou non elle est mineure
     * @param date la date à laquelle doit être fait le test
     * @return <code>true</code> si la personne physique est effectivement mineure à la date donnée, ou <code>false</code> si elle ne l'est pas ou que sa date de naissance est inconnue
     */
    boolean isMineur(PersonnePhysique pp, RegDate date);


    /**
     * Retourne la date de debut de veuvage d'une personne,
     *
     * @param pp
     * @param date Date à laquelle le veuvage est valide
     * @return la date de veuvage null si la personne n'est pas veuve
     */
    RegDate getDateDebutVeuvage(PersonnePhysique pp, RegDate date);

    /**
     * @param pp une personne physique
     * @return la date de décès ou <code>null</code> si la personne n'est pas décédée.
     */
    RegDate getDateDeces(@Nullable PersonnePhysique pp);

	/**
	 * Déduit la date de décès d'une {@link PersonnePhysique} à partir de son dernier for ou de celui de son eventuel
	 * ménage commun. le for doit être fermé (la date de fermeture renseignée) et le motif de fermeture doit être
	 * {@link MotifFor.VEUVAGE_DECES}
	 *
	 * @param pp une personne physique
	 *
	 * @return la date de décès déduite d'apres le dernier for ou <code>null</code> si le dernier for principal
	 * est ouvert ou s'il est fermé pour un motif autre que {@link MotifFor.VEUVAGE_DECES}
	 */
	RegDate getDateDecesDepuisDernierForPrincipal(PersonnePhysique pp);


	/**
     * @param pp une personne physique
     * @return true si la personne est décédé, false si la personne n'est pas décédée.
     */
    boolean isDecede(PersonnePhysique pp);

    /**
     * @param pp une personne physique
     * @return le numéro d'assuré social (numéro AVS EAN13) de la personne physique spécifiée, ou <b>null</b> si cette information n'est pas disponible.
     */
    String getNumeroAssureSocial(PersonnePhysique pp);

    /**
     * @param pp une personne physique
     * @return l'ancien numéro d'assuré social (numéro AVS 11 positions) de la personne physique spécifiée, ou <b>null</b> si cette information n'est pas disponible.
     */
    String getAncienNumeroAssureSocial(PersonnePhysique pp);

    /**
     * récupère l'office d'impôt du contribuable
     *
     * @param tiers un contribuable
     * @return l'office d'impot dont dépend le contribuable ou null s'il n'en possède pas
     */
    Integer getOfficeImpotId(Tiers tiers);

    /**
     * Calcule l'id de l'office d'impôt responsable d'un tiers à une date donnée.
     *
     * @param tiers le tiers dont on veut connaître l'office d'impôt
     * @param date  la date de validité de l'office d'impôt; ou <i>null</i> pour obtenir l'état courant.
     * @return un id de l'office d'impôt; ou <i>null</null> si le tiers n'est pas assujetti ou que son office d'impôt ne peut pas être calculé pour une autre raison.
     */
    Integer getOfficeImpotIdAt(Tiers tiers, RegDate date);

    /**
     * Calcule l'id de l'office d'impôt responsable d'un for de gestion donné
     *
     * @param forGestion le for de gestion dont on veut connaître l'office d'impôt
     * @return un id d'office d'impôt
     */
    Integer getOfficeImpotId(ForGestion forGestion);

    /**
     * Calcul l'office d'impôt responsable d'une commune
     *
     * @param noOfsCommune le numéro Ofs de la commune
     * @return un id de l'office d'impôt; ou <i>null</null> l'office d'impôt ne peut pas être calculé pour une autre raison.
     */
    Integer getOfficeImpotId(int noOfsCommune);

    /**
     * Calcule et retourne l'office d'impôt responsable d'un tiers à une date donnée.
     *
     * @param tiers le tiers dont on veut connaître l'office d'impôt
     * @param date  la date de validité de l'office d'impôt; ou <i>null</i> pour obtenir l'état courant.
     * @return un office d'impôt; ou <i>null</null> si le tiers n'est pas assujetti ou que son office d'impôt ne peut pas être calculé pour une autre raison.
     */
    CollectiviteAdministrative getOfficeImpotAt(Tiers tiers, @Nullable RegDate date);


    /**
     * Calcule et retourne l'office d'impôt de regroupement / Region responsable d'un tiers à une date donnée.
     *
     * @param tiers le tiers dont on veut connaître l'office d'impôt de regroupement
     * @param date  la date de validité de l'office d'impôt; ou <i>null</i> pour obtenir l'état courant.
     * @return un office d'impôt; ou <i>null</null> si le tiers n'est pas assujetti ou que son office d'impôt ne peut pas être calculé pour une autre raison.
     */
    CollectiviteAdministrative getOfficeImpotRegionAt(Tiers tiers, @Nullable RegDate date);


    /**
     * Calcule l'office d'impôt du tiers spécifié et retourne son id.
     *
     * @param tiers un tiers.
     * @return l'ID de l'office d'impôt (OID); ou <b>null</b> si le tiers n'a pas de for de gestion donc pas d'OID.
     */
    Integer calculateCurrentOfficeID(Tiers tiers);

    /**
     * Réouvre le for et l'assigne au tiers.
     *
     * @param ff    un for fiscal
     * @param tiers un tiers
     */
    void reopenFor(ForFiscal ff, Tiers tiers);

    /**
     * Réouvre, pour un tiers, tous ses fors fermés à une date donnée et avec le motif de fermeture spécifié si applicable.
     *
     * @param date           la date de fermeture
     * @param motifFermeture le motif de fermeture
     * @param tiers          le tiers pour qui les fors seront réouverts
     */
    void reopenForsClosedAt(RegDate date, MotifFor motifFermeture, Tiers tiers);

	/**
	 * Annule le for fiscal passé en paramètre. Si le for spécifié est un for principal et qu'il existe un for principal précédent adjacent, ce dernier est réouvert.
	 *
	 * @param forFiscal le for fiscal à annuler.
	 * @return le for fiscal qui a été réouvert en conséquence de l'annulation du for spécifié; ou <b>null</b> si aucun for fiscal n'a été réouvert.
	 * @throws ValidationException si l'annulation du for principal n'est pas possible
	 */
	ForFiscal annuleForFiscal(ForFiscal forFiscal) throws ValidationException;

	/**
     * Annule un tiers, et effectue toutes les tâches de cleanup et de maintient de la cohérence des données.
     *
     * @param tiers le tiers à annuler.
     */
    void annuleTiers(Tiers tiers);

    /**
     * Retourne le for fiscal élu <i>for de gestion</i> à la date donnée.
     * <b>Attention !</b> Cette méthode retourne <b>null</b> si aucun for fiscal n'est valide à la date spécifiée. Par obtenir le dernier for de gestion connu, veuillez utiliser la méthode {@link
     * #getDernierForGestionConnu(RegDate)}.
     * <b>Attention !</b> Pour des raisons de performances, les dates de début et de fin de validité retournées ne sont pas garanties correctes. Elles correspondent aux dates de début et de fin du for
     * fiscal sous-jacent, ce qui peut être faux dans certaines conditions particulières. Pour obtenir les dates correctes, veuillez utiliser la méthode {@link #getForsGestionHisto()}.
     *
     * @param tiers le tiers dont on veut connaître le for de gestion
     * @param date  la date d'activité du for, ou <b>null</b> pour obtenir le for couremment actif
     * @return le for de gestion, ou <b>null</b> si aucun for actif n'est trouvé.
     * @see #getDernierForGestionConnu(RegDate)
     */
    ForGestion getForGestionActif(Tiers tiers, @Nullable RegDate date);

    /**
     * Calcul et retourne l'historique des fors de gestion. Les fors retournés se touchent tous même si le contribuable n'est plus assujetti en continu (même comportement que getDernierForGestionConnu).
     *
     * @param tiers le tiers dont on veut connaître le for de gestion
     * @return l'historique des fors de gestion
     */
    List<ForGestion> getForsGestionHisto(Tiers tiers);

    /**
     * Retourne le dernier for de gestion <b>connu</b> à la date donnée, même si le contribuable n'est plus assujetti à ce moment-là.
     * <b>Attention !</b> Pour des raisons de performances, les dates de début et de fin de validité retournées ne sont pas garanties correctes. Elles correspondent aux dates de début et de fin du for
     * fiscal sous-jacent, ce qui peut être faux dans certaines conditions particulières. Pour obtenir les dates correctes, veuillez utiliser la méthode {@link #getForsGestionHisto()}.
     *
     * @param tiers le tiers dont on veut connaître le for de gestion
     * @param date  la date de validité de la requête
     * @return le dernier for de festion connu, ou <b>null</b> si le tiers n'a jamais été assujetti (= aucun for).
     */
    ForGestion getDernierForGestionConnu(Tiers tiers, @Nullable RegDate date);

    /**
     * Ferme les adresses flagées temporaires dans le fiscale
     *
     * @param tiers le tiers concerné
     * @param date  date de fermeture
     * @return liste des adresses fermées
     */
    List<AdresseTiers> fermeAdresseTiersTemporaire(Tiers tiers, RegDate date);

    /**
     * Retourne une chaîne de caractères comme "Non assujetti", "Imposition ordinaire VD/HS/HC", "Dépense"... qui décrit le type d'assujettissement du tiers donné à la date donnée
     *
     * @param tiers un tiers
     * @param date  une date de validité
     * @return une dénomination de l'assujettissement du tiers
     */
    String getRoleAssujettissement(Tiers tiers, @Nullable RegDate date);

	/**
	 * Retourne l'assujettissement calculé à la date donnée (ou "maintenant" si aucune date n'est donnée)
	 * @param contribuable un contribuable
	 * @param date une date de référence
	 * @return l'assujettissement du contribuable valide à la date demandée (ou <code>null</code> si le contribuable n'est pas assujetti à la date en question)
	 */
	Assujettissement getAssujettissement(Contribuable contribuable, @Nullable RegDate date);

    /**
     * Ceci est la méthode officielle de calcul des exercices commerciaux d'une entreprise (notamment en ce qui concerne la toute première date et la toute dernière)
     * @param entreprise une entreprise
     * @return les exercices commerciaux de cette entreprise (jusqu'à au plus tard l'exercice courant ou, s'il n'y en a plus, le dernier exercice connu)
     */
    List<ExerciceCommercial> getExercicesCommerciaux(Entreprise entreprise);

    /**
     * @param entreprise une entreprise
     * @param date date de référence
     * @return L'exercice commercial de cette entreprise valide à la date donnée (si cette date est future et que l'exercice courant ne la contient pas,
     * alors l'exercice commercial retourné pourra ne pas être présent dans la liste fournie par {@link #getExercicesCommerciaux(Entreprise)})
     */
    ExerciceCommercial getExerciceCommercialAt(Entreprise entreprise, RegDate date);

    /**
     * Défini la date limite d'exclusion sur les contribuables spécifiés par leur numéros.
     *
     * @param ctbIds     les numéros des contribuables
     * @param dateLimite la date limite d'exclusion (voir {@link Contribuable#getDateLimiteExclusionEnvoiDeclarationImpot()})
     * @param s          un status manager
     * @return le résultat de l'opération
     */
    ExclureContribuablesEnvoiResults setDateLimiteExclusion(List<Long> ctbIds, RegDate dateLimite, StatusManager s);

    /**
     * Ouvre et ferme un nouveau for fiscal principal sur un contribuable soumis au régime des personnes physiques.
     *
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau for.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @param modeImposition           le mode d'imposition du for fiscal principal
     * @param motifOuverture           le motif d'ouverture
     * @param dateFermeture            la date de fermeture du for
     * @param motifFermeture           le motif de fermeture
     * @return le nouveau for fiscal principal
     */
    ForFiscalPrincipalPP openAndCloseForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture,
                                                        MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
                                                        ModeImposition modeImposition, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture);

    /**
     * Ouvre et ferme un nouveau for fiscal principal sur un contribuable soumis au régime des personnes morales.
     *
     * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
     * @param dateOuverture            la date à laquelle le nouveau for est ouvert
     * @param motifRattachement        le motif de rattachement du nouveau for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau for.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @param motifOuverture           le motif d'ouverture
     * @param dateFermeture            la date de fermeture du for
     * @param motifFermeture           le motif de fermeture
     * @param genreImpot               le genre d'impôt
     * @return le nouveau for fiscal principal
     */
    ForFiscalPrincipalPM openAndCloseForFiscalPrincipal(ContribuableImpositionPersonnesMorales contribuable, final RegDate dateOuverture,
                                                        MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
                                                        MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, GenreImpot genreImpot);

    /**
     * Ouvre et ferme un for debiteur préstation imposable sur un débiteur
     *
     * @param debiteur                 sur lequel le for est ouvert et ferme
     * @param dateOuverture            date d'ouverture du for
     * @param motifOuverture           motif d'ouverture du for
     * @param dateFermeture            date de fermeture du for
     * @param motifFermeture           motif de fermeture du for
     * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
     * @param typeAutoriteFiscale      le type d'autorité fiscale.
     * @return le nouveau for principal
     */

    ForDebiteurPrestationImposable openAndCloseForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, MotifFor motifOuverture,
                                                                              RegDate dateFermeture, MotifFor motifFermeture, int numeroOfsAutoriteFiscale,
                                                                              TypeAutoriteFiscale typeAutoriteFiscale);

    /**
     * Lance la correction des flags "habitant" sur les personnes physiques en fonction de leur for fiscal principal actif
     *
     * @param nbThreads     nombre de threads du traitement
     * @param statusManager status manager
     */
    CorrectionFlagHabitantResults corrigeFlagHabitantSurPersonnesPhysiques(int nbThreads, StatusManager statusManager);

	/**
	 * Lance le job de récupération des données des anciens habitants depuis le registre civil
	 * @param nbThreads nombre de threads du traitement
	 * @param forceEcrasement <code>true</code> si les valeurs existantes trouvées peuvent être écrasées, <code>false</code> si on ne se permet de changer que les valeurs vides
	 * @param parents <code>true</code> si les noms/prénoms des parents doivent être récupérés
	 * @param prenoms <code>true</code> si la liste complète des prénoms de l'individu doit être récupérée
	 * @param nomNaissance <code>true</code> si le nom de naissance de l'individu doit être récupéré
	 * @param statusManager status manager
	 * @return le rapport du traitement
	 */
	RecuperationDonneesAnciensHabitantsResults recupereDonneesSurAnciensHabitants(int nbThreads, boolean forceEcrasement, boolean parents, boolean prenoms, boolean nomNaissance, StatusManager statusManager);

	/**
	 * Lance le job de récupération des données d'origine des non-habitants
	 * <ul>
	 *     <li>pour les anciens habitants, depuis les données du registre civil</li>
	 *     <li>pour les non-habitants <i>purs</i>, en tentant de retrouver une commune suisse à partir du libellé enregistré en base</li>
	 * </ul>
	 * @param nbThreads nombre de threads du traitement
	 * @param dryRun <code>true</code> s'il ne s'agit que de faire un état des lieux des choses à changer
	 * @param statusManager status manager
	 * @return le rapport du traitement
	 */
	RecuperationOriginesNonHabitantsResults recupereOriginesNonHabitants(int nbThreads, boolean dryRun, StatusManager statusManager);

    /**
     * Renvoie <code>true</code> si la personne physique est un sourcier gris à la date donnée
     *
     * @param pp   personne physique
     * @param date date de référence
     * @return <code>true</code> si la personne physique est un sourcier gris
     */
    boolean isSourcierGris(Contribuable pp, @Nullable RegDate date);

    /**
     * Retourne l'ensemble des débiteurs de prestations imposables pour lesquels le contribuable donné est tiers référent
     *
     * @param contribuable tiers référent
     * @return ensemble des débiteurs de prestations imposables
     */
    Set<DebiteurPrestationImposable> getDebiteursPrestationImposable(Contribuable contribuable);

    /**
     * Retourne la date de début de validité pour une nouvelle périodicité calculé en fonction de la dernière LR émise et de la nouvelle périodicité souhaitée
     *
     * @param debiteur le débiteur IS concerné
     * @param nouvelle nouvelle périodicité souhaitée
     * @return la date de debut de validité calculée
     */
    RegDate getDateDebutNouvellePeriodicite(DebiteurPrestationImposable debiteur, PeriodiciteDecompte nouvelle);

    /**
     * @param menage un ménage commun
     * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun (0, 1 ou 2 personnes max, par définition) en ignorant les rapports annulés
     */
    @NotNull
    Set<PersonnePhysique> getPersonnesPhysiques(MenageCommun menage);

    /**
     * @param menage un ménage commun
     * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun en prenant en compte les rapports éventuellement annulés (il peut donc y avoir plus de deux personnes
     *         physiques concernées en cas de correction de données) ; le dernier rapport entre tiers est également indiqué
     */
    @NotNull
    Map<PersonnePhysique, RapportEntreTiers> getToutesPersonnesPhysiquesImpliquees(MenageCommun menage);

    /**
     * @return le contribuable associé au débiteur; ou <b>null</b> si le débiteur n'en possède pas.
     */
    Contribuable getContribuable(DebiteurPrestationImposable debiteur);

    /**
     * Renvoie la désignation sociale du débiteur (potentiellement sur plusieurs lignes)
     *
     * @param debiteur débiteur de prestation imposable dont on veut connaître la raison sociale
     * @return liste de lignes composant la raison sociale
     */
    List<String> getRaisonSociale(DebiteurPrestationImposable debiteur);

    /**
     * @param pm l'entreprise dont on veut connaître la raison sociale complète
     * @return la raison sociale complète de l'entreprise donnée, potentiellement sur plusieurs lignes
     */
    String getDerniereRaisonSociale(Entreprise pm);

    /**
     * @param etablissement l'établissement dont on veut connaître la raison sociale
     * @return la raison sociale
     */
    String getDerniereRaisonSociale(Etablissement etablissement);

    /**
     * Renvoie une liste des composants du ménage valides à une date donnée.
     *
     * @param menageCommun
     * @param date         la date de référence, ou null pour obtenir tous les composants connus dans l'histoire du ménage.
     * @return un ensemble contenant 1 ou 2 personnes physiques correspondants au composants du ménage, ou <b>null</b> si aucune personne n'est trouvée
     */
    Set<PersonnePhysique> getComposantsMenage(MenageCommun menageCommun, RegDate date);

	/**
	 * Renvoie une liste des composants du ménage valides sur une période fiscale (1 janvier au 31 décembre) donnée.
	 *
	 * @param menageCommun le ménage en question
	 * @param anneePeriode la période fiscale de référence
	 * @return un ensemble contenant 1 ou 2 personnes physiques correspondants au composants du ménage, ou <b>null</b> si aucune personne n'est trouvée
	 */
	Set<PersonnePhysique> getComposantsMenage(MenageCommun menageCommun, int anneePeriode);

	/**
	 * Renvoie la liste des numéros d'individus liés à ce tiers et pour lesquels il existe des événements civils (RegPP ou ECH) non traités
	 * @param tiers la personne physique ou le ménage commun considéré (si ménage commun, tous ses membres seront inspectés)
	 * @return la liste (triée par ordre croissant) des numéros d'individus liés à ce tiers et pour lesquels il existe des événements civils (RegPP ou ECH) non traités
	 */
	EvenementsCivilsNonTraites getIndividusAvecEvenementsCivilsNonTraites(Tiers tiers);

    /**
     * Permet de savoir si un tiers est un veuf(ve) marié seul
     *
     * @param tiers
     * @return true si le tiers est une veuf marié seul false sinon
     */
    boolean isVeuvageMarieSeul(PersonnePhysique tiers);

    /**
     * Extrait Le numéro d'individu à partir d'un tiers si c'est possible
     *
     * @param tiers
     * @return le numéro d'individu de la personne physique ou de la personne principal du menage. null si le tiers ne possède pas de numéro d'individu
     */
    Long extractNumeroIndividuPrincipal(Tiers tiers);

    /**
     * Analyse le graphe des entités liées et retourne tous les tiers trouvés.
     *
     * @param entity         une entité liée à d'autres entités.
     * @param includeAnnuled <b>vrai</b> s'il faut tenir compte des liens annulés (utile dans le cas d'une annulation de rapport-entre-tiers, par exemple); ou <b>faux</b> s'il ne faut pas en tenir
     *                       compte.
     * @return l'ensemble des tiers trouvés; ou un ensemble vide si aucun tiers n'est trouvé.
     */
    Set<Tiers> getLinkedTiers(LinkedEntity entity, boolean includeAnnuled);

    /**
     * permet d'adapter la date de début de validité de la première périodicité en fonction d'une date
     */
    void adaptPremierePeriodicite(DebiteurPrestationImposable debiteurPrestationImposable, RegDate dateDebut);

    /**
     * Retourne le nom de la collectivité administrative en paramètre
     *
     * @param collId le numéro d'une collectivité administrative
     * @return le nom de la collectivité adminsitrative
     */
    String getNomCollectiviteAdministrative(int collId);

    /**
     * Retourne le nom de la collectivité administrative en paramètre
     *
     * @param collectiviteAdministrative une collectivité administrative
     * @return le nom de la collectivité adminsitrative
     */
    String getNomCollectiviteAdministrative(CollectiviteAdministrative collectiviteAdministrative);

    /**
     * Permet de savoir si un contribuable possède un for hors canton à une date données
     *
     * @param contribuable à analyser
     * @param date         référence
     * @return
     */
    boolean isHorsCanton(Contribuable contribuable, RegDate date);

    /**
     * @param tiers
     * @return <code>true</code> si le dernier for fiscal principal du contribuable a bien été fermé pour motif "séparation", <code>false</code> sinon
     */
    boolean isDernierForFiscalPrincipalFermePourSeparation(Tiers tiers);

    /**
     * @param menage un ménage commun
     * @param date   une date
     * @return <b>vrai</b> si le ménage possède au moins une personne physique active à la date spécifiée; <b>faux</b> autrement.
     */
    boolean isMenageActif(MenageCommun menage, @Nullable RegDate date);

    /**
     * Retourne la liste des parents d'un contribuable
     *
     * @param personnePhysique un personne physique
     * @param yComprisRelationsAnnulees <code>true</code> si on veut toutes les relations existantes, <code>false</code> si on ne veut que les relations non-annulées
     * @return la liste des rapports de parenté trouvés.
     */
    @NotNull
    List<Parente> getParents(PersonnePhysique enfant, boolean yComprisRelationsAnnulees);

	@NotNull
	List<PersonnePhysique> getParents(PersonnePhysique enfant, RegDate dateValidite);

	/**
	 * Retourne la liste des enfants d'un contribuable
	 *
	 * @param personnePhysique un personne physique
	 * @param yComprisRelationsAnnulees <code>true</code> si on veut toutes les relations existantes, <code>false</code> si on ne veut que les relations non-annulées
	 * @return la liste des rapports de parenté trouvés.
	 */
	@NotNull
	List<Parente> getEnfants(PersonnePhysique parent, boolean yComprisRelationsAnnulees);

	/**
	 * Rafraîchissement des parentés fiscales depuis les données civiles de l'individu indiqué.
	 * <ul>
	 *     <li>les parentés vers les parents de l'individu sont de toute façon rafraîchies&nbsp;;</li>
	 *     <li>les parentés déjà connues vers les enfants de l'individu sont rafraîchies (on ne crée donc jamais de parenté vers un enfant ici !)&nbsp;;</li>
	 *     <li>on ne fait rien du tout si l'individu ne correspond à aucune personne physique.</li>
	 * </ul>
	 * @param noIndividu numéro technique de l'individu civil
	 * @return les parentés créées/annulées et les erreurs rencontrées
	 * @throws PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException si le numéro d'individu correspond à plusieurs personnes physiques non annulées/désactivées
	 */
	ParenteUpdateResult refreshParentesDepuisNumeroIndividu(long noIndividu);

	/**
	 * Marquage des tiers associés au numéro d'individu donnu comme "nécessitant un recalcul des parentés".
	 * <ul>
	 *     <li>le(s) tiers(s) correspondant(s) directement au numéro d'individu est/sont de toute façon marqué(s)&nbsp;;</li>
	 *     <li>les tiers enfants connus sont également marqués&nbsp;;</li>
	 *     <li>on ne fait rien du tout si l'individu ne correspond à aucune personne physique.</li>
	 * </ul>
	 * @param noIndividu numéro technique de l'individu civil
	 * @throws PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException si on se heurte à un numéro d'individu partagé par plusieurs personnes physiques non annulées/désactivées durant le processus
	 */
	void markParentesDirtyDepuisNumeroIndividu(long noIndividu);

	/**
	 * Rafraîchissement des parentés fiscales depuis les données civiles de l'individu derrière la personne physique donnée
	 * <ul>
	 *     <li>les parentés vers les parents (= ascendantes) de l'individu sont de toute façon rafraîchies&nbsp;;</li>
	 *     <li>les parentés déjà connues vers les enfants (= descendantes) de l'individu sont rafraîchies sur demande du flag <code>enfantsAussi</code> (on ne crée donc jamais de parenté vers un enfant ici !)&nbsp;;</li>
	 *     <li>on ne fait rien du tout si la personne physique est <code>null</code> ou si elle n'a pas de numéro d'individu civil.</li>
	 * </ul>
	 * @param pp            personne physique dont les parentés doivent être rafraîchies
	 * @param enfantsAussi  <code>false</code> si seules les parentés ascendantes sont rafraîchies, <code>true</code> pour rafraîchir également les parentés descendantes
	 * @return les parentés créées/annulées et les erreurs rencontrées
	 * @throws PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException si on se heurte à un numéro d'individu partagé par plusieurs personnes physiques non annulées/désactivées durant le processus
	 */
	ParenteUpdateResult refreshParentesSurPersonnePhysique(PersonnePhysique pp, boolean enfantsAussi);

	/**
	 * Rafraîchissement des parentés fiscales depuis les données civiles de l'individu derrière la personne physique donnée
	 * <ul>
	 *     <li>seules les filiations (<i>i.e.</i> parentés vers les parents) sont mises à jour sur la personne physique donnée&nbsp;;</li>
	 *     <li>on ne fait rien du tout si la personne physique est <code>null</code> ou si elle n'a pas de numéro d'individu civil.</li>
	 * </ul>
	 * @param pp personne physique dont les parentés doivent être rafraîchies
	 * @return les parentés créées/annulées et les erreurs rencontrées
	 * @throws PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException si on se heurte à un numéro d'individu partagé par plusieurs personnes physiques non annulées/désactivées durant le processus
	 */
	ParenteUpdateResult initParentesDepuisFiliationsCiviles(PersonnePhysique pp);

	/**
	 * Renvoie les numéros d'individus liés par une relation de parenté au(x) contribuable(s) dont le numéro d'individu est donné (lien direct)
	 * @param noIndividuSource le numéro d'individu origine des relations de parenté recherchées
	 * @return ensemble non-null des numéros d'individus liés (le numéro d'individu source n'est pas inclus)
	 */
	Set<Long> getNumerosIndividusLiesParParente(long noIndividuSource);

	/**
     * Permet de traiter la éouverture d'un for fiscal d'un débiteur. Entraine également la réouverture
     * de tous les rapports de travail fermés à la même date que le for.
     *
     * @param forDebiteur le for à réouvrir
     */
    void reouvrirForDebiteur(@NotNull ForDebiteurPrestationImposable forDebiteur);

    /**
     * Détermine et retourne les numéros de tiers des offices d'impôt de district et de région pour une commune donnée.
     *
     * @param noOfs le numéro Ofs d'une commune
     * @param date  la date de valeur voulue
     * @return les numéros de tiers des offices d'impôt de district et de région; ou <b>null</b> si la commune n'est pas vaudoise ou est inconnue.
     */
    NumerosOfficesImpot getOfficesImpot(int noOfs, @Nullable RegDate date);

	/**
	 * Permet de récuperer toutes les rapports de préstation imposable couvrant une période donnée.
	 *
	 * @param dpi            un débiteur
	 * @param sourcier       un sourcier
	 * @param nonAnnuleOnly
	 * @param doNotAutoFlush
	 * @return la listes des rapports de prestations
	 */
	List<RapportPrestationImposable> getAllRapportPrestationImposable(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, boolean nonAnnuleOnly, boolean doNotAutoFlush);


	/**
	 * Créer une décision aci lié au contribuable passé en paramètre
 	 * @param ctb
	 * @param typeAutoriteFiscale
	 * @param numeroAutoritéFiscale
	 * @param dateDebut
	 * @param dateFin
	 * @param Remarque
	 * @return la décision créée
	 */
    DecisionAci addDecisionAci(Contribuable ctb, TypeAutoriteFiscale typeAutoriteFiscale, int numeroAutoriteFiscale, RegDate dateDebut, RegDate dateFin, String Remarque);

	/**
	 * Permet de mettre à jour un edécision ACI ou de l'annuler et la recréer en fonction des données modifiéss
	 * @param decisionAci
	 * @param dateFin
	 * @param remarque
	 * @param numeroAutoriteFiscale
	 * @return la décision modifiée ou une nouvelle décision avec les modifications,
	 */
	DecisionAci updateDecisionAci(DecisionAci decisionAci, RegDate dateFin, String remarque, Integer numeroAutoriteFiscale);

	/**
	 * Indique si un tiers dont le numero est passé en paramètre à une décision Aci En cours c.a.d sans date de fin
	 * @param idTiers numero de tiers à tester
	 * @return <b>true</b> si une décision en cours existe <b>false sinon</b>
	 */
	boolean hasDecisionAciEnCours(long idTiers);

	/**
	 * Indique si un tiers dont le numero est passé en paramètre à une décision Aci valide à une date donnée
	 * @param idTiers numero de tiers à tester
	 *@param date une date de référence
	 * @return <b>true</b> si une décision est valide à la date donnée <b>false sinon</b>
	 */
	boolean hasDecisionAciValidAt(long idTiers, RegDate date);

	/**
	 * Récupère tous les ménages communs dans lesquelles une personne physique a été impliqué
	 * @param pp la personne physique que l'on veut analyser
	 * @return la liste des ménages communs trouvés ou null
	 */
	List<MenageCommun> getAllMenagesCommuns(PersonnePhysique pp);

    /**
     * Determine si un ctb est sous l'influence d'une décision ACI: soit directement soit par une de ses relations directe
     * Couple ou membre du couple
     * @param ctb
     * @return vrai si une influence de décision a été détectée false sinon
     */
    boolean isSousInfluenceDecisions(Contribuable ctb);

	/**
	 * Crée un domicile sur l'établissement passé en paramètre, avec les informations fournies sans effectuer de contrôle.
	 * @param etb l'établissement destinataire
	 * @param typeAutoriteFiscale le type d'autorité fiscale du domicile
	 * @param numeroAutoriteFiscale le numéro d'autorité fiscale du domicile
	 * @param dateDebut la date de début
	 * @param dateFin la date de fin (en générale optionnelle)
	 * @return le domicile nouvellement créé
	 */
	DomicileEtablissement addDomicileEtablissement(Etablissement etb, TypeAutoriteFiscale typeAutoriteFiscale, int numeroAutoriteFiscale, RegDate dateDebut, RegDate dateFin);

	/**
	 * Ferme le domicile à la date indiquée
	 * @param domicile le domicile d'établissement à fermer
	 * @param dateFin la date de fin à utiliser
	 */
	void closeDomicileEtablissement(DomicileEtablissement domicile, RegDate dateFin);

	void closeRapportEntreTiers(RapportEntreTiers rapport, RegDate dateFin);

	/**
	 * Crée un allègement fiscal cantonal sur l'entreprise passée en paramètre, avec les informations fournies
	 * @param e l'entreprise destinataire
	 * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
	 * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
	 * @param dateDebut la date de début de la validité de l'allègement fiscal
	 * @param dateFin la date de fin de la validité de l'allègement fiscal
     * @param type le type d'allègement ICC
	 * @return l'allègement nouvellement créé
	 */
    AllegementFiscalCanton addAllegementFiscalCantonal(Entreprise e,
                                                       @Nullable BigDecimal pourcentageAllegement,
                                                       AllegementFiscal.TypeImpot typeImpot,
                                                       RegDate dateDebut,
                                                       RegDate dateFin,
                                                       AllegementFiscalCantonCommune.Type type);

	/**
	 * Crée un allègement fiscal communal sur l'entreprise passée en paramètre, avec les informations fournies
	 * @param e l'entreprise destinataire
	 * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
	 * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
	 * @param noOfsCommune le numéro ofs de la commune (vaudoise) (<code>null</code> pour "TOUTES")
	 * @param dateDebut la date de début de la validité de l'allègement fiscal
	 * @param dateFin la date de fin de la validité de l'allègement fiscal
     * @param type le type d'allègement ICC
	 * @return l'allègement nouvellement créé
	 */
    AllegementFiscalCommune addAllegementFiscalCommunal(Entreprise e,
                                                        @Nullable BigDecimal pourcentageAllegement,
                                                        AllegementFiscal.TypeImpot typeImpot,
                                                        RegDate dateDebut,
                                                        RegDate dateFin,
                                                        AllegementFiscalCantonCommune.Type type,
                                                        Integer noOfsCommune);

	/**
	 * Crée un allègement fiscal fédéral sur l'entreprise passée en paramètre, avec les informations fournies
	 * @param e l'entreprise destinataire
	 * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
	 * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
	 * @param dateDebut la date de début de la validité de l'allègement fiscal
	 * @param dateFin la date de fin de la validité de l'allègement fiscal
     * @param type le type d'allègement IFD
	 * @return l'allègement nouvellement créé
	 */
    AllegementFiscalConfederation addAllegementFiscalFederal(Entreprise e,
                                                             @Nullable BigDecimal pourcentageAllegement,
                                                             AllegementFiscal.TypeImpot typeImpot,
                                                             RegDate dateDebut,
                                                             RegDate dateFin,
                                                             AllegementFiscalConfederation.Type type);

    /**
     * Crée un allègement fiscal cantonal sur l'entreprise passée en paramètre, avec les informations fournies
     * @param e l'entreprise destinataire
     * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
     * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
     * @param dateDebut la date de début de la validité de l'allègement fiscal
     * @param type le type d'allègement ICC
     * @return l'allègement nouvellement créé
     */
    AllegementFiscalCanton openAllegementFiscalCantonal(Entreprise e,
                                                        @Nullable BigDecimal pourcentageAllegement,
                                                        AllegementFiscal.TypeImpot typeImpot,
                                                        RegDate dateDebut,
                                                        AllegementFiscalCantonCommune.Type type);

    /**
     * Crée un allègement fiscal cantonal sur l'entreprise passée en paramètre, avec les informations fournies
     * @param e l'entreprise destinataire
     * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
     * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
     * @param dateDebut la date de début de la validité de l'allègement fiscal
     * @param dateFin la date de fin de la validité de l'allègement fiscal
     * @param type le type d'allègement ICC
     * @return l'allègement nouvellement créé
     */
    AllegementFiscalCanton openAndCloseAllegementFiscalCantonal(Entreprise e,
                                                                @Nullable BigDecimal pourcentageAllegement,
                                                                AllegementFiscal.TypeImpot typeImpot,
                                                                RegDate dateDebut,
                                                                RegDate dateFin,
                                                                AllegementFiscalCantonCommune.Type type);

    /**
     * Crée un allègement fiscal sur l'entreprise passée en paramètre, avec les informations fournies
     * @param e l'entreprise destinataire
     * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
     * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
     * @param noOfsCommune le numéro ofs de la commune (vaudoise) (<code>null</code> pour "TOUTES")
     * @param dateDebut la date de début de la validité de l'allègement fiscal
     * @param type le type d'allègement ICC
     * @return l'allègement nouvellement créé
     */
    AllegementFiscalCommune openAllegementFiscalCommunal(Entreprise e,
                                                         @Nullable BigDecimal pourcentageAllegement,
                                                         AllegementFiscal.TypeImpot typeImpot,
                                                         Integer noOfsCommune,
                                                         RegDate dateDebut,
                                                         AllegementFiscalCantonCommune.Type type);

    /**
     * Crée un allègement fiscal sur l'entreprise passée en paramètre, avec les informations fournies
     * @param e l'entreprise destinataire
     * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
     * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
     * @param noOfsCommune le numéro ofs de la commune (vaudoise) (<code>null</code> pour "TOUTES")
     * @param dateDebut la date de début de la validité de l'allègement fiscal
     * @param dateFin la date de fin de la validité de l'allègement fiscal
     * @param type le type d'allègement ICC
     * @return l'allègement nouvellement créé
     */
    AllegementFiscalCommune openAndCloseAllegementFiscalCommunal(Entreprise e,
                                                                 @Nullable BigDecimal pourcentageAllegement,
                                                                 AllegementFiscal.TypeImpot typeImpot,
                                                                 Integer noOfsCommune,
                                                                 RegDate dateDebut,
                                                                 RegDate dateFin,
                                                                 AllegementFiscalCantonCommune.Type type);

    /**
     * Crée un allègement fiscal sur l'entreprise passée en paramètre, avec les informations fournies
     * @param e l'entreprise destinataire
     * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
     * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
     * @param dateDebut la date de début de la validité de l'allègement fiscal
     * @param type le type d'allègement IFD
     * @return l'allègement nouvellement créé
     */
    AllegementFiscalConfederation openAllegementFiscalFederal(Entreprise e,
                                                              @Nullable BigDecimal pourcentageAllegement,
                                                              AllegementFiscal.TypeImpot typeImpot,
                                                              RegDate dateDebut,
                                                              AllegementFiscalConfederation.Type type);

    /**
     * Crée un allègement fiscal sur l'entreprise passée en paramètre, avec les informations fournies
     * @param e l'entreprise destinataire
     * @param pourcentageAllegement le pourcentage d'allègement souhaité (0 -> 100)
     * @param typeImpot le type d'impôt concerné par l'allègement (<code>null</code> pour "TOUS")
     * @param dateDebut la date de début de la validité de l'allègement fiscal
     * @param dateFin la date de fin de la validité de l'allègement fiscal
     * @param type le type d'allègement IFD
     * @return l'allègement nouvellement créé
     */
    AllegementFiscalConfederation openAndCloseAllegementFiscalFederal(Entreprise e,
                                                                      @Nullable BigDecimal pourcentageAllegement,
                                                                      AllegementFiscal.TypeImpot typeImpot,
                                                                      RegDate dateDebut,
                                                                      RegDate dateFin,
                                                                      AllegementFiscalConfederation.Type type);

	/**
	 * Ferme l'allègement fiscal à la date indiquée
	 * @param af l'allègement fiscal à fermer
	 * @param dateFin la date de fin à utiliser
	 */
	void closeAllegementFiscal(AllegementFiscal af, RegDate dateFin);

    /**
     * Annule l'allègement fiscal passé en paramètre
     * @param af l'allègement fiscal à annuler
     */
    void annuleAllegementFiscal(AllegementFiscal af);

	/**
	 * Crée une nouvelle raison sociale pour l'entreprise passée en paramètre.
 	 * @param e l'entreprise concernée
	 * @param raisonSociale la nouvelle raison sociale
	 * @param dateDebut la date de début de validité de la nouvelle raison sociale
	 * @param dateFin la date de fin, ou null si la durée de validité est indéterminée
	 * @return la raison sociale nouvellement créée
	 */
	RaisonSocialeFiscaleEntreprise addRaisonSocialeFiscale(Entreprise e, String raisonSociale, RegDate dateDebut, RegDate dateFin) throws TiersException;

	/**
	 * Met à jour la raison sociale de l'entreprise. Seule le libellé peut être changé, car on ne
	 * peut fermer une raison sociale qu'en ouvrant la suivante.
	 * @param rs la raison sociale mise à jour
	 * @param raisonSociale le libellé de la nouvelle raison sociale
	 */
	void updateRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise rs, String raisonSociale) throws TiersException;

	/**
	 * Ferme la raison sociale à la date indiquée
	 * @param raisonSociale la raison sociale à fermer
	 * @param dateFin le dernier jour de validité
	 */
	void closeRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise raisonSociale, RegDate dateFin);

	/**
	 * Annule la raison sociale passée en paramètre
	 * @param raisonSociale la raison sociale à annuler
	 */
	void annuleRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise raisonSociale) throws TiersException;

	/**
	 * Crée une nouvelle forme juridique pour l'entreprise passée en paramètre.
	 * @param e l'entreprise concernée
	 * @param raisonSociale la nouvelle forme juridique
	 * @param dateDebut la date de début de validité de la nouvelle forme juridique
	 * @param dateFin la date de fin, ou null si la durée de validité est indéterminée
	 * @return la forme juridique nouvellement créée
	 */
	FormeJuridiqueFiscaleEntreprise addFormeJuridiqueFiscale(Entreprise e, FormeJuridiqueEntreprise formeJuridique, RegDate dateDebut, RegDate dateFin) throws TiersException;

	/**
	 * Met à jour la forme juridique de l'entreprise. Seule la forme peut être changée, car on ne
	 * peut fermer une forme juridique qu'en ouvrant la suivante.
	 * @param fj la forme juridique mise à jour
	 * @param formeJurique le libellé de la nouvelle forme juridique
	 */
	void updateFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise fj, FormeJuridiqueEntreprise formeJuridique) throws TiersException;

	/**
	 * Ferme la forme juridique à la date indiquée
	 * @param raisonSociale la raison forme juridique
	 * @param dateFin le dernier jour de validité
	 */
	void closeFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise formeJuridique, RegDate dateFin);

	/**
	 * Annule la forme juridique passée en paramètre
	 * @param raisonSociale la forme juridique à annuler
	 */
	void annuleFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise formeJuridique) throws TiersException;

	/**
	 * Crée un domicile pour l'établissement passé en paramètre. Si d'autre domicile sont présent, vérifie que
	 * le nouveau domicile est débute après le dernier et ferme celui-ci s'il y a lieu.
	 *
	 * @param etablissement l'établissement concerné
	 * @param typeAutorite le type d'autorité fiscale du domicile
	 * @param noOfs le numéro OFS de la commune ou pays
	 * @param dateDebut la date de début de validité du nouveau domicile
	 * @param dateFin la date de fin optionnelle du nouveau domicile
	 * @return le domicile créé
	 */
	DomicileEtablissement addDomicileFiscal(Etablissement etablissement, TypeAutoriteFiscale typeAutorite, Integer noOfs, RegDate dateDebut, RegDate dateFin) throws TiersException;

	/**
	 * Met à jour le domicile fiscal
	 * @param domicile le domicile visé
	 * @param typeAutorite le nouveau type d'autorité fiscale
	 * @param noOfs le nouveau numéro OFS de la commune ou du pays
	 * @return le (nouveau) domicile fiscal représentant le domicile édité
	 */
	DomicileEtablissement updateDomicileFiscal(DomicileEtablissement domicile, TypeAutoriteFiscale typeAutorite, Integer noOfs) throws TiersException;

	/**
	 * Met à jour le domicile fiscal, avec une date de fin. Cette opération n'est permise que sur le dernier domicile.
	 * @param domicile le domicile visé
	 * @param dateFin la nouvelle date de fin
	 * @param typeAutorite le nouveau type d'autorité fiscale
	 * @param noOfs le nouveau numéro OFS de la commune ou du pays
	 * @return le (nouveau) domicile fiscal représentant le domicile édité
	 */
	DomicileEtablissement updateDomicileFiscal(DomicileEtablissement domicile, TypeAutoriteFiscale typeAutorite, Integer noOfs, RegDate dateFin) throws TiersException;

	/**
	 * Ferme le domicile fiscal à la date indiquée. Contrôle qu'il s'agit bien du dernier.
 	 * @param domicile le domicile visé
	 * @param dateFin le dernier jour de validité
	 */
	void closeDomicileFiscal(DomicileEtablissement domicile, RegDate dateFin) throws TiersException;

	/**
	 * Annule le domicile fiscal
	 * @param domicile le domicile à annuler
	 */
	void annuleDomicileFiscal(DomicileEtablissement domicile) throws TiersException;

	/**
	 * Crée un nouveau capital pour l'entreprise passée en paramètre.
	 * @param e l'entreprise concernée
	 * @param raisonSociale le nouveau capital
	 * @param dateDebut la date de début de validité du nouveau capital
	 * @param dateFin la date de fin, ou null si la durée de validité est indéterminée
	 * @return le capital nouvellement créé
	 */
	CapitalFiscalEntreprise addCapitalFiscal(Entreprise e, Long montant, String monnaie, RegDate dateDebut, RegDate dateFin) throws TiersException;

	/**
	 * Met à jour le capital de l'entreprise. On peut changer le montant et la date de fin, mais pas
	 * la monnaie, pour éviter les risques de confusion.
	 * @param cf le capital à mettre à jour
	 * @param montant un nouveau montant
	 * @param dateFin une nouvelle date de fin
	 */
	void updateCapitalFiscal(CapitalFiscalEntreprise cf, Long montant, String monnaie, RegDate dateFin) throws TiersException;

	/**
	 * Ferme le capital à la date indiquée
	 * @param capital le capital
	 * @param dateFin le dernier jour de validité
	 */
	void closeCapitalFiscal(CapitalFiscalEntreprise capital, RegDate dateFin);

	/**
	 * Annule le capital passé en paramètre
	 * @param capital le capital à annuler
	 */
	void annuleCapitalFiscal(CapitalFiscalEntreprise capital) throws TiersException;

	/**
     * Crée un régime fiscal sur l'entreprise passée en paramètre avec les informations fournies
     * @param e l'entreprise destinataire
     * @param portee la portée du régime fiscal
     * @param type le type de régime fiscal
     * @param dateDebut la date de début de validité du régime fiscal
     * @param dateFin la date de fin de validité du régime fiscal
     * @return le régime fiscal nouvellement créé
     */
    RegimeFiscal addRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut, RegDate dateFin);

    /**
     * Crée un régime fiscal valide depuis la date donnée avec les informations fournies
     * @param e l'entreprise destinataire
     * @param portee la portée du régime fiscal
     * @param type le type de régime fiscal
     * @param dateDebut la date de début de validité du régime fiscal
     * @return le régime fiscal nouvellement créé
     */
    RegimeFiscal openRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut);

    /**
     * Crée un régime fiscal valide dans la période donnée avec les informations fournies
     * @param e l'entreprise destinataire
     * @param portee la portée du régime fiscal
     * @param type le type de régime fiscal
     * @param dateDebut la date de début de validité du régime fiscal
     * @param dateFin la date de fin de validité du régime fiscal
     * @return le régime fiscal nouvellement créé
     */
    RegimeFiscal openAndCloseRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut, RegDate dateFin);

    /**
     * Ferme le régime fiscal à la date indiquée
     * @param rf le régime fiscal à fermer
     * @param dateFin la date de fin à utiliser
     */
    void closeRegimeFiscal(RegimeFiscal rf, RegDate dateFin);

    /**
     * Annule le régime fiscal passé en paramètre
     * @param rf le régime fiscal à annuler
     */
    void annuleRegimeFiscal(RegimeFiscal rf);

    /**
     * Crée un nouveau flag sur l'entreprise avec les données fournies
     * @param e entreprise destinataire
     * @param type le type de flag
     * @param dateDebut la date de début de validité du flag (incluse)
     * @param dateFin (optionnelle) la date de fin de validité du flag (incluse)
     * @return le flag nouvellement créé
     */
    FlagEntreprise addFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut, @Nullable RegDate dateFin);

    /**
     * Crée un flag valide depuis la date donnée avec les informations fournies
     * @param e entreprise destinataire
     * @param type le type de flag
     * @param dateDebut la date de début de validité du flag (incluse)
     * @return le flag nouvellement créé
     */
    FlagEntreprise openFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut);

    /**
     * Crée un flag valide sur la période donnée avec les informations fournies
     * @param e entreprise destinataire
     * @param type le type de flag
     * @param dateDebut la date de début de validité du flag (incluse)
     * @param dateFin la date de fin de validité du flag (incluse)
     * @return le flag nouvellement créé
     */
    FlagEntreprise openAndCloseFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut, RegDate dateFin);

    /**
     * Ferme le flag à l'année indiquée (incluse)
     * @param flag le flag à clôturer
     * @param dateFin date de clôture
     */
    void closeFlagEntreprise(FlagEntreprise flag, RegDate dateFin);

    /**
     * Annule le flag entreprise passé en paramètre
     * @param flag le flag à annuler
     */
    void annuleFlagEntreprise(FlagEntreprise flag);

	/**
	 * Ajoute un mandat en base avec modification des autres mandats (et adresses mandataires, le cas échéant) similaires du mandant
	 * @param mandant le mandant
	 * @param mandat le nouveau mandat
	 */
	void addMandat(Contribuable mandant, Mandat mandat);

	/**
	 * Ajoute un mandat sous la forme d'une adresse de mandataire, en modifiant si nécessaire les mandats et autres adresses de mandataires similaires existants sur le mandant
	 * @param mandant le mandant
	 * @param mandat la nouvelle adresse mandataire
	 */
	void addMandat(Contribuable mandant, AdresseMandataire mandat);

    /**
     * L'historiques des données civiles fourni par RCEnt (en gros, rien avant leur reprise, quelque part en 2015...) ne nous permet pas de donner
     * avec certitude les périodes pendant lesquelles une société avait une forme juridique correspondant à une société de personnes. En revanche,
     * nous pouvons fournir les périodes connues pendant lesquelles on est sûr qu'elle n'était pas dans ce cas, pour au moins ne relâcher certaines
     * contraintes qu'en dehors de ces périodes...
     * @param entreprise une entreprise
     * @return la liste des périodes (triées, non chevauchantes) pendant lesquelles on est certain que la société n'est pas une société de personnes (= SNC, SC) ni individuelle
     */
    List<DateRange> getPeriodesNonSocieteDePersonnesNiIndividuelle(Entreprise entreprise);

    /**
     * Permet de récuperer touts les contribuables ayant un lien d'au plus 5 ans avec le contribuable dont l'id est passéen paramètre
     * @param ctb
     * @param ageLiaison
     * @return
     */
    Set<Contribuable> getContribuablesLies(Contribuable ctb, Integer ageLiaison);

    /**
     * @param entreprise une entreprise fiscale
     * @return les données civiles de l'entreprise, ou <code>null</code> si cette entreprise est inconnue dans les registres civils
     */
    Organisation getOrganisation(@NotNull Entreprise entreprise);

    Organisation getOrganisationPourEtablissement(@NotNull Etablissement etablissement);

	/**
	 * Renvoie le site d'organisation RCEnt correspondant à l'établissement.
	 * @param etablissement L'établissement connu au civil
	 * @return Le site correspondant
	 */
	SiteOrganisation getSiteOrganisationPourEtablissement(@NotNull Etablissement etablissement);

	/**
	 * @param entreprise une entreprise
	 * @return la date d'inscription au RC (ou, à défaut, la date de fondation) de l'entreprise
	 */
	RegDate getDateCreation(@NotNull Entreprise entreprise);

	/**
	 * Renvoie le dernier numéro IDE associé à une entreprise dans le registre civil ou fiscal.
	 * Si l'entreprise est associée au registre civil, c'est le numéro rapporté par ce dernier qui est retourné.
	 * Sinon, c'est le numéro trouvé dans le registre fiscal qui est renvoyé.
	 *
     * @param entreprise une entreprise fiscale
     * @return le dernier numéro IDE associé à l'entreprise, ou <code>null</code> si on n'en connait aucun
     */
    @Nullable
    String getNumeroIDE(@NotNull Entreprise entreprise);

	/**
	 * Renvoie le dernier numéro IDE associé à un établissement dans le registre civil ou fiscal.
	 * Le registre civil est prépondérant.
	 *
	 * @param etablissement une établissement fiscale
	 * @return le dernier numéro IDE associé à l'établissement, ou <code>null</code> si on n'en connait aucun
	 */
	@Nullable
	String getNumeroIDE(@NotNull Etablissement etablissement);

	/**
     * @param entreprise une entreprise fiscale
     * @return la liste des données de capital de l'entreprise (en tenant compte des données civiles et d'éventuelles surcharges fiscales)
     */
    List<CapitalHisto> getCapitaux(@NotNull Entreprise entreprise, boolean aussiAnnule);

    /**
     * @param entreprise une entreprise (fiscale)
     * @param date une date de référence (si <code>null</code>, on prendra la date du jour)
     * @return la catégorie d'entreprise associée, à la date donnée, à l'entreprise donnée
     */
    CategorieEntreprise getCategorieEntreprise(@NotNull Entreprise entreprise, RegDate date);

    /**
     * @param entreprise une entreprise (fiscale)
     * @return l'historique des catégories d'entreprise
     */
    List<CategorieEntrepriseHisto> getCategoriesEntrepriseHisto(@NotNull Entreprise entreprise);

	/**
	 * Pour l'entreprise connue au civil, crée des surcharges en base des données civiles pour la période fournie avec les valeurs
	 * retournée par RCEnt à la date de valeur.
	 *
	 * Exemple d'utilisation: combler le décalage de valeur qui existe entre la date d'inscription au RC et la première photo de
	 * l'entreprise dans RCEnt.
	 *
	 * @param entreprise L'entreprise concernée
	 * @param range La période pour laquelle les valeurs civiles doivent être surchargées. La période doit avoir une date de fin.
	 * @param dateValeur La date à utiliser pour rechercher les valeurs
	 */
	void appliqueDonneesCivilesSurPeriode(Entreprise entreprise, DateRange range, RegDate dateValeur) throws TiersException;

	/**
	 * Pour l'établissement connue au civil, crée des surcharges en base des données civiles pour la période fournie avec les valeurs
	 * retournée par RCEnt à la date de valeur.
	 *
	 * @param etablissement L'etablissement concerné
	 * @param range La période pour laquelle les valeurs civiles doivent être surchargées. La période doit avoir une date de fin.
	 * @param dateValeur La date à utiliser pour rechercher les valeurs
	 */
	void appliqueDonneesCivilesSurPeriode(Etablissement etablissement, DateRange range, RegDate dateValeur) throws TiersException;
}

