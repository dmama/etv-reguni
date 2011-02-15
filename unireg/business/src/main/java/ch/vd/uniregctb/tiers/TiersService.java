package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurMenagesResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurPersonnesPhysiquesResults;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Fournit les differents services d'accès aux données du Tiers.
 */
@SuppressWarnings({"JavadocReference"})
public interface TiersService {

	public TiersDAO getTiersDAO();

	public ServiceInfrastructureService getServiceInfra();

	public ServiceCivilService getServiceCivilService();

	/**
	 * Recherche les Tiers correspondants aux critères dans le data model de Unireg
	 *
	 * @param tiersCriteria les critères de recherche
	 * @return la liste des tiers correspondants aux criteres.
	 * @throws IndexerException en cas d'impossibilité d'exécuter la recherche
	 */
	public List<TiersIndexedData> search(TiersCriteria tiersCriteria) throws IndexerException;

	/**
	 * Renvoie la personne physique correspondant au numéro d'individu passé en paramètre.
	 *
	 * @param numeroIndividu le numéro de l'individu.
	 * @return la personne physique (tiers non-annulé) correspondante au numéro d'individu passé en paramètre, ou <b>null</b>.
	 */
	public PersonnePhysique getPersonnePhysiqueByNumeroIndividu(long numeroIndividu);

	/**
	 * Retourne un tiers en fonction de son numéro de tiers.
	 *
	 * @param numeroTiers
	 *            le numéro de tiers (= numéro de contribuable, sauf dans le cas du débiteur prestation imposable)
	 * @return le tiers trouvé, ou null si aucun tiers ne possède ce numéro.
	 */
	public Tiers getTiers(long numeroTiers);

	/**
	 * Ré-initialise les champs NAVS11 et NumRCE du non-habitant donné
	 * @param nonHabitant le tiers non-habitant sur lequel les identifiants vont être assignés
	 * @param navs11 NAVS11 (potentiellement avec points...)
	 * @param numRce numéro du registre des étrangers
	 */
	public void setIdentifiantsPersonne(PersonnePhysique nonHabitant, String navs11, String numRce);

	/**
	 * Change un non Habitant (qui n'a jamais été habitant) en ménage.
	 * Méthode a utiliser qu'en cas de strict necessité
	 *
	 * @param numeroTiers
	 *            le numéro de tiers (= numéro de contribuable de la PP non habitant)
	 *
	 */
	public void changeNHenMenage(long numeroTiers);

	/**
	 * change un non habitant en habitant (en cas d'arrivée HC ou HS)
	 *
	 * @param nonHabitant la PP de type nonHabitant
	 * @param numInd      le numéro d'individu de l'habitant
	 * @param date        la dte du changement (si aucune date donnée, ni les situations de famille ni les adresses ne seront modifiées = rattrapage!)
	 * @return la même PP de type habitant maintenant
	 */
	public PersonnePhysique changeNHenHabitant(PersonnePhysique nonHabitant, Long numInd, RegDate date);

	/**
	 * change un habitant en non habitant (en cas de départ HC ou HS)
	 *
	 * @param habitant un habitant
	 * @return l'habitant transformé en non-habitant
	 */
	public PersonnePhysique changeHabitantenNH(PersonnePhysique habitant);

	/**
	 * @param pp personne physique dont on veut connaître la localisation du domicile
	 * @param date date de référence pour le domicile du contribuable
	 * @return <code>true</code> si l'adresse de domicile associée à la personne physique donnée est sur le canton de vaud, false sinon
	 */
	public boolean isDomicileVaudois(PersonnePhysique pp, RegDate date);

	/**
	 * Change un habitant en non-habitant s'il est actuellement domicilié hors du canton de Vaud
	 * @param pp la personne physique
	 * @return <code>true</code> si un changement de flag habitant a eu lieu
	 */
	public boolean changeHabitantEnNHSiDomicilieHorsDuCanton(PersonnePhysique pp);

	/**
	 * Change un non-habitant en habitant s'il est actuellement domicilié sur le canton de Vaud
	 * (et qu'il a déjà un numéro d'individu)
	 * @param pp la personne physique
	 * @param dateArrivee
	 * @return <code>true</code> si un changement de flag habitant a eu lieu
	 */
	public boolean changeNHEnHabitantSiDomicilieDansLeCanton(PersonnePhysique pp, RegDate dateArrivee);

	/**
	 * Retourne le contribuable <i>père</i> (au sens civil du terme) du contribuable spécifié.
	 * <p/>
	 * Si le contribuable spécifié est inconnu au contrôle des habitants, la valeur retournée est nulle. D'autre part, n'est retourné une valeur qui le père est lui-même contribuable.
	 *
	 * @param pp           une personne physique
	 * @param dateValidite la date de validité des données retournées
	 * @return un contribuable, ou <b>null</b> selon les cas.
	 */
	public PersonnePhysique getPere(PersonnePhysique pp, RegDate dateValidite);

	/**
	 * Retourne le contribuable <i>mère</i> (au sens civil du terme) du contribuable spécifié.
	 * <p/>
	 * Si le contribuable spécifié est inconnu au contrôle des habitants, la valeur retournée est nulle. D'autre part, n'est retourné une valeur qui la mère est elle-même contribuable.
	 *
	 * @param pp           une personne physique
	 * @param dateValidite la date de validité des données retournées
	 * @return un contribuable, ou <b>null</b> selon les cas.
	 */
	public PersonnePhysique getMere(PersonnePhysique pp, RegDate dateValidite);

	/**
	 * Retourne la liste des contribuables <i>parents</i> (au sens civil du terme) du contribuable spécifié.
	 * <p/>
	 * Si le contribuable spécifié est inconnu au contrôle des habitants, la liste retournée est vide. D'autre part, ne sont retournés que les parents qui sont eux-mêmes contribuables.
	 *
	 * @param pp           une personne physique
	 * @param dateValidite la date de validité des données retournées
	 * @return une liste de contribuables, qui peut contenir 0 ou plusieurs contribuables selon les cas.
	 */
	public List<PersonnePhysique> getParents(PersonnePhysique pp, RegDate dateValidite);

