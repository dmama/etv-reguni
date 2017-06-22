package ch.vd.uniregctb.evenement.organisation.interne;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.BouclementHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.metier.AjustementForsSecondairesResult;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Classe de base de traitement des événements organisation en provenance du RCEnt.
 *
 * <p>
 * <b>Notes importantes:</b>
 * </p>
 * <ul>
 *    <li>
 *                Cette classe <b>NE</b> doit <b>PAS</b> être étendue directement. Utiliser une des deux classes dérivées
 *                officielles selon le type de traitement:
 *       <ul>
 *          <li>
 *                    {@link EvenementOrganisationInterneDeTraitement} est à étendre pour le traitement d'événements utilisant
 *                    les services Unireg à l'exception de celui servant à l'envoi d'événements fiscaux. (Les services d'Unireg appelés
 *                    dans le cadre du traitement émetteront eux-même, le cas échéant, des événements fiscaux via ce service)
 *          </li>
 *          <li>
 *                    {@link EvenementOrganisationInterneInformationPure} est à étendre pour le traitement d'événements sans appel
 *                    à des services Unireg autres que le service d'émission d'événement fiscaux. Les classes dérivées s'engagent
 *                    à avoir pour seul but l'envoi d'événements fiscaux Unireg.
 *          </li>
 *          <li>
 *                    {@link EvenementOrganisationInterneComposite} sert à porter une suite d'événements internes et ne doit pas
 *                    être étendue.
 *          </li>
 *       </ul>
 *    </li>
 *    <li>
 *                Une opération est une action par laquelle des données Unireg sont modifiées et / ou des événements sont émis, ou tout autre effet de bord.
 *                Le principe ici est que tout traitement est effectué au niveau de cette classe dans une méthode qui prend en charge la gestion de la redondance,
 *                le statut et le suivi.
 *    </li>
 *    <li>
 *                Le status de l'événement est à REDONDANT au départ. Le principe à suivre est le suivant: une opération qui devrait être effectuée
 *                en réaction à l'événment, mais qu'on ommet parce que l'effet de cette opération est déjà présent tel qu'on l'attend (par opposition à une
 *                opération qu'on ne peut effectuer des suites d'une erreur), est redondant, de sorte qu'on n'élève pas le niveau de statut.
 *    </li>
 *    <li>
 *                Lors de tout traitement non redondant, le statut doit être passé à TRAITE au moyen de la méthode raiseStatusTo().
 *    </li>
 *    <li>
 *                Lorsqu'un événement ne demande aucun traitement, son statut doit aussi être passé à TRAITE. (voir SIFISC-23172) C'est assez difficile à réaliser. Il faudrait
 *                disposer de la liste des opérations requises par un événement et monter le statut à TRAITE quand cette liste est vide.
 *    </li>
 *    <li>
 *                Par convention tous les opérations (traitements) qui ont un impact Unireg (sur la BD ou qui emettent des événements)
 *                doivent être exécutées dans une méthode de cette classe et de la manière suivante:
 *    <ol>
 *       <li>
 *                    l'opération qui va être accomplie est annoncée au moyen de suivis.addSuivi() (il faut donc passer en paramètre le collector de suivi),
 *       </li>
 *       <li>
 *                    Vérifie s'il y a redondance et le rapporte dans les logs,
 *       </li>
 *       <li>
 *                    Effectue le traitement si non redondant,
 *       </li>
 *       <li>
 *                    Utilise la méthode raiseStatusTo() pour régler le statut à TRAITE si non redondant.
 *       </li>
 *    </ol>
 *    </li>
 * </ul>
 */
public abstract class EvenementOrganisationInterne {

//	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationInterne.class);

	private EvenementOrganisation evenement;
	private Entreprise entreprise;
	private Organisation organisation;
	private String organisationDescription;

	private HandleStatus status = HandleStatus.REDONDANT;

	public static final String PREFIXE_MUTATION_TRAITEE = "Mutation : ";

	private final EvenementOrganisationContext context;
	private final EvenementOrganisationOptions options;

	protected EvenementOrganisationInterne(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		this.context = context;
		this.options = options;

		/* récupération des informations liés à l'événement */
		this.evenement = evenement;
		this.organisation = organisation;
		this.entreprise = entreprise;

		/* Champs précalculés */
		this.organisationDescription = context.getServiceOrganisation().createOrganisationDescription(organisation, getDateEvt());
	}

	public final void validate(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		validateCommon(erreurs);
		if (!erreurs.hasErreurs()) {
			validateSpecific(erreurs, warnings, suivis);
		}
	}

	/**
	 * Effectue le traitement métier voulu pour l'événement organisation courant.
	 * <p>
	 * Cette méthode lève une exception en cas d'erreur inattendue dans le traitement (la majorité des erreurs prévisibles devraient avoir été traitées
	 * dans la méthode {@link #validate(EvenementOrganisationErreurCollector, EvenementOrganisationWarningCollector, EvenementOrganisationSuiviCollector suivis)}).
	 * </p><p/>
	 * Les éventuels avertissement sont renseignés dans la
	 * collection de warnings passée en paramètre. Cette méthode retourne un status qui permet de savoir si l'événement est redondant ou non.
	 * <p/>
	 * En fonction des différentes valeurs renseignées par cette méthode, le framework de traitement des événements va déterminer l'état de l'événement organisation de la manière suivante :
	 * <ul>
	 *     <li>
	 *                  exception => état de l'événement = EN_ERREUR
	 *     </li><li>
	 *                  status = TRAITE et pas de warnings => état de l'événement = TRAITE
	 *     </li><li>
	 *                  status = REDONDANT et pas de warnings => état de l'événement = REDONDANT
	 *     </li><li>
	 *                  status = TRAITE avec warnings => état de l'événement = A_VERIFIER
	 *     </li><li>
	 *                  status = REDONDANT avec warnings => état de l'événement = A_VERIFIER
	 *     </li>
	 * </ul>
	 * </p>
	 * <p>
	 * A noter que cette méthode est finale. Le traitement à proprement parler est effectué dans la méthode doHandle().
	 * </p>
	 * <p>
	 * Cette méthode vérifie s'il faut bien exécuter le traitement, ou non s'il a un effet dans Unireg et qu'on est en mode "sans impact Unireg".
	 * </p>
	 * <p>
	 * Quelques règles à respecter dans doHandle() pour que tout se passe bien:
	 * <ul>
	 *     <li>
	 *                  Chaque traitement entraînant la création d'objet en base, ou tout autre action métier, doit être effectué au sein d'une
	 *                  méthode dédiée à l'opération métier correspondante, et nommée en fonction d'elle.
	 *     </li><li>
	 *                  Chaque méthode métier doit impérativement se terminer par un appel à raiseStatusTo() pour établir le status résultant de
	 *                  l'opération.
	 *     </li><li>
	 *                  D'une manière générale, les décisions métier doivent ressortir clairement de la lecture de la méthode doHandle().
	 *     </li><li>
	 *                  Les paramètres métier sont passés en paramètres des méthodes métier. En particulier les dates. En revanche, ces méthodes
	 *                  accédent au contexte directement.
	 *     </li><li>
	 *                  Les methodes handle() et les méthodes metiers ajouteront des messages de suivis permettant à l'utilisateur de suivre le
	 *                  déroulement du traitement dans l'interface de gestion des événements RCEnt.
	 *     </li>
	 * </ul>
	 * </p>
	 * @param warnings une liste de warnings qui sera remplie - si nécessaire - par la méthode.
	 * @param suivis   une liste de messages de suivi pour l'utilisateur, qui sera renseigné au fur et à mesure du traitement.
	 * @return un code de status permettant de savoir si lévénement a été traité ou s'il était redondant.
	 * @throws EvenementOrganisationException si le traitement de l'événement est impossible pour une raison ou pour une autre.
	 */
	@NotNull
	public final HandleStatus handle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		if (!(this instanceof EvenementOrganisationInterneComposite || this instanceof Indexation)) {
			if (this.describe() != null) {
				suivis.addSuivi(String.format("%s%s", PREFIXE_MUTATION_TRAITEE, this.describe()));
			}
		}