	/**
	 * Retourne la liste des contribuables <i>enfants</i> (au sens civil du terme) du contribuable spécifié.
	 * <p/>
	 * Si le contribuable spécifié est inconnu au contrôle des habitants, la liste retournée est vide. D'autre part, ne sont retournés que les enfants qui sont eux-mêmes contribuables.
	 *
	 * @param pp           une personne physique
	 * @param dateValidite la date de validité des données retournées
	 * @return une liste de contribuables, qui peut contenir 0 ou plusieurs contribuables selon les cas.
	 */
	public List<PersonnePhysique> getEnfants(PersonnePhysique pp, RegDate dateValidite);

	/**
	 * Détermine et retourne le contribuable (la mère ou ménage-commun de la mère) qui possède l'autorité parentale du contribuable spécifié [UNIREG-3244].
	 * <p/>
	 * <b>Note:</b> le contribuable est supposé non-majeur à la date de validité spécifiée
	 *
	 * @param contribuableEnfant un contribuable enfant
	 * @param dateValidite       une date de validité
	 * @return le contribuable personne physique ou ménage-commun qui possède l'autorité parentale; ou <b>null</b> si la mère est inconnue.
	 */
	public Contribuable getAutoriteParentaleDe(PersonnePhysique contribuableEnfant, RegDate dateValidite);

	/**
	 * Renvoie la collectivité administrative rattachée au numero de collectivité donné.
	 *
	 * @param noTechnique
	 *            le numero de la collectivité
	 * @return le tiers représentant la collectivité administrative correspondant
	 */
	public CollectiviteAdministrative getCollectiviteAdministrative(int noTechnique);
	public CollectiviteAdministrative getCollectiviteAdministrative(int noTechnique, boolean doNotAutoFlush);

	/**
	 * Récupère le tiers correspondant à la collectivite administrative avec un numéro donné (crée le tiers s'il n'existe pas).
	 *
	 * @param noTechnique
	 *            le numero technique de la collectivite administrative
	 * @return le tiers correspondant à la collectivite administrative
	 */
	public CollectiviteAdministrative getOrCreateCollectiviteAdministrative(int noTechnique);
	public CollectiviteAdministrative getOrCreateCollectiviteAdministrative(int noTechnique, boolean doNotAutoFlush);

	/**
	 * Recupere l'individu correspondant à une personne physique
	 *
	 * @param personne la personne physique en question.
	 * @return un individu, ou <i>null</i> si la personne physique est un non-habitant.
	 */
	public Individu getIndividu(PersonnePhysique personne);

	/**
	 * Recupere l'individu correspondant à une personne physique avec l'état valide pour une année donnée.
	 *
	 * @param personne   la personne physique en question.
	 * @param annee      l'année de validité des données retournées.
	 * @param attributes les attributs renseignés sur l'individu.
	 * @return un individu, ou <i>null</i> si la personne physique est un non-habitant.
	 */
	public Individu getIndividu(PersonnePhysique personne, int annee, AttributeIndividu[] attributes);

	/**
	 * Détermine si une personne physique est suisse.
	 *
	 * @param pp
	 *            la personne physique.
	 * @param date
	 *            la date à laquelle on désire se placer
	 * @return true si la personne physique a la nationalité suisse à la date donnée.
	 * @throws TiersException
	 *             si la nationalite ne peut être déterminée
	 */
	public boolean isSuisse(PersonnePhysique pp, RegDate date) throws TiersException;

	/**
	 * Détermine si une personne physique est suisse, possède un permis C ou est réfugiée.
	 *
	 * @param pp            une personne physique
	 * @param dateEvenement la date à laquelle on désire connaître cette information
	 * @return <b>true</b> si la personne physique possède la nationalité suisse, ou si elle possède un permis C ou si elle est réfugiée; <b>false</b> autrement.
	 * @throws TiersException si la nationalite ne peut être déterminée
	 */
	boolean isSuisseOuPermisCOuRefugie(PersonnePhysique pp, RegDate dateEvenement) throws TiersException;

	/**
	 * Détermine si un individu est suisse.
	 *
	 * @param individu un individu
	 * @param date     la date à laquelle on désire se placer
	 * @return <b>vrai</b> si l'individu possède la nationalité Suisse à la date spécifiée; <b>faux</b> autrement.
	 * @throws TiersException si la nationalite ne peut être déterminée
	 */
	public boolean isSuisse(Individu individu, RegDate date) throws TiersException;

	/**
	 * L'individu est-t-il avec permis C en cours de validité ?
	 *
	 * @param individu
	 *            l'individu
	 * @return true si l'individu n'a pas de permis C en cours
	 */
	public boolean isAvecPermisC(Individu individu);

	/**
	 * L'individu est-t-il avec permis C en cours de validité ?
	 *
	 * @param individu
	 *            l'individu
	 * @param date
	 *            la date à laquelle on désire se placer
	 * @return true si l'individu n'a pas de permis C en cours
	 */
	public boolean isAvecPermisC(Individu individu, RegDate date);

	/**
	 * Détermine si un habitant avec permis C.
	 *
	 * @param habitant
	 *            l'habitant
	 * @param date
	 * 				la date à laquelle on souhaite se placer
	 * @return true si l'habitant est étrangère avec permis C à la date donnée
	 * @throws TiersException
	 *             si la nationalite ne peut être déterminée
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public boolean isHabitantEtrangerAvecPermisC (PersonnePhysique habitant, RegDate date) throws TiersException;

	/**
	 * Détermine si une personne physique est étrangère sans permis C.
	 *
	 * @param pp
	 *            la personne physique
	 * @param date
	 * 				la date à laquelle on souhaite se placer
	 * @return true si la personne physique est étrangère sans permis C à la date donnée
	 * @throws TiersException
	 *             si la nationalite ne peut être déterminée
	 */
	public boolean isEtrangerSansPermisC(PersonnePhysique pp, RegDate date) throws TiersException;

	/**
	 * Détermine si une personne physique est réfugié.
	 *
	 * @param habitant
	 *            la personne physique
	 * @param date
	 * 				la date à laquelle on souhaite se placer
	 * @return true si la personne physique est réfugié à la date donnée
	 * @throws TiersException
	 *             si le permis ne peut être déterminée
	 */
	public boolean isHabitantRefugie(PersonnePhysique habitant, RegDate date) throws TiersException;