		this.doHandle(warnings, suivis);
		Assert.notNull(status, "Status inconnu après le traitement de l'événement interne!");
		return status;
	}

	/*
			Méthode à redéfinir pour implémenter le traitement concret. Voir ci-dessus handle().
		 */
	public abstract void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException;

	/**
	 * Méthode renvoyant un événement dont la seule action de l'événement est d'émettre un ou des événements fiscaux. Sinon,
	 * null. (Cf. ci-dessus)
	 *
	 * @return Un événement, ou null.
	 */
	public abstract EvenementOrganisationInterne seulementEvenementsFiscaux() throws EvenementOrganisationException;


	/**
	 * Fourni une courte (env. 30-40 lettres max.) description humaine de la mutation couverte par cet événement interne. Cette méthode est destinée
	 * à être redéfinie par les classes dérivées. Cette description apparaît dans le compte rendu utilisateur du traitement de l'événement RCEnt.
	 * @return La description de la mutation.
	 */
	public String describe() {
		return this.getClass().getSimpleName();
	}

	@NotNull
	protected List<RegimeFiscal> extractRegimesFiscauxVD() {
		List<RegimeFiscal> regimesFiscauxNonAnnulesTries = getEntreprise().getRegimesFiscauxNonAnnulesTries();
		List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();

		for (RegimeFiscal regime : regimesFiscauxNonAnnulesTries) {
			if (regime.getPortee() == RegimeFiscal.Portee.VD) {
				regimesFiscauxVD.add(regime);
			}
		}
		return regimesFiscauxVD;
	}

	@NotNull
	protected List<RegimeFiscal> extractRegimesFiscauxCH() {
		List<RegimeFiscal> regimesFiscauxNonAnnulesTries = getEntreprise().getRegimesFiscauxNonAnnulesTries();
		List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();

		for (RegimeFiscal regime : regimesFiscauxNonAnnulesTries) {
			if (regime.getPortee() == RegimeFiscal.Portee.CH) {
				regimesFiscauxVD.add(regime);
			}
		}
		return regimesFiscauxVD;
	}

	/**
	 * Détermine le motif d'ouverture de for approprié, du point de vue d'un établissement principal vaudois.
	 * @param isCreation Si on considère l'entreprise comme étant nouvellement fondée.
	 * @return le motif d'ouverture de for appropriée
	 */
	@NotNull
	protected MotifFor determineMotifOuvertureFor(boolean isCreation) throws EvenementOrganisationException {
		final MotifFor motifOuverture;
		Domicile siegePrecedant = OrganisationHelper.siegePrincipalPrecedant(getOrganisation(), getDateEvt());
		if (isCreation) {
			motifOuverture = MotifFor.DEBUT_EXPLOITATION;
		} else {
			if (siegePrecedant == null) {
				motifOuverture = MotifFor.ARRIVEE_HC;
			} else {
				switch (siegePrecedant.getTypeAutoriteFiscale()) {
				case COMMUNE_OU_FRACTION_VD:
					/*
					 SIFISC-19332 - Si on est ici, c'est qu'on existe déjà dans RCEnt mais pas depuis plus de 15 jours et qu'on n'est pas en création. Ce qui peut se produire dans les cas suivant:
					  a) On a faussement détecté l'entreprise comme n'étant pas en création, ce qui devrait ne se produire qu'avec les Non-RC
					  b) Une non RC ne devrait pas poser de problème de création sur le premier événement, donc ne devrait pas obliger à sauter le premier événement.
					 Conclusion: le cas ne devrait pas se produire, sauf en cas de problème grave avec les données, et là c'est souhaitable.
					  */

					throw new EvenementOrganisationException(
							"Tentative d'ouvrir un for pour une entreprise vaudoise pas nouvelle mais inconnue d'Unireg jusque là. Ceci indique qu'un ou plusieurs événements précédant ont été manqués (soit non reçus, soit forcés). Il faut traiter ce cas manuellement.");
				case COMMUNE_HC:
					motifOuverture = MotifFor.ARRIVEE_HC;
					break;
				case PAYS_HS:
					motifOuverture = MotifFor.ARRIVEE_HS;
					break;
				default:
					throw new EvenementOrganisationException("L'organisation a un précédent siège avec un type d'autorité fiscal inconnu. Veuillez traiter le cas manuellement.");
				}
			}
		}
		return motifOuverture;
	}

	/**
	 * Trouve le range en cours de validité et vérifie qu'il est ouvert.
	 * <p>
	 * A utiliser pour s'assurer qu'on est en présence d'une situation "propre", c'est-à-dire d'une valeur en cours de validité. N'est valable
	 * que pour une liste représentant l'historique d'une donnée (une seule valeur à la fois). La liste n'a pas besoin d'être dans l'ordre chronologique.
	 * </p>
	 * @param list La liste des valeurs représentant l'historique
	 * @param date La date de référence
	 * @param <T> Le type des valeurs de la liste, implémentant {@link DateRange}
	 * @return Le range valide à la date, null s'il n'y en a pas ou qu'il est fermé.
	 */
	protected <T extends DateRange> T getAndValidateOpen(List<T> list, RegDate date) {
		T range = DateRangeHelper.rangeAt(list, date);
		return validateOpen(range) ? range : null;
	}

	private <T extends DateRange> boolean validateOpen(T range) {
		return range != null && range.getDateFin() == null;
	}

	protected boolean determineAssujettie(Entreprise entreprise, RegDate date) throws EvenementOrganisationException {
		final boolean assujettie;
		try {
			assujettie = isAssujetti(entreprise, date);
		}
		catch (AssujettissementException exception) {
			throw new EvenementOrganisationException(
					String.format("Impossible de déterminer si l'entreprise n°%s est assujettie. Une erreur est survenue: %s",
					              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
					              exception.getMessage())
					, exception
			);
		}
		return assujettie;
	}

	protected abstract void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException;

	protected void validateCommon(EvenementOrganisationErreurCollector erreurs) {

		/*
		 * Vérifie que les éléments de base sont renseignés
		 */
		if (getDateEvt() == null) {
			erreurs.addErreur("L'événement n'est pas daté");
			return;
		}

		/*
		 * La date de l'événement se situe dans le futur.
		 */
		if (getDateEvt().isAfter(RegDate.get())) {
			erreurs.addErreur("La date de l'événement est dans le futur");
		}

		// TODO: Validations supplémentaires pour organisations?

		// en tout cas, l'individu devrait exister dans le registre civil !
		final Organisation organisation = getOrganisation();
		if (organisation == null) {
			erreurs.addErreur("L'organisation est introuvable dans RCEnt!");
		}
	}
	/**
	 * @param etat l'état final de l'événement organisation après (tentative de) traitement
	 * @param commentaireTraitement commentaire de traitement présent dans l'événement organisation à la fin du traitement
	 * @return <code>true</code> si le commentaire de traitement doit être éliminé car il n'y a pas de sens de le garder compte tenu de l'état final de l'événement
	 */
	public boolean shouldResetCommentaireTraitement(EtatEvenementOrganisation etat, String commentaireTraitement) {
		return false;
	}

	protected EvenementOrganisationContext getContext() {
		return context;
	}

	protected EvenementOrganisationOptions getOptions() {
		return options;
	}

	public EvenementOrganisation getEvenement() {
		return evenement;
	}

	/**
	 * @return le numéro d'événement RCEnt
	 */
	public Long getNumeroEvenement() {
		return evenement.getNoEvenement();
	}

	public RegDate getDateEvt() {
		return evenement.getDateEvenement();
	}

	public long getNoOrganisation() {
		return organisation.getNumeroOrganisation();
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	public String getOrganisationDescription() {
		return organisationDescription;
	}

	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void raiseStatusTo(HandleStatus nouveau) {
		if (status != null) {
			status = status.raiseTo(nouveau);
		} else {
			status = HandleStatus.REDONDANT.raiseTo(nouveau);
		}
	}

	/**
	 * @return true si une entreprise est assujettie pour une certaine date.
	 */
	protected boolean isAssujetti(Entreprise entreprise, RegDate date) throws AssujettissementException {
		List<Assujettissement> assujettissements = context.getAssujettissementService().determine(entreprise);
		Assujettissement assujettissement = null;
		if (assujettissements != null && !assujettissements.isEmpty()) {
			assujettissement = DateRangeHelper.rangeAt(assujettissements, date);
		}
		return assujettissement != null;
	}

	protected Etablissement getEtablissementByNumeroSite(long numeroSite) {
		return context.getTiersDAO().getEtablissementByNumeroSite(numeroSite);
	}

	protected void programmeReindexation(Entreprise pm, EvenementOrganisationSuiviCollector suivis) {
		Audit.info(getNumeroEvenement(), String.format("Déclenchement de la réindexation pour l'entreprise n°%s.", FormatNumeroHelper.numeroCTBToDisplay(pm.getNumero())));
		context.getIndexer().schedule(pm.getNumero());
	}

	/**
	 * Crée et persiste une nouvelle entreprise pour un numéro d'organisation donné
	 * @param noOrganisation numéro d'organisation à utiliser
	 * @return la nouvelle entreprise déjà persistée
	 */
	private Entreprise createEntreprise(long noOrganisation) {
		final Entreprise entreprise = new Entreprise();
		entreprise.setNumeroEntreprise(noOrganisation);
		raiseStatusTo(HandleStatus.TRAITE);
		return (Entreprise) context.getTiersDAO().save(entreprise);
	}

	protected void createEntreprise(RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		Assert.notNull(organisation);
		Assert.notNull(dateDebut);

		final Entreprise entreprise = createEntreprise(getNoOrganisation());
		suivis.addSuivi(String.format("Entreprise créée avec le numéro de contribuable %s pour l'organisation n°%d", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), getNoOrganisation()));
		setEntreprise(entreprise);

		if (organisation.isInscriteAuRC(getDateEvt())) {
			changeEtatEntreprise(entreprise, TypeEtatEntreprise.INSCRITE_RC, dateDebut, suivis);
		}
		else {
			changeEtatEntreprise(entreprise, TypeEtatEntreprise.FONDEE, dateDebut, suivis);
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	protected void changeEtatEntreprise(Entreprise entreprise, TypeEtatEntreprise etat, RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		context.getTiersService().changeEtatEntreprise(etat, entreprise, dateDebut, TypeGenerationEtatEntreprise.AUTOMATIQUE);
		suivis.addSuivi(String.format("Réglage de l'état: %s.", etat.getLibelle()));
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * Règle les régimes fiscaux VD et CH par défaut en fonction de la forme juridique.
	 */
	protected void openRegimesFiscauxParDefautCHVD(Entreprise entreprise, Organisation organisation, RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		final FormeJuridiqueEntreprise formeJuridique = FormeJuridiqueEntreprise.fromCode(organisation.getFormeLegale(getDateEvt()).getCode());
		final TypeRegimeFiscal typeRegimeFiscal = context.getRegimeFiscalService().getTypeRegimeFiscalParDefaut(formeJuridique);
		context.getTiersService().openRegimeFiscal(entreprise, RegimeFiscal.Portee.CH, typeRegimeFiscal, dateDebut);
		context.getTiersService().openRegimeFiscal(entreprise, RegimeFiscal.Portee.VD, typeRegimeFiscal, dateDebut);
		suivis.addSuivi(String.format("Régimes fiscaux par défaut [%s] VD et CH ouverts pour l'entreprise n°%s (civil: %d)",
		                              typeRegimeFiscal.getLibelleAvecCode(), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), getNoOrganisation()));
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * Règle les régimes fiscaux VD et CH de type indéterminé et programme une tâche de détermination du régime fiscal
	 */
	protected void openRegimesFiscauxIndetermineCHVD(Entreprise entreprise, Organisation organisation, RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		final TypeRegimeFiscal typeRegimeFiscal = context.getRegimeFiscalService().getTypeRegimeFiscalIndetermine();
		context.getTiersService().openRegimeFiscal(entreprise, RegimeFiscal.Portee.CH, typeRegimeFiscal, dateDebut);
		context.getTiersService().openRegimeFiscal(entreprise, RegimeFiscal.Portee.VD, typeRegimeFiscal, dateDebut);
		suivis.addSuivi(String.format("Régimes fiscaux de type indéterminé [%s] VD et CH ouverts pour l'entreprise n°%s (civil: %d)",
		                              typeRegimeFiscal.getLibelleAvecCode(), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), getNoOrganisation()));
		raiseStatusTo(HandleStatus.TRAITE);
	}

	protected void closeRegimesFiscauxOrdinairesCHVD(RegimeFiscal regimeFiscalCH, RegimeFiscal regimeFiscalVD, RegDate dateFin, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// Le régime fiscal VD + CH
		context.getTiersService().closeRegimeFiscal(regimeFiscalCH, dateFin);
		context.getTiersService().closeRegimeFiscal(regimeFiscalVD, dateFin);

		suivis.addSuivi(String.format("Régimes fiscaux VD et CH fermés pour l'entreprise n°%s (civil: %d)", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), getNoOrganisation()));
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/*
	 * Protection contre l'écrasement intempestif de l'entreprise existante, qui si elle existe déjà,
	 * est à priori une entité rattachée à un contexte de persistence. Entité que l'on doit modifier directement si l'on
	 * effectue des modifications.
	 */
	private void setEntreprise(Entreprise entreprise) {
		if (this.entreprise != null) {
			throw new IllegalStateException(
					String.format("Refus d'écraser l'instance d'entreprise existante [no: %s, no organisation: %d]. Arrêt du traitement de l'événement.",
					              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
					              entreprise.getNumeroEntreprise())
			);
		}
		this.entreprise = entreprise;
	}


	/**
	 * Méthode de convenance à utiliser de préférence pour la création de nouveaux établissements.
	 * Cette méthode effectue certains contrôles supplémentaires.
	 *
	 * @param site
	 * @param warnings
	 * @param suivis
	 * @return l'établissement créé
	 * @throws EvenementOrganisationException
	 */
	protected Etablissement addEtablissementSecondaire(SiteOrganisation site, RegDate dateDebut, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		long numeroSite = site.getNumeroSite();
		Etablissement etablissement = getEtablissementByNumeroSite(numeroSite);
		if (etablissement != null) {
			throw new EvenementOrganisationException(
					String.format("%s existe déjà pour l'organisation en création n°%d(%s). Impossible de continuer.",
					              etablissement, getNoOrganisation(), getOrganisation().getNom(dateDebut)));
		}

		final Domicile autoriteFiscale = site.getDomicile(getDateEvt());
		if (autoriteFiscale == null) {
			throw new EvenementOrganisationException(
					String.format(
							"Autorité fiscale (siège) introuvable pour le site secondaire %d de l'organisation n°%d %s. Impossible de créer le domicile de l'établissement secondaire.",
							site.getNumeroSite(), getNoOrganisation(), getOrganisation().getNom(getDateEvt())));
		}

		return createAddEtablissement(site.getNumeroSite(), autoriteFiscale, false, dateDebut, suivis);
	}

	/**
	 * Créer un établissement, avec toutes ses caractéristiques usuelles, et le rattacher à l'entreprise en cours au
	 * moyen d'un rapport entre tiers d'activité économique.
	 * @param numeroSite Le numéro du site sur lequel porte l'établissement
	 * @param autoriteFiscale La commune politique de domicile de l'établissement
	 * @param principal Si l'établissement est principal ou secondaire
	 * @param suivis       Le collector pour le suivi
	 * @param dateDebut Date de début
	 * @return l'établissement créé
	 */
	protected Etablissement createAddEtablissement(Long numeroSite, Domicile autoriteFiscale, boolean principal, RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) throws
			EvenementOrganisationException {
		Assert.notNull(numeroSite);
		Assert.notNull(autoriteFiscale);
		Assert.notNull(dateDebut);

		// L'établissement
		Etablissement etablissement = context.getTiersService().createEtablissement(numeroSite);
		// L'activité économique
		context.getTiersService().addRapport(new ActiviteEconomique(dateDebut, null, entreprise, etablissement, principal), getEntreprise(), etablissement);

		final Commune commune = getCommune(autoriteFiscale.getNumeroOfsAutoriteFiscale(), dateDebut);

		suivis.addSuivi(String.format("Etablissement %s créé avec le numéro %s pour le site %d, domicile %s (ofs: %d), à partir du %s",
		                              principal ? "principal" : "secondaire",
		                              FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
		                              numeroSite,
		                              commune.getNomOfficielAvecCanton(),
		                              autoriteFiscale.getNumeroOfsAutoriteFiscale(),
		                              RegDateHelper.dateToDisplayString(dateDebut)));
		raiseStatusTo(HandleStatus.TRAITE);
		return etablissement;
	}

	/**
	 * Recherche dans FiDoR la commune par numéro OFS et pour une date.
	 */
	@NotNull
	protected Commune getCommune(int noOfsAutoriteFiscale, RegDate dateDebut) throws EvenementOrganisationException {
		final List<Commune> communeHistoByNumeroOfs = context.getServiceInfra().getCommuneHistoByNumeroOfs(noOfsAutoriteFiscale);
		final Commune commune = DateRangeHelper.rangeAt(communeHistoByNumeroOfs, dateDebut);
		String infosCommuneAuCommencement = "";
		if (commune == null) {
			if (communeHistoByNumeroOfs.size() > 0) {
				Commune communeAuCommencement = communeHistoByNumeroOfs.get(0);
				infosCommuneAuCommencement = String.format(" On en trouve cependant une appelée %s commençant fiscalement en date du %s.",
				                                           communeAuCommencement.getNomOfficielAvecCanton(),
				                                           RegDateHelper.dateToDisplayString(communeAuCommencement.getDateDebut()));
			}
			throw new EvenementOrganisationException(String.format("La commune au numéro ofs %d n'existe pas en date du %s!%s",
			                                                       noOfsAutoriteFiscale,
			                                                       RegDateHelper.dateToDisplayString(dateDebut),
			                                                       infosCommuneAuCommencement));
		}
		return commune;
	}

	/**
	 * Méthode qui se contente de signaler le changement de domicile de l'établissement.
	 *
	 * @param etablissement
	 * @param ancienDomicile
	 * @param nouveauDomicile
	 * @param dateDebut
	 * @param suivis
	 */
	protected void signaleDemenagement(Etablissement etablissement, Domicile ancienDomicile, Domicile nouveauDomicile, RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) throws
			EvenementOrganisationException {
		final String ancienneCommune = getCommune(ancienDomicile.getNumeroOfsAutoriteFiscale(), dateDebut.getOneDayBefore()).getNomOfficielAvecCanton();
		final String nouvelleCommune = getCommune(nouveauDomicile.getNumeroOfsAutoriteFiscale(), dateDebut).getNomOfficielAvecCanton();
		suivis.addSuivi(String.format("L'établissement n°%s a déménagé de %s (ofs: %d) à %s (ofs: %d), le %s.",
		                              FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
		                              ancienneCommune,
		                              ancienDomicile.getNumeroOfsAutoriteFiscale(),
		                              nouvelleCommune,
		                              nouveauDomicile.getNumeroOfsAutoriteFiscale(),
		                              RegDateHelper.dateToDisplayString(dateDebut)));
	}

	/**
	 * Ouvre un nouveau for fiscal principal.
	 *
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param autoriteFiscale          l'autorité fiscale sur laquelle est ouvert le nouveau for.
	 * @param rattachement             le motif de rattachement du nouveau for
	 * @param genreImpot               le genre d'impôt du nouveau for
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal, peut être <code>null</code> si l'établissement principal est hors canton.
	 * @param suivis       Le collector pour le suivi
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipalPM openForFiscalPrincipal(final RegDate dateOuverture, Domicile autoriteFiscale,
	                                                      MotifRattachement rattachement, @Nullable MotifFor motifOuverture, GenreImpot genreImpot, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws
			EvenementOrganisationException {
		if (autoriteFiscale.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && motifOuverture == null) {
			throw new EvenementOrganisationException("Le motif d'ouverture est obligatoire sur un for principal dans le canton");
		}

		final Commune commune = getCommune(autoriteFiscale.getNumeroOfsAutoriteFiscale(), dateOuverture);
		if (!commune.isPrincipale()) {
			suivis.addSuivi(String.format("Ouverture d'un for fiscal principal à %s à partir du %s, motif ouverture %s, rattachement %s, pour l'entreprise n°%s (civil: %d).",
			                              commune.getNomOfficielAvecCanton(),
			                              RegDateHelper.dateToDisplayString(dateOuverture), motifOuverture == null ? "<aucun>" : motifOuverture.getDescription(true), rattachement,
			                              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entreprise.getNumeroEntreprise())
			);
			raiseStatusTo(HandleStatus.TRAITE);
			return context.getTiersService().openForFiscalPrincipal(entreprise, dateOuverture, rattachement, autoriteFiscale.getNumeroOfsAutoriteFiscale(), autoriteFiscale.getTypeAutoriteFiscale(), motifOuverture, genreImpot);
		} else {
			warnings.addWarning(
					String.format("Ouverture de for fiscal principal sur une commune faîtière de fractions, %s: Veuillez saisir le for fiscal principal manuellement.",
					              commune.getNomOfficielAvecCanton()));
			raiseStatusTo(HandleStatus.TRAITE);
		}
		return null;
	}

	/**
	 * Crée un nouveau for fiscal secondaire, optionnellement déjà fermé.
	 *
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param dateFermeture            la date de fermeture (optionnelle)
	 * @param motifRattachement        le motif de rattachement du nouveau for
	 * @param numeroOfsAutoriteFiscale l'autorité fiscale sur laquelle est ouvert le nouveau for.
	 * @param typeAutoriteFiscale      le type d'autorité fiscale applicable
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @param motifFermeture           le motif de fermeture (si date de fermeture présente)
	 * @param warnings                 le collector pour les avertissements
	 * @param suivis                   le collector pour le suivi
	 * @return                         le nouveau for fiscal secondaire
	 */
	protected ForFiscalSecondaire creerForFiscalSecondaire(RegDate dateOuverture, @Nullable RegDate dateFermeture, MotifRattachement motifRattachement,
	                                                       int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
	                                                       MotifFor motifOuverture, @Nullable MotifFor motifFermeture, GenreImpot genreImpot,
	                                                       EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		final Commune commune = getCommune(numeroOfsAutoriteFiscale, dateOuverture);
		if (!commune.isPrincipale()) {
			Assert.notNull(motifOuverture, "Le motif d'ouverture est obligatoire sur un for secondaire dans le canton"); // TODO: is it?
			suivis.addSuivi(String.format("Création d'un for fiscal secondaire à %s à partir du %s (%s)%s, rattachement %s, entreprise n°%s (civil: %d).",
			                              commune.getNomOfficielAvecCanton(),
			                              RegDateHelper.dateToDisplayString(dateOuverture),
			                              motifOuverture.getDescription(true),
			                              dateFermeture != null ? ", au " + RegDateHelper.dateToDisplayString(dateFermeture) +
					                              (motifFermeture != null ? " (" + motifFermeture.getDescription(false) + ")" : "") : "",
			                              motifRattachement,
			                              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entreprise.getNumeroEntreprise())
			);
			raiseStatusTo(HandleStatus.TRAITE);
			return context.getTiersService().addForSecondaire(entreprise, dateOuverture, dateFermeture, motifRattachement, numeroOfsAutoriteFiscale, typeAutoriteFiscale,
			                                           motifOuverture, motifFermeture, genreImpot);
		} else {
			warnings.addWarning(
					String.format("La création doit être faite manuellement pour un for fiscal secondaire sur une commune faîtière de fractions " +
							              "%s à partir du %s (%s)%s, rattachement %s, entreprise n°%s (civil: %d).",
					              commune.getNomOfficielAvecCanton(),
					              RegDateHelper.dateToDisplayString(dateOuverture),
					              motifOuverture.getDescription(true),
					              dateFermeture != null ? ", au " + RegDateHelper.dateToDisplayString(dateFermeture) +
							              (motifFermeture != null ? " (" + motifFermeture.getDescription(false) + ")" : "") : "",
					              motifRattachement,
					              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entreprise.getNumeroEntreprise())
			);
			raiseStatusTo(HandleStatus.TRAITE);
		}
		return null;
	}

	/**
	 * Ferme un for secondaire.
	 *
	 * @param dateDeFermeture la date à laquelle le for est fermé
	 * @param motifFermeture  le motif de fermeture du for fiscal principal
	 * @param suivis       Le collector pour le suivi
	 * @return
	 */
	protected ForFiscalSecondaire closeForFiscalSecondaire(RegDate dateDeFermeture, ForFiscalSecondaire forAFermer, MotifFor motifFermeture, EvenementOrganisationSuiviCollector suivis) throws
			EvenementOrganisationException {

		final Commune commune = getCommune(forAFermer.getNumeroOfsAutoriteFiscale(), dateDeFermeture);
		suivis.addSuivi(String.format("Fermeture d'un for secondaire à %s en date du %s, motif fermeture %s, pour l'entreprise n°%s (civil: %d).",
		                              commune.getNomOfficielAvecCanton(),
		                              RegDateHelper.dateToDisplayString(dateDeFermeture),
		                              motifFermeture.getDescription(false),
		                              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entreprise.getNumeroEntreprise())
		);

		raiseStatusTo(HandleStatus.TRAITE);
		return context.getTiersService().closeForFiscalSecondaire(entreprise, forAFermer, dateDeFermeture, motifFermeture);
	}

	protected void annulerForFiscalSecondaire(final ForFiscalSecondaire forAAnnuler, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws
			EvenementOrganisationException {
		final Commune commune = getCommune(forAAnnuler.getNumeroOfsAutoriteFiscale(), forAAnnuler.getDateDebut());
		suivis.addSuivi(String.format("Annulation d'un for fiscal secondaire à %s débutant le %s%s, motif ouverture %s, rattachement %s, pour l'entreprise n°%s (civil: %d).",
		                              commune.getNomOfficielAvecCanton(),
		                              RegDateHelper.dateToDisplayString(forAAnnuler.getDateDebut()),
		                              forAAnnuler.getDateFin() != null ? " et prenant fin le " + forAAnnuler.getDateFin() : "",
		                              forAAnnuler.getMotifOuverture().getDescription(true),
		                              forAAnnuler.getMotifRattachement(),
		                              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entreprise.getNumeroEntreprise()));
		context.getTiersService().annuleForFiscal(forAAnnuler);
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * Ferme l'actuel for principal.
	 *
	 * @param dateDeFermeture la date à laquelle l'ancien for est fermé
	 * @param motifFermeture  le motif de fermeture du for fiscal principal
	 * @param suivis          le collector pour le suivi
	 * @return
	 */
	protected ForFiscalPrincipal closeForFiscalPrincipal(RegDate dateDeFermeture, MotifFor motifFermeture, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		final ForFiscalPrincipalPM forFiscalPrincipal = entreprise.getForFiscalPrincipalAt(null);
		final Commune commune = getCommune(forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), dateDeFermeture);
		suivis.addSuivi(String.format("Fermeture du for fiscal principal à %s en date du %s, motif fermeture %s, pour l'entreprise n°%s (civil: %d)",
		                              commune.getNomOfficielAvecCanton(),
		                              RegDateHelper.dateToDisplayString(dateDeFermeture), motifFermeture.getDescription(false),
		                              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entreprise.getNumeroEntreprise()));

		raiseStatusTo(HandleStatus.TRAITE);
		return context.getTiersService().closeForFiscalPrincipal(entreprise, dateDeFermeture, motifFermeture);
	}

	protected void reopenForFiscalPrincipal(ForFiscalPrincipal forFiscalPrincipal, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		final Commune commune = getCommune(forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getDateDebut());
		suivis.addSuivi(String.format("Réouverture du for principal de %s, qui commençait le %s%s, pour l'entreprise n°%s (civil: %s).",
		                              commune.getNomOfficielAvecCanton(),
		                              RegDateHelper.dateToDisplayString(forFiscalPrincipal.getDateDebut()),
		                              forFiscalPrincipal.getDateFin() != null ? " et se terminait le " + RegDateHelper.dateToDisplayString(forFiscalPrincipal.getDateFin()) : "",
		                              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entreprise.getNumeroEntreprise())
		);
		context.getTiersService().annuleForFiscal(forFiscalPrincipal);
		context.getTiersService().reopenFor(forFiscalPrincipal, forFiscalPrincipal.getTiers());
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * Ajoute un bouclement en bonne et due forme.
	 * @param dateDebut Date de début du bouclement
	 */
	protected void createAddBouclement(RegDate dateDebut, boolean isCreationPure, EvenementOrganisationSuiviCollector suivis) {
		final Bouclement bouclement;

		if (isCreationPure) {
			bouclement = BouclementHelper.createBouclement3112SelonSemestre(dateDebut);
		} else {
			entreprise.setDateDebutPremierExerciceCommercial(RegDate.get(dateDebut.year(), 1, 1));
			bouclement = BouclementHelper.createBouclement3112(RegDate.get(dateDebut.year(), 12, 1));
		}
		bouclement.setEntreprise(entreprise);
		context.getTiersDAO().addAndSave(entreprise, bouclement);
		RegDate premierBouclement = RegDate.get(bouclement.getDateDebut().year(), bouclement.getAncrage().month(), bouclement.getAncrage().day());
		suivis.addSuivi(String.format("Bouclement créé avec une périodicité de %d mois à partir du %s",
		                              bouclement.getPeriodeMois(), RegDateHelper.dateToDisplayString(premierBouclement)));
		raiseStatusTo(HandleStatus.TRAITE);
	}

	protected void regleDateDebutPremierExerciceCommercial(Entreprise entreprise, RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		final RegDate dateDebutPremierExerciceCommercial = RegDate.get(dateDebut.year(), 1, 1);
		suivis.addSuivi(String.format("Réglage de la date de début du premier exercice commercial au %s", RegDateHelper.dateToDisplayString(dateDebutPremierExerciceCommercial)));
		entreprise.setDateDebutPremierExerciceCommercial(dateDebutPremierExerciceCommercial);
		raiseStatusTo(HandleStatus.TRAITE);
	}

	protected void closeEtablissement(Etablissement etablissement, RegDate dateFin,  EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws
			EvenementOrganisationException {
		suivis.addSuivi(String.format("Fermeture de l'établissement n°%s pour le %s", FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()), RegDateHelper.dateToDisplayString(dateFin)));
		final List<DomicileEtablissement> sortedDomiciles = etablissement.getSortedDomiciles(false);
		final DomicileEtablissement domicile = DateRangeHelper.rangeAt(sortedDomiciles, dateFin);
		if (domicile != null) {
			if (!DateRangeHelper.equals(domicile, CollectionsUtils.getLastElement(sortedDomiciles))) {
				throw new EvenementOrganisationException(String.format("L'établissement n°%s a déménagé depuis la date pour laquelle on cherche à le fermer!",
				                                                       FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
			}

			context.getTiersService().closeDomicileEtablissement(domicile, dateFin);
		}
		final RapportEntreTiers rapportEntreprise = etablissement.getRapportObjetValidAt(dateFin, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
		// SIFISC-19230: Le rapport peut avoir été fermé dans le cadre du processus complexe "Fin d'activité"
		if (rapportEntreprise != null) {
			context.getTiersService().closeRapportEntreTiers(rapportEntreprise, dateFin);
		}

		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * Publie un événement fiscal d'information.
	 *
	 * @param date         Date de valeur de l'événement
	 * @param entreprise   L'entreprise concernée
	 * @param typeInfo     Le type d'information représenté par le message
	 * @param suivis       Le collector pour le suivi
	 */
	protected void emetEvtFiscalInformation(RegDate date, Entreprise entreprise, EvenementFiscalInformationComplementaire.TypeInformationComplementaire typeInfo, String message, EvenementOrganisationSuiviCollector suivis) {
		suivis.addSuivi(message);
		context.getEvenementFiscalService().publierEvenementFiscalInformationComplementaire(entreprise, typeInfo, date);
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * Méthode qui aligne les fors secondaires sur les établissements secondaires de l'entreprise.
	 * Elle crée, ferme et annule en fonction de la nécessité.
	 *
	 * La méthode crée les for secondaires uniquement sur VD.
	 *
	 * La détermination des fors eux-même est sous-traitée au service métier de haut niveau.
	 *
	 * @param entreprise l'entreprise concernée
	 * @param warnings Le collector pour les avertissements
	 * @param suivis Le collector pour le suivi
	 */
	protected void adapteForsSecondairesPourEtablissementsVD(Entreprise entreprise, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws
			EvenementOrganisationException {

		final AjustementForsSecondairesResult ajustementForsSecondaires;
		try {
			ajustementForsSecondaires = context.getMetierServicePM().calculAjustementForsSecondairesPourEtablissementsVD(entreprise);
		}
		catch (MetierServiceException e) {
			warnings.addWarning(e.getMessage());
			return;
		}

		for (ForFiscalSecondaire forAAnnuler : ajustementForsSecondaires.getAAnnuler()) {
			annulerForFiscalSecondaire(forAAnnuler, warnings, suivis);
		}

		for (AjustementForsSecondairesResult.ForAFermer forAFermer : ajustementForsSecondaires.getAFermer()) {
			closeForFiscalSecondaire(forAFermer.getDateFermeture(), forAFermer.getForFiscal(), MotifFor.FIN_EXPLOITATION, suivis);
		}
		final List<ForFiscalPrincipalPM> forsFiscauxPrincipauxActifsSorted = entreprise.getForsFiscauxPrincipauxActifsSorted();

		for (ForFiscalSecondaire forACreer : ajustementForsSecondaires.getACreer()) {
			final boolean forPrincipalCouvreLaPeriode = DateRangeHelper.isFullyCovered(forACreer, forsFiscauxPrincipauxActifsSorted);
			if (forPrincipalCouvreLaPeriode) {
				creerForFiscalSecondaire(forACreer.getDateDebut(), forACreer.getDateFin(), forACreer.getMotifRattachement(), forACreer.getNumeroOfsAutoriteFiscale(), forACreer.getTypeAutoriteFiscale(),
				                         forACreer.getMotifOuverture(), forACreer.getMotifFermeture(), forACreer.getGenreImpot(), warnings, suivis);
			} else {
				warnings.addWarning(renderNonCouvertWarning(forACreer));
				raiseStatusTo(HandleStatus.TRAITE);
			}
		}
	}

	private String renderNonCouvertWarning(ForFiscalSecondaire forSecondaireNonCouvert) throws EvenementOrganisationException {
		return String.format("Impossible de créer le for: la période de for fiscal secondaire à %s (ofs: %d) débutant le %s%s " +
				                                  "n'est pas couverte par un for principal valide. Veuillez créer ou ajuster le for à la main après avoir corrigé le for principal.",
		                     getCommune(forSecondaireNonCouvert.getNumeroOfsAutoriteFiscale(), forSecondaireNonCouvert.getDateDebut()).getNomOfficielAvecCanton(),
		                     forSecondaireNonCouvert.getNumeroOfsAutoriteFiscale(),
		                     RegDateHelper.dateToDisplayString(forSecondaireNonCouvert.getDateDebut()),
		                     forSecondaireNonCouvert.getDateFin() != null ? " et se terminant le " + RegDateHelper.dateToDisplayString(forSecondaireNonCouvert.getDateFin()) : ""
		);
	}

	private boolean isPremierSnapshot() {
		return organisation.getSitePrincipal(getDateEvt().getOneDayBefore()) == null;
	}

	/**
	 * <p>
	 *     Applique la surcharge des données civiles sur la période indiquée avec les données civiles de RCEnt de la date de valeur indiquée.
	 * </p>
	 * <p>
	 *     Les surcharges éventuellement présentes seront annulées et / ou terminées au jour précédant la période.
	 * </p>
	 *
	 * @param entreprise l'entreprise concernée
	 * @param range de quand à quand la surcharge doit être appliquée.
	 * @param dateValeur la date pour laquelle il faut rechercher les valeurs civiles dans RCEnt
	 * @throws EvenementOrganisationException En cas de problème, notamment lorsque la surcharge existante empiète ou dépasse la date de valeur
	 */
	protected void appliqueDonneesCivilesSurPeriode(Entreprise entreprise, SurchargeCorrectiveRange range, RegDate dateValeur, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		suivis.addSuivi(String.format("Application de la surcharge civile entre le %s et le %s avec les valeurs du %s",
		                              RegDateHelper.dateToDisplayString(range.getDateDebut()),
		                              RegDateHelper.dateToDisplayString(range.getDateFin()),
		                              RegDateHelper.dateToDisplayString(dateValeur)));

		empecheSurchargeExcessive(range);

		try {
			context.getTiersService().appliqueDonneesCivilesSurPeriode(entreprise, range, dateValeur);
		}
		catch (TiersException e) {
			throw new EvenementOrganisationException(String.format("Impossible d'appliquer la surcharge des données civiles: %s", e.getMessage()));
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * <p>
	 *     Applique la surcharge des données civiles sur la période indiquée avec les données civiles de RCEnt de la date de valeur indiquée.
	 * </p>
	 * <p>
	 *     Les surcharges éventuellement présentes seront annulées et / ou terminées au jour précédant la période.
	 * </p>
	 *
	 * @param etablissement l'etablissement concerné
	 * @param range de quand à quand la surcharge doit être appliquée.
	 * @param dateValeur la date pour laquelle il faut rechercher les valeurs civiles dans RCEnt
	 * @throws EvenementOrganisationException En cas de problème, notamment lorsque la surcharge existante empiète ou dépasse la date de valeur
	 */
	protected void appliqueDonneesCivilesSurPeriode(Etablissement etablissement, SurchargeCorrectiveRange range, RegDate dateValeur, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		suivis.addSuivi(String.format("Application de la surcharge civile sur l'établissement n°%s entre le %s et le %s avec les valeurs du %s",
		                              FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
		                              RegDateHelper.dateToDisplayString(range.getDateDebut()),
		                              RegDateHelper.dateToDisplayString(range.getDateFin()),
		                              RegDateHelper.dateToDisplayString(dateValeur)));

		empecheSurchargeExcessive(range);

		try {
			context.getTiersService().appliqueDonneesCivilesSurPeriode(etablissement, range, dateValeur);
		}
		catch (TiersException e) {
			throw new EvenementOrganisationException(String.format("Impossible d'appliquer la surcharge des données civiles: %s", e.getMessage()));
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	protected String afficheAttributsSite(@Nullable SiteOrganisation site, @Nullable RegDate date) {
		return getContext().getServiceOrganisation().afficheAttributsSite(site, date);
	}

	protected static class SurchargeCorrectiveRange extends DateRangeHelper.Range {

		public SurchargeCorrectiveRange(RegDate debut, RegDate fin) {
			super(debut, fin);
		}

		/**
		 * @return le nombre de jour d'une borne à l'autre incluses.
		 */
		public int getEtendue() {
			return RegDateHelper.getDaysBetween(getDateDebut(), getDateFin()) + 1;
		}

		/**
		 * @return si l'étendue de la surcharge est plus petite ou égale à la tolérance de décalage avec le RC {@link OrganisationHelper#NB_JOURS_TOLERANCE_DE_DECALAGE_RC}
		 */
		public boolean isAcceptable() {
			final int tolerance = OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC;
			return getEtendue() <= tolerance;
		}
	}

	private void empecheSurchargeExcessive(SurchargeCorrectiveRange range) throws EvenementOrganisationException {
		if (!range.isAcceptable()) {
			throw new SurchageCorrectiveExcessiveException(
					String.format("Refus d'appliquer la surcharge civile sur une période plus large (%d jours) que la tolérance admise de %d jours.",
					              range.getEtendue(),
					              OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC
					));
		}
	}

	protected static class SurchageCorrectiveExcessiveException extends EvenementOrganisationException {
		public SurchageCorrectiveExcessiveException(String message) {
			super(message);
		}
	}

	protected boolean hasCapital(Organisation organisation, RegDate date) {
		return organisation.getCapital(date) != null && organisation.getCapital(date).getCapitalLibere().longValue() > 0;
	}

	protected void traiteTransitionAdresseEffective(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis, RegDate date,
	                                                boolean utiliserCommeAdressePoursuite) throws EvenementOrganisationException {
		final AdresseSupplementaire adresseCourrier = getAdresseTiers(TypeAdresseTiers.COURRIER, date);
		if (adresseCourrier != null) {
			if (adresseCourrier.isPermanente()) {
				warnings.addWarning("L'adresse fiscale de courrier, permanente, est maintenue malgré le changement de l'adresse effective civile (issue de l'IDE).");
			} else {
				context.getAdresseService().fermerAdresse(adresseCourrier, date.getOneDayBefore());
				suivis.addSuivi("L'adresse fiscale de courrier, non-permanente, a été fermée. L'adresse de courrier est maintenant donnée par l'adresse effective civile (issue de l'IDE).");
			}
		} else {
			suivis.addSuivi("L'adresse de courrier a changé suite au changement de l'adresse effective civile (issue de l'IDE).");
		}
		final AdresseSupplementaire adresseRepresentation = getAdresseTiers(TypeAdresseTiers.REPRESENTATION, date);
		if (adresseRepresentation != null) {
			if (adresseRepresentation.isPermanente()) {
				warnings.addWarning("L'adresse fiscale de représentation, permanente, est maintenue malgré le changement de l'adresse effective civile (issue de l'IDE).");
			} else {
				context.getAdresseService().fermerAdresse(adresseRepresentation, date.getOneDayBefore());
				suivis.addSuivi("L'adresse fiscale de représentation, non-permanente, a été fermée. L'adresse de représentation est maintenant donnée par l'adresse effective civile (issue de l'IDE).");
			}
		} else {
			suivis.addSuivi("L'adresse de représentation a changé suite au changement de l'adresse effective civile (issue de l'IDE).");
		}
		final AdresseSupplementaire adressePoursuite = getAdresseTiers(TypeAdresseTiers.POURSUITE, date);
		if (utiliserCommeAdressePoursuite) {
			if (adressePoursuite != null) {
				if (adressePoursuite.isPermanente()) {
					warnings.addWarning("L'adresse fiscale poursuite, permanente, est maintenue malgré le changement de l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).");
				}
				else {
					context.getAdresseService().fermerAdresse(adressePoursuite, date.getOneDayBefore());
					suivis.addSuivi("L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).");
				}
			}
			else {
				suivis.addSuivi("L'adresse de poursuite a changé suite au changement de l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).");
			}
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	protected void traiteTransitionAdresseLegale(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis, RegDate date) throws EvenementOrganisationException {
		final AdresseSupplementaire adressePoursuite = getAdresseTiers(TypeAdresseTiers.POURSUITE, date);
		if (adressePoursuite != null) {
			if (adressePoursuite.isPermanente()) {
				warnings.addWarning("L'adresse fiscale de poursuite, permanente, est maintenue malgré le changement de l'adresse légale civile (issue du RC).");
			} else {
				context.getAdresseService().fermerAdresse(adressePoursuite, date.getOneDayBefore());
				suivis.addSuivi("L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse légale civile (issue du RC).");
			}
		} else {
			suivis.addSuivi("L'adresse de poursuite a changé suite au changement de l'adresse légale civile (issue du RC).");
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Nullable
	protected AdresseSupplementaire getAdresseTiers(TypeAdresseTiers type, RegDate date) throws EvenementOrganisationException {
		final List<AdresseTiers> adressesTiersSorted = AnnulableHelper.sansElementsAnnules(getEntreprise().getAdressesTiersSorted(type));
		if (adressesTiersSorted.isEmpty()) {
			return null;
		}
		final AdresseTiers adresseTiers = DateRangeHelper.rangeAt(adressesTiersSorted, date);
		if (adresseTiers != null) {
			if (adresseTiers.getDateDebut().isAfter(date)) { // SIFISC-19483 - N'est plus censé se produire
				throw new EvenementOrganisationException(String.format("L'adresse valide à la date demandée %s n'est pas la dernière de l'historique!", RegDateHelper.dateToDisplayString(date)));
			}
			if (adresseTiers instanceof AdresseSupplementaire) {
				return (AdresseSupplementaire) adresseTiers;
			}
			throw new EvenementOrganisationException(String.format("l'adresse %s trouvée n'est pas de type AdresseSupplementaire! Elle est de type %s", type, adresseTiers.getClass()));
		}
		return null;
	}

}