	/**
	 * Détermination de l'individidu secondaire
	 * <ul>
	 * <li>2 personnes de meme sexe : le deuxieme dans l'ordre alphabétique est le secondaire</li>
	 * <li>2 personnes de sexe different : la femme est le secondaire</li>
	 * </ul>
	 * @param tiers1 une personne physique.
	 * @param tiers2 une autre personne physique.
	 * @return la personne physique principale.
	 */
	public PersonnePhysique getPrincipal(PersonnePhysique tiers1, PersonnePhysique tiers2);

	/**
	 * Détermination de l'individidu principal du ménage
	 * <ul>
	 * <li>2 personnes de meme sexe : le premier dans l'ordre alphabétique est le principal</li>
	 * <li>2 personnes de sexe different : l'homme est le principal</li>
	 * </ul>
	 * @param menageCommun un ménage commun.
	 * @return la personne physique principale du ménage.
	 */
	public PersonnePhysique getPrincipal(MenageCommun menageCommun);

	/**
	 * Recherche le ménage commun d'une personne physique à une date donnée.
	 *
	 * @param personne
	 *            la personne dont on recherche le ménage.
	 *
	 * @param date
	 *            la date de référence, ou null pour obtenir le ménage courant.
	 * @return le ménage common dont la personne est membre à la date donnée, ou <b>null<b> si aucun ménage n'a été trouvé.
	 */
	public MenageCommun findMenageCommun(PersonnePhysique personne, RegDate date);

	/**
	 * Recherche le dernier ménage commun d'une personne physique.
	 *
	 * @param personne
	 *            la personne dont on recherche le ménage.
	 * @return le dernier ménage common dont la personne est membre, ou <b>null<b> si aucun ménage n'a été trouvé.
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public MenageCommun findDernierMenageCommun(PersonnePhysique personne);

	/**
	 * Détermine si une personne physique fait partie d'un ménage commun à une date donnée.
	 *
	 * @param personne
	 *            la personne physique.
	 *
	 * @param date
	 *            la date de référence, ou null pour obtenir le ménage courant.
	 * @return true si la personne physique est membre d'un ménage commun à la date donnée.
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public boolean isInMenageCommun(PersonnePhysique personne, RegDate date);

	/**
	 * Contruit l'ensemble des tiers individuels et tiers menage à partir du tiers ménage-commun.
	 *
	 * @param menageCommun
	 *            le tiers ménage-commun du menage
	 * @param date
	 *            la date de référence, ou null pour obtenir tous les composants connus dans l'histoire du ménage.
	 * @return un objet EnsembleTiersCouple regroupant l'ensemble des tiers individuels et tiers menage.
	 */
	public EnsembleTiersCouple getEnsembleTiersCouple(MenageCommun menageCommun, RegDate date);

	/**
	 * Contruit l'ensemble des tiers individuels et tiers menage à partir du tiers ménage-commun.
	 *
	 * @param menageCommun
	 *            le tiers ménage-commun du menage
	 * @param anneePeriode
	 *            la période fiscale considérée pour déterminer les composants du couple. Chacun des composants du couple est pris en compte
	 *            pour autant qu'il soit valide durant la période.
	 * @return un objet EnsembleTiersCouple regroupant l'ensemble des tiers individuels et tiers menage.
	 */
	public EnsembleTiersCouple getEnsembleTiersCouple(MenageCommun menageCommun, int anneePeriode);

	/**
	 * Contruit l'ensemble des tiers individuels et tiers menage à partir d'un habitant membre du menage.
	 *
	 * @param personne
	 *            le tiers membre du menage
	 * @param date
	 *            la date de référence, ou null pour obtenir l'ensemble actif
	 * @return un objet EnsembleTiersCouple regroupant l'ensemble des tiers individuels et tiers menage, ou null si la personne n'appartient
	 *         pas à un ménage.
	 */
	public EnsembleTiersCouple getEnsembleTiersCouple(PersonnePhysique personne, RegDate date);

	/**
	 * Ajoute l'individu spécifié en tant que tiers du ménage commun, à partir de la date spécifiée.
	 * <p>
	 * <b>Attention : le menage et le tiers spécifiés seront automatiques sauvés !</b>
	 *
	 * @param menage
	 *            le ménage sur lequel le tiers doit être ajouté
	 * @param tiers
	 *            le tiers à ajouter au ménage
	 * @param dateDebut
	 *            la date de début de validité de la relation entre tiers
	 * @param dateFin
	 *            la date de fin de validité de la relation entre tiers (peut être nulle)
	 * @return le rapport-entre-tiers avec les références mises-à-jour des objets sauvés
	 */
	public RapportEntreTiers addTiersToCouple(MenageCommun menage, PersonnePhysique tiers, RegDate dateDebut, RegDate dateFin);

	/**
	 * Clôt l'appartenance menageCommun entre les 2 tiers à la date donnée.
	 *
	 * @param pp
	 *            la pp
	 * @param menage
	 *            le menage
	 * @param dateFermeture
	 *            la date de fermeture du rapport
	 */
	public void closeAppartenanceMenage(PersonnePhysique pp, MenageCommun menage, RegDate dateFermeture);

	/**
	 * Clôt tous les rapports du tiers.
	 *
	 * @param pp
	 *            la pp
	 * @param dateFermeture
	 *            la date de fermeture du rapport
	 */
	public void closeAllRapports(PersonnePhysique pp, RegDate dateFermeture);

	/**
	 * Ajoute un rapport prestation imposable
	 *
	 * @param sourcier
	 *            le sourcier sur lequel le debiteur doit être ajouté
	 * @param debiteur
	 *            le debiteur à ajouter au sourcier
	 * @param dateDebut
	 *            la date de début de validité de la relation entre tiers
	 * @param dateFin
	 *            la date de fin de validité de la relation entre tiers (peut être nulle)
	 * @param typeActivite
	 *            le type d'activite
	 * @param tauxActivite
	 *            le taux d'activite
	 * @return le rapport-prestation-imposable avec les références mises-à-jour des objets sauvés
	 */
	public RapportPrestationImposable addRapportPrestationImposable(PersonnePhysique sourcier, DebiteurPrestationImposable debiteur,
			RegDate dateDebut, RegDate dateFin, TypeActivite typeActivite, Integer tauxActivite);


	/**
	 * Ajout d'un rapport de type contact impôt source entre le débiteur et le contribuable
	 *
	 * @param debiteur     un débiteur
	 * @param contribuable un contribuable
	 * @return le rapport
	 */
	public RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable);


	/**
	 * Ajout d'un rapport de type contact impôt source entre le débiteur et le contribuable avec une date de début
	 *
	 * @param debiteur     un débiteur
	 * @param contribuable un contribuable
	 * @param dateDebut    la date de début du rapport
	 * @return le rapport
	 */
	public RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable, RegDate dateDebut);


	/**
	 * Crée et sauvegarde en base un ménage-commun avec ces deux parties.
	 *
	 * @param tiers1
	 *            un tiers du ménage-commun
	 * @param tiers2
	 *            l'autre tiers du ménage-commun (peut être nul)
	 * @param dateDebut
	 *            la date de début de validité de la relation entre tiers
	 * @param dateFin
	 *            la date de fin de validité de la relation entre tiers (peut être nulle)
	 * @return l'ensemble tiers-couple sauvé en base avec les références mises-à-jour des objets sauvés.
	 */
	public EnsembleTiersCouple createEnsembleTiersCouple(PersonnePhysique tiers1, PersonnePhysique tiers2, RegDate dateDebut,
			RegDate dateFin);

	/**
	 * Etabli et sauve en base un rapport entre deux tiers.
	 *
	 * @param rapport
	 *            le rapport à sauver
	 * @param sujet
	 *            le tiers sujet considéré
	 * @param objet
	 *            le tiers objet considéré
	 * @return le rapport sauvé en base
	 */
	public RapportEntreTiers addRapport(RapportEntreTiers rapport, Tiers sujet, Tiers objet);

	/**
	 * @param pp le personne dont on veut connaître le sexe.
	 * @return le sexe de la personne spécifiée, ou <b>null</b> si cette information n'est pas disponible.
	 */
	public Sexe getSexe(PersonnePhysique pp);

	/**
	 * @param pp    le personne dont on veut connaître le sexe.
	 * @param annee l'année de validité de l'information retournée.
	 * @return le sexe de la personne spécifiée, ou <b>null</b> si cette information n'est pas disponible.
	 */
	public Sexe getSexe(PersonnePhysique pp, int annee);

	/**
	 * Détermine si les deux personnes physiques sont de même sexe.
	 *
	 * @param pp1 une personne physique.
	 * @param pp2 une autre personne physique.
	 * @return true si les personnes sont de même sexe.
	 */
	public boolean isMemeSexe(PersonnePhysique pp1, PersonnePhysique pp2);

	/**
	 * Ouvre un nouveau for fiscal principal sur un contribuable.
	 * <p/>
	 * <b>Note:</b> pour ajouter un for fiscal fermé voir la méthode {@link #addForPrincipal(Contribuable, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor,
	 * ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor, ch.vd.uniregctb.type.MotifRattachement, int, ch.vd.uniregctb.type.TypeAutoriteFiscale, ch.vd.uniregctb.type.ModeImposition)}
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param motifRattachement        le motif de rattachement du nouveau for
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @param modeImposition           le mode d'imposition du for fiscal principal
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @param changeHabitantFlag       <b>vrai</b> s'il faut changer le flag habitant en fonction du type d'autorité fiscale
	 * @return le nouveau for fiscal principal
	 */
	ForFiscalPrincipal openForFiscalPrincipal(Contribuable contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
	                                          TypeAutoriteFiscale typeAutoriteFiscale, ModeImposition modeImposition, MotifFor motifOuverture, boolean changeHabitantFlag);

	/**
	 * Ouvre un nouveau for fiscal secondaire sur un contribuable.
	 * <p/>
	 * <b>Note:</b> pour ajouter un for fiscal fermé voir la méthode {@link #addForSecondaire(Contribuable, ch.vd.registre.base.date.RegDate, ch.vd.registre.base.date.RegDate,
	 * ch.vd.uniregctb.type.MotifRattachement, int, ch.vd.uniregctb.type.TypeAutoriteFiscale, ch.vd.uniregctb.type.MotifFor, ch.vd.uniregctb.type.MotifFor)}
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param motifRattachement        le motif de rattachement du nouveau for
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param typeAutoriteFiscale      le type d'autorité fiscale
	 * @param motifOuverture           le motif d'ouverture
	 * @return le nouveau for fiscal secondaire
	 */
	ForFiscalSecondaire openForFiscalSecondaire(Contribuable contribuable, final RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
	                                            TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture);

	/**
	 * Ouvre un nouveau for fiscal autre élément imposable sur un contribuable.
	 *
	 * @param contribuable
	 *            le contribuable sur lequel le nouveau for est ouvert
	 * @param genreImpot
	 *            le genre d'impot
	 * @param dateOuverture
	 *            la date à laquelle le nouveau for est ouvert
	 * @param motifRattachement
	 *            le motif de rattachement du nouveau for
	 * @param numeroOfsAutoriteFiscale
	 *            le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param typeAutoriteFiscale
	 *            le type d'autorité fiscale
	 * @param motifOuverture
	 *            le motif d'ouverture
	 * @return le nouveau for fiscal autre élément imposable
	 */
	ForFiscalAutreElementImposable openForFiscalAutreElementImposable(Contribuable contribuable, GenreImpot genreImpot,
			final RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
			TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture);

	/**
	 * Ouvre un nouveau for fiscal autre impot sur un contribuable.
	 * <p/>
	 * <b>Note:</b> un for autre impôt possède une validité de 1 jour, il n'y a donc pas de méthode pour créer un for fiscal autre impôt fermé.
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param genreImpot               le genre d'impot
	 * @param dateImpot                la date à laquelle le nouveau for est valide
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @return le nouveau for autre impot
	 */
	ForFiscalAutreImpot openForFiscalAutreImpot(Contribuable contribuable, GenreImpot genreImpot, RegDate dateImpot, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale);

	/**
	 * Ouvre un nouveau for fiscal debiteur sur un contribuable.
	 * <p/>
	 * <b>Note:</b> pour ajouter un for fiscal fermé voir la méthode {@link #addForDebiteur(DebiteurPrestationImposable, ch.vd.registre.base.date.RegDate, ch.vd.registre.base.date.RegDate, int)}
	 *
	 * @param debiteur                 le debiteur sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @return le nouveau for debiteur
	 */
	ForDebiteurPrestationImposable openForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, int numeroOfsAutoriteFiscale,
	                                                                  TypeAutoriteFiscale typeAutoriteFiscale);

	/**
	 * Ré-ouvre les rapports de prestation imposables du débiteur qui ont été fermés à la date de désactivation avec une nouvelle date d'ouverture à la réactivation
	 * @param debiteur              le débiteur sur lequel les rapports de prestation imposable doivent être ré-ouverts
	 * @param dateDesactivation     la date à laquelle les rapports avaient été fermés
	 * @param dateReactivation      la date à laquelle les rapports doivent être ré-ouverts
	 */
	void reopenRapportsPrestation(DebiteurPrestationImposable debiteur, RegDate dateDesactivation, RegDate dateReactivation);

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
	ForFiscalPrincipal closeForFiscalPrincipal(ForFiscalPrincipal forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture);

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
	 * @param contribuable
	 *            le contribuable concerné
	 * @param forFiscalAutreElementImposable
	 *            le for à fermer
	 * @param dateFermeture
	 *            la date de fermeture du for
	 * @param motifFermeture
	 *            la motif de fermeture du for
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
	 * @param fermerRapportsPrestation       <code>true</code> s'il faut fermer les rapports "prestation" du débiteur, <code>false</code> s'il faut les laisser ouverts
	 * @return le for debiteur fermé, ou <b>null</b> si le contribuable n'en possédait pas.
	 */
	ForDebiteurPrestationImposable closeForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur,
	                                                                   ForDebiteurPrestationImposable forDebiteurPrestationImposable,
	                                                                   RegDate dateFermeture, boolean fermerRapportsPrestation);

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
	ForFiscalPrincipal changeModeImposition(Contribuable contribuable, RegDate dateChangementModeImposition,
	                                        ModeImposition modeImposition, MotifFor motifFor);

	/**
	 * Corrige l'autorité fiscale du for fiscal spécifié. Le for fiscal est annulé et un nouveau for fiscal avec l'autorité fiscale corrigée est ajouté au tiers [UNIREG-2322].
	 * <p/>
	 * <b>Note:</b> Le type de l'autorité fiscale ne peut pas changer.
	 *
	 * @param forFiscal            le for fiscal à corriger
	 * @param noOfsAutoriteFiscale le mouveau numéro Ofs de l'autorité fiscale
	 * @return le nouveau for fiscal corrigé
	 */
	ForFiscal corrigerAutoriteFiscale(ForFiscal forFiscal, int noOfsAutoriteFiscale);

	/**
	 * Corrige la période de validité (date de début et date de fin) d'un for fiscal secondaire. Le for fiscal est annulé et un nouveau for fiscal avec la période de validité corrigée est
	 * ajouté au tiers [UNIREG-2322].
	 *
	 * @param ffs            le for fiscal secondaire à corriger
	 * @param dateOuverture  la nouvelle date d'ouverture
	 * @param motifOuverture le nouveau motif d'ouverture
	 * @param dateFermeture  la nouvelle date de fermeture
	 * @param motifFermeture le nouveau motif de fermeture
	 * @return le nouveau for fiscal corrigé
	 */
	ForFiscalSecondaire corrigerPeriodeValidite(ForFiscalSecondaire ffs, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture);

	/**
	 * Ajoute un for fiscal principal sur un contribuable. Le for fiscal principal courant est fermé si nécessaire.
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
	ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, MotifRattachement motifRattachement,
	                                   int autoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, ModeImposition modeImposition);

	/**
	 * Annule tous les fors ouverts à la date spécifiée (et qui ne sont pas fermés) sur le contribuable donné et dont le motif d'ouverture correspond à ce qui est indiqué
	 * @param contribuable contribuable visé
	 * @param dateOuverture date d'ouverture des fors à annuler
	 * @param motifOuverture motif d'ouverture des fors à annuler (<code>null</code> possible si tout motif convient)
	 */
	void annuleForsOuvertsAu(Contribuable contribuable, RegDate dateOuverture, MotifFor motifOuverture);

	/**Permet de rajouter une nouvelle périodicité sur un débiteur
	 * verifie si il en existe une pour la même période, et l'annule avant d'ajouter la nouvelle
	 *
	 * @param debiteur
	 * @param periodiciteDecompte
	 * @param periodeDecompte
	 * @param dateDebut
	 * @param dateFin
	 * @return la periodicité ajoutés
	 */
	Periodicite addPeriodicite(DebiteurPrestationImposable debiteur, PeriodiciteDecompte periodiciteDecompte, PeriodeDecompte periodeDecompte, RegDate dateDebut,RegDate dateFin);

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
	 * @return le nouveau for fiscal secondaire.
	 */
	ForFiscalSecondaire addForSecondaire(Contribuable contribuable, RegDate dateOuverture, RegDate dateFermeture, MotifRattachement motifRattachement,
	                                     int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture, MotifFor motifFermeture);

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
	 * @param typeAutoriteFiscale le type de l'autorité fiscale du for à créer
	 * @return le nouveau for fiscal.
	 */
	ForFiscalAutreElementImposable addForAutreElementImposable(Contribuable contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture,
	                                                           MotifRattachement motifRattachement, TypeAutoriteFiscale typeAutoriteFiscale, int autoriteFiscale);

	/**
	 * Ajoute un for fiscal débiteur sur un contribuable.
	 *
	 * @param debiteur        un débiteur de prestations imposables
	 * @param dateDebut       la date d'ouverture du for à créer
	 * @param dateFin         la date de fermeture du for à créer (peut être nulle)
	 * @param autoriteFiscale le numéro de l'autorité fiscale du for à créer (implicitement une commune vaudoise)
	 * @return le nouveau for fiscal.
	 */
	ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable debiteur, RegDate dateDebut, RegDate dateFin, int autoriteFiscale);

	/**
	 * Fusionne un non habitant avec un habitant
	 *
	 * @param habitant    un habitant
	 * @param nonHabitant un non-habitant
	 */
	public void fusionne(PersonnePhysique habitant, PersonnePhysique nonHabitant);

	/**
	 * Retourne les nom et prénoms pour l'adressage de l'individu spécifié.
	 *
	 * @param individu un individu
	 * @return le prénom + le nom du l'individu
	 */
	public String getNomPrenom(Individu individu);

	/**
	 * Retourne les nom et prénoms de la personne physique spécifiée
	 * @param pp personne physique dont on veut le nom
	 * @return une pair composée du (ou des) prénom(s) (premier élément) et du nom (deuxième élément) de la personne physique ( ou {@link NomPrenom.VIDE} si la donnée est inconnue)
	 */
	public NomPrenom getDecompositionNomPrenom(PersonnePhysique pp);

	/**
	 * Retourne les nom et prénoms pour l'adressage de la personne physique spécifiée.
	 *
	 * @param personne une personne physique
	 * @return le prénom + le nom de la personne
	 */
	public String getNomPrenom(PersonnePhysique personne);

	/**
	 * @param pp une personne physique
	 * @return la date de naissance de la personne spécifiée.
	 */
	public RegDate getDateNaissance(PersonnePhysique pp);

	/**
	 * @param pp personne physique dont on veut connaître si oui ou non elle est mineure
	 * @param date la date à laquelle doit être fait le test
	 * @return <code>true</code> si la personne physique est effectivement mineure à la date donnée, ou <code>false</code> si elle ne l'est pas ou que sa date de naissance est inconnue
	 */
	public boolean isMineur(PersonnePhysique pp, RegDate date);


	/**Retourne la date de debut de veuvage d'une personne,
	 *
	 *
	 * @param pp
	 * @param date Date à laquelle le veuvage est valide
	 * @return  la date de veuvage null si la personne n'est pas veuve
	 */
	public RegDate getDateDebutVeuvage(PersonnePhysique pp, RegDate date);

	/**
	 * @param pp une personne physique
	 * @return la date de décès ou <code>null</code> si la personne n'est pas décédée.
	 */
	public RegDate getDateDeces(PersonnePhysique pp);

	/**
	 * @param pp une personne physique
	 * @return true si la personne est décédé, false si la personne n'est pas décédée.
	 */
	public boolean isDecede(PersonnePhysique pp);

	/**
	 * @param pp une personne physique
	 * @return le numéro d'assuré social (numéro AVS EAN13) de la personne physique spécifiée, ou <b>null</b> si cette information n'est pas
	 *         disponible.
	 */
	public String getNumeroAssureSocial(PersonnePhysique pp);

	/**
	 * @param pp une personne physique
	 * @return l'ancien numéro d'assuré social (numéro AVS 11 positions) de la personne physique spécifiée, ou <b>null</b> si cette
	 *         information n'est pas disponible.
	 */
	public String getAncienNumeroAssureSocial(PersonnePhysique pp);

	/**
	 * récupère l'office d'impôt du contribuable
	 *
	 * @param tiers un contribuable
	 * @return l'office d'impot dont dépend le contribuable ou null s'il n'en possède pas
	 */
	public Integer getOfficeImpotId(Tiers tiers);

	/**
	 * Calcule l'id de l'office d'impôt responsable d'un tiers à une date donnée.
	 *
	 * @param tiers le tiers dont on veut connaître l'office d'impôt
	 * @param date  la date de validité de l'office d'impôt; ou <i>null</i> pour obtenir l'état courant.
	 * @return un id de l'office d'impôt; ou <i>null</null> si le tiers n'est pas assujetti ou que son office d'impôt ne peut pas être calculé pour une autre raison.
	 */
	public Integer getOfficeImpotIdAt(Tiers tiers, RegDate date);

	/**
	 * Calcule l'id de l'office d'impôt responsable d'un for de gestion donné
	 * @param forGestion le for de gestion dont on veut connaître l'office d'impôt
	 * @return un id d'office d'impôt
	 */
	public Integer getOfficeImpotId(ForGestion forGestion);

	/**
	 * Calcule et retourne l'office d'impôt responsable d'un tiers à une date donnée.
	 *
	 * @param tiers le tiers dont on veut connaître l'office d'impôt
	 * @param date  la date de validité de l'office d'impôt; ou <i>null</i> pour obtenir l'état courant.
	 * @return un office d'impôt; ou <i>null</null> si le tiers n'est pas assujetti ou que son office d'impôt ne peut pas être calculé pour une autre raison.
	 */
	public CollectiviteAdministrative getOfficeImpotAt(Tiers tiers, RegDate date);

	/**
	 * Calcule l'office d'impôt du tiers spécifié et retourne son id.
	 *
	 * @param tiers un tiers.
	 * @return l'ID de l'office d'impôt (OID); ou <b>null</b> si le tiers n'a pas de for de gestion donc pas d'OID.
	 */
	public Integer calculateCurrentOfficeID(Tiers tiers);

	/**
	 * Réouvre le for et l'assigne au tiers.
	 *
	 * @param ff    un for fiscal
	 * @param tiers un tiers
	 */
	public void reopenFor(ForFiscal ff, Tiers tiers);

	/**
	 * Réouvre, pour un tiers, tous ses fors fermés à une date donnée et avec le
	 * motif de fermeture spécifié si applicable.
	 *
	 * @param date
	 *            la date de fermeture
	 * @param motifFermeture
	 *            le motif de fermeture
	 * @param tiers
	 *            le tiers pour qui les fors seront réouverts
	 */
	public void reopenForsClosedAt(RegDate date, MotifFor motifFermeture, Tiers tiers);

	/**
	 * Annule le for fiscal passé en paramètre.
	 * <p>
	 * Si le for spécifié est un for principal et qu'il existe un for principal précédent adjacent, ce dernier est réouvert.
	 *
	 * @param forFiscal
	 *            le for fiscal à annuler.
	 * @param changeHabitantFlag
	 * 			  pour indiquer si le flag habitant doit être mis à jour lors de l'opération.
	 * @throws ValidationException
	 *             si l'annulation du for principal n'est pas possible
	 */
	public void annuleForFiscal(ForFiscal forFiscal, boolean changeHabitantFlag) throws ValidationException;

	/**
	 * Annule un tiers, et effectue toutes les tâches de cleanup et de maintient de la cohérence des données.
	 *
	 * @param tiers
	 *            le tiers à annuler.
	 */
	public void annuleTiers(Tiers tiers);

	/**
	 * Retourne le for fiscal élu <i>for de gestion</i> à la date donnée.
	 * <p>
	 * <b>Attention !</b> Cette méthode retourne <b>null</b> si aucun for fiscal n'est valide à la date spécifiée. Par obtenir le dernier
	 * for de gestion connu, veuillez utiliser la méthode {@link #getDernierForGestionConnu(RegDate)}.
	 * <p>
	 * <b>Attention !</b> Pour des raisons de performances, les dates de début et de fin de validité retournées ne sont pas garanties
	 * correctes. Elles correspondent aux dates de début et de fin du for fiscal sous-jacent, ce qui peut être faux dans certaines
	 * conditions particulières. Pour obtenir les dates correctes, veuillez utiliser la méthode {@link #getForsGestionHisto()}.
	 *
	 * @param tiers
	 *            le tiers dont on veut connaître le for de gestion
	 * @param date
	 *            la date d'activité du for, ou <b>null</b> pour obtenir le for couremment actif
	 * @return le for de gestion, ou <b>null</b> si aucun for actif n'est trouvé.
	 *
	 * @see #getDernierForGestionConnu(RegDate)
	 */
	public ForGestion getForGestionActif(Tiers tiers, RegDate date);

	/**
	 * Calcul et retourne l'historique des fors de gestion. Les fors retournés se touchent tous même si le contribuable n'est plus assujetti
	 * en continu (même comportement que getDernierForGestionConnu).
	 *
	 * @param tiers
	 *            le tiers dont on veut connaître le for de gestion
	 * @return l'historique des fors de gestion
	 */
	public List<ForGestion> getForsGestionHisto(Tiers tiers);

	/**
	 * Retourne le dernier for de gestion <b>connu</b> à la date donnée, même si le contribuable n'est plus assujetti à ce moment-là.
	 * <p/>
	 * <b>Attention !</b> Pour des raisons de performances, les dates de début et de fin de validité retournées ne sont pas garanties correctes. Elles correspondent aux dates de début et de fin du for
	 * fiscal sous-jacent, ce qui peut être faux dans certaines conditions particulières. Pour obtenir les dates correctes, veuillez utiliser la méthode {@link #getForsGestionHisto()}.
	 *
	 * @param tiers le tiers dont on veut connaître le for de gestion
	 * @param date  la date de validité de la requête
	 * @return le dernier for de festion connu, ou <b>null</b> si le tiers n'a jamais été assujetti (= aucun for).
	 */
	public ForGestion getDernierForGestionConnu(Tiers tiers, RegDate date);

	/**
	 * Ferme les adresses flagées temporaires dans le fiscale
	 *
	 * @param tiers le tiers concerné
	 * @param date date de fermeture
	 * @return liste des adresses fermées
	 */
	public List<AdresseTiers> fermeAdresseTiersTemporaire (Tiers tiers, RegDate date);

	/**
	 * Retourne une chaîne de caractères comme "Non assujetti", "Imposition ordinaire VD/HS/HC", "Dépense"... qui décrit le type d'assujettissement du tiers donné à la date donnée
	 *
	 * @param tiers un tiers
	 * @param date  une date de validité
	 * @return une dénomination de l'assujettissement du tiers
	 */
	public String getRoleAssujettissement(Tiers tiers, RegDate date);

	/**
	 * Ajoute un nouveau for fiscal à un tiers.
	 *
	 * @param tiers le tiers sur lequel on veut ajouter un for fiscal
	 * @param forFiscal le nouveau for fiscal
	 * @return une nouvelle instance du for fiscal avec son id renseigné.
	 */
	<T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal);

	/**
	 * Ajoute une nouvelle periodicite à un debiteur.
	 *
	 * @param debiteur
	 *           le debiteur sur lequel on va ajouter la periodicite
	 * @param periodicite
	 *            la nouvelle periodicite
	 * @return une nouvelle instance de la periodicite avec son id renseigné.
	 */
	Periodicite addAndSave(DebiteurPrestationImposable debiteur, Periodicite periodicite);

	/**
	 * Ajoute une nouvelle situation de famille à un contribuable.
	 *
	 * @param contribuable
	 *            le contribuable sur lequel on veut ajouter un for fiscal
	 * @param situation
	 *            la nouvelle situation de famille
	 * @return une nouvelle instance de la situation de famille avec son id renseigné.
	 */
	SituationFamille addAndSave(Contribuable contribuable, SituationFamille situation);

	/**
	 * Ajoute une nouvelle adresse à un tiers.
	 *
	 * @param tiers
	 *            le tiers sur lequel on veut ajouter un for fiscal
	 * @param adresse
	 *            la nouvelle adresse
	 * @return une nouvelle instance de l'adresse avec son id renseigné.
	 */
	AdresseTiers addAndSave(Tiers tiers, AdresseTiers adresse);

	/**
	 * Ajoute une nouvelle identifiant de personne à une personne physique
	 * @param pp une personne physique
	 * @param ident l'identifiant à ajouter
	 * @return une nouvelle instande de l'identificant avec son id renseigné.
	 */
	IdentificationPersonne addAndSave(PersonnePhysique pp, IdentificationPersonne ident);

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
	 * Ouvre et ferme un nouveau for fiscal principal sur un contribuable .
	 *
	 * @param contribuable
	 *            le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture
	 *            la date à laquelle le nouveau for est ouvert
	 * @param motifRattachement
	 *            le motif de rattachement du nouveau for
	 * @param numeroOfsAutoriteFiscale
	 *            le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param typeAutoriteFiscale
	 *            le type d'autorité fiscale.
	 * @param modeImposition
	 *            le mode d'imposition du for fiscal principal
	 * @param motifOuverture
	 *            le motif d'ouverture
	 * @param dateFermeture
	 *            la date de fermeture du for	 *
	 * @param motifFermeture
	 *            le motif de fermeture
	 *
	 * @return le nouveau for fiscal principal
	 */
	public ForFiscalPrincipal openAndCloseForFiscalPrincipal(Contribuable contribuable, final RegDate dateOuverture,
			MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
			ModeImposition modeImposition, MotifFor motifOuverture,RegDate dateFermeture, MotifFor motifFermeture,
			boolean changeHabitantFlag);

	/**
	 * Lance la correction des flags "habitant" sur les personnes physiques en fonction de
	 * leur for fiscal principal actif
	 * @param nbThreads
	 * @param statusManager
	 */
	public CorrectionFlagHabitantSurPersonnesPhysiquesResults corrigeFlagHabitantSurPersonnesPhysiques(int nbThreads, StatusManager statusManager);

	/**
	 * Lance la correction des flags "habitant" sur les personnes physiques en ménage commun en
	 * fonction du for fiscal principal actif du ménage
	 * @param nbThreads
	 * @param statusManager
	 */
	public CorrectionFlagHabitantSurMenagesResults corrigeFlagHabitantSurMenagesCommuns(int nbThreads, StatusManager statusManager);

	/**
	 * Renvoie <code>true</code> si la personne physique est un sourcier gris à la date donnée
	 * @param pp personne physique
	 * @param date date de référence
	 * @return <code>true</code> si la personne physique est un sourcier gris
	 */
	boolean isSourcierGris(Contribuable pp, RegDate date);

	/**
	 * Retourne l'ensemble des débiteurs de prestations imposables pour lesquels le contribuable donné est tiers référent
	 * @param contribuable tiers référent
	 * @return ensemble des débiteurs de prestations imposables
	 */
	Set<DebiteurPrestationImposable> getDebiteursPrestationImposable(Contribuable contribuable);


	/**
	 * Retourne la date de début de validité pour une nouvelle périodicité calculé en fonction
	 * de la dernière LR émise
	 * @param debiteur
	 * @return la date de debut de validité calculée
	 */
	RegDate getDateDebutNouvellePeriodicite(DebiteurPrestationImposable debiteur);

	/**
	 * @param menage un ménage commun
	 * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun (0, 1 ou 2 personnes max, par définition) en ignorant les rapports annulés
	 */
	Set<PersonnePhysique> getPersonnesPhysiques(MenageCommun menage);

	/**
	 * @param menage un ménage commun
	 * @return l'ensemble des personnes physiques ayant fait ou faisant partie du ménage commun en prenant en compte les rapports éventuellement annulés (il peut donc y avoir plus de deux personnes
	 *         physiques concernées en cas de correction de données) ; le dernier rapport entre tiers est également indiqué
	 */
	Map<PersonnePhysique, RapportEntreTiers> getToutesPersonnesPhysiquesImpliquees(MenageCommun menage);

	/**
	 * @return le contribuable associé au débiteur; ou <b>null</b> si le débiteur n'en possède pas.
	 */
	Contribuable getContribuable(DebiteurPrestationImposable debiteur);

	/**
	 * Renvoie la désignation sociale du débiteur (potentiellement sur plusieurs lignes)
	 * @param debiteur débiteur de prestation imposable dont on veut connaître la raison sociale
	 * @return liste de lignes composant la raison sociale
	 */
	List<String> getRaisonSociale(DebiteurPrestationImposable debiteur);

	/**
	 * Renvoie une liste des composants du ménage valides à une date donnée.
	 *
	 * @param menageCommun
	 * @param date
	 *            la date de référence, ou null pour obtenir tous les composants connus dans l'histoire du ménage.
	 * @return un ensemble contenant 1 ou 2 personnes physiques correspondants au composants du ménage, ou <b>null</b> si aucune personne
	 *         n'est trouvée
	 */
	Set<PersonnePhysique> getComposantsMenage(MenageCommun menageCommun, RegDate date);

	/**
	 * Renvoie une liste des composants du ménage valides sur une période fiscale (1 janvier au 31 décembre) donnée.
	 *
	 * @param menageCommun
	 *            le ménage en question
	 * @param anneePeriode
	 *            la période fiscale de référence
	 * @return un ensemble contenant 1 ou 2 personnes physiques correspondants au composants du ménage, ou <b>null</b> si aucune personne
	 *         n'est trouvée
	 */
	Set<PersonnePhysique> getComposantsMenage(MenageCommun menageCommun, int anneePeriode);

	/**
	 * Recherche le menage commun actif auquel est rattaché une personne
	 *
	 * @param personne
	 *            la personne potentiellement rattachée à un ménage commun
	 * @param periode
	 * @return le ménage commun trouvé, ou null si cette personne n'est pas rattaché au ménage.
	 * @throws TiersException si plus d'un ménage commun est trouvé
	 */
	MenageCommun getMenageCommunActifAt(final Contribuable personne, final DateRangeHelper.Range periode) throws TiersException;

	/**
	 * Renvoie les événements civils non traités concernant le tiers donné
	 * @param tiers la personne physique ou le ménage commun considéré (si ménage commun, tous ses membres seront inspectés)
	 * @return les événements civils encore non-traités (en erreur, ou pas encore traités) sur ce tiers
	 */
	List<EvenementCivilData> getEvenementsCivilsNonTraites(Tiers tiers);

	/**Permet de savoir si un tiers est un veuf(ve) marié seul
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
	public Long extractNumeroIndividuPrincipal(Tiers tiers);

	/**
	 * Analyse le graphe des entités liées et retourne tous les tiers trouvés.
	 *
	 * @param entity une entité liée à d'autres entités.
	 * @return l'ensemble des tiers trouvés; ou un ensemble vide si aucun tiers n'est trouvé.
	 */
	Set<Tiers> getLinkedTiers(LinkedEntity entity);

		/**
	 * permet d'adapter la date de début de validité de la première périodicité en fonction d'une date
	 *
	 */
	void adaptPremierePeriodicite(DebiteurPrestationImposable debiteurPrestationImposable, RegDate dateDebut);

	/**Retourne le nom de la collectivité administrative en paramètre
	 *
	 * @param collectiviteAdministrative
	 * @return le nom de la collectivité adminsitrative
	 */
	public String getNomCollectiviteAdministrative(CollectiviteAdministrative collectiviteAdministrative);

	/**Permet de savoir si un contribuable possède un for hors canton à une date données
	 *
	 * @param contribuable à analyser
	 * @param date référence
	 * @return
	 */
	boolean isHorsCanton(Contribuable contribuable, RegDate date);
}

