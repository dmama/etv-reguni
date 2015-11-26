package ch.vd.uniregctb.evenement.organisation.interne;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.helper.BouclementHelper;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Classe de base des événements organisation en provenance du RCEnt.
 *
 * Note importante: - Cette classe NE doit PAS être étendue directement. Utiliser une des deux classes dérivées
 *                    officielles, EvenementOrganisationInterneAvecImpactUnireg et EvenementOrganisationInterneSansImpactUnireg.
 *
 *                  - Le status de l'événement est à REDONDANT dès le départ. Lors du traitement il faut, lorsque des données
 *                    sont modifiées et / ou quelque action est entreprise en réaction à l'événement, faire passer le status
 *                    à TRAITE au moyen de la méthode raiseStatusTo().
 *
 *                  - Le "context" est privé à cette classe. C'est intentionnel qu'il n'y a pas d'accesseur.
 *                    1) suivis.addSuivi() proprement ce qui va être fait (il faut donc passer en paramètre le collector de suivi),
 *                    2) Vérifie s'il y a redondance, le rapport dans les logs et sort le cas échéant,
 *                    3) Fait ce qui doit être fait s'il y a lieu,
 *                    4) Utilise la méthode raiseStatusTo() pour régler le statut en fonction de ce qui a été fait.
 */
public abstract class EvenementOrganisationInterne {

//	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationInterne.class);

	private final long noOrganisation;
	private Entreprise entreprise;
	private Organisation organisation;
	private String organisationDescription;

	private final RegDate dateEvt;
	private final Long numeroEvenement;

	private HandleStatus status = HandleStatus.REDONDANT;

	private final EvenementOrganisationContext context;
	private final EvenementOrganisationOptions options;

	private final TypeImpact typeImpact;

	protected static final String MSG_GENERIQUE_A_VERIFIER = "Veuillez vérifier que le traitement automatique de création de l'entreprise donne bien le résultat escompté.";

	protected EvenementOrganisationInterne(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context, EvenementOrganisationOptions options, TypeImpact typeImpact) throws EvenementOrganisationException {
		this.context = context;
		this.options = options;

		/* récupération des informations liés à l'événement */
		this.dateEvt = evenement.getDateEvenement();
		this.numeroEvenement = evenement.getId();
		this.noOrganisation = evenement.getNoOrganisation();
		this.organisation = organisation;
		this.entreprise = entreprise;

		/* Champs précalculés */
		this.organisationDescription = context.getServiceOrganisation().createOrganisationDescription(organisation, getDateEvt());

		this.typeImpact = typeImpact;
	}

	public final void validate(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		validateCommon(erreurs);
		if (!erreurs.hasErreurs()) {
			if (options.isSeulementEvtFiscaux() && typeImpact == TypeImpact.AVEC_IMPACT_UNIREG) {
				return;
			}
			validateSpecific(erreurs, warnings);
		}
	}

	/**
	 * Effectue le traitement métier voulu pour l'événement organisation courant.
	 * <p/>
	 * Cette méthode lève une exception en cas d'erreur inattendue dans le traitement (la majorité des erreurs prévisibles devraient avoir été traitées dans la méthode {@link
	 * #validate(EvenementOrganisationErreurCollector, EvenementOrganisationWarningCollector)}). Les éventuels avertissement sont renseignés dans la
	 * collection de warnings passée en paramètre. Cette méthode retourne un status qui permet de savoir si l'événement est redondant ou non.
	 * <p/>
	 * En fonction des différentes valeurs renseignées par cette méthode, le framework de traitement des événements va déterminer l'état de l'événement organisation de la manière suivante : <ul>
	 * <li>exception => état de l'événement = EN_ERREUR</li><li>status = TRAITE et pas de warnings => état de l'événement = TRAITE</li> <li>status = REDONDANT et pas de warnings => état de l'événement =
	 * REDONDANT</li> <li>status = TRAITE avec warnings => état de l'événement = A_VERIFIER</li> <li>status = REDONDANT avec warnings => état de l'événement = A_VERIFIER</li> </ul>
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
	 *     <li>Chaque traitement entraînant la création d'objet en base, ou tout autre action métier, doit être effectué au sein d'une méthode dédiée à l'opération
	 *     métier correspondante, et nommée en fonction d'elle.</li>
	 *     <li>Chaque méthode métier doit impérativement se terminer par un appel à raiseStatusTo() pour établir le status résultant de l'opération.</li>
	 *     <li>D'une manière générale, les décisions métier doivent ressortir clairement de la lecture de la méthode doHandle().</li>
	 *     <li>Les paramètres métier sont passés en paramètres des méthodes métier. En particulier les dates. En revanche, ces méthodes accédent au contexte directement.</li>
	 * </ul>
	 * </p>
	 * @param warnings une liste de warnings qui sera remplie - si nécessaire - par la méthode.
	 * @param suivis       Le collector pour le suivi
	 * @return un code de status permettant de savoir si lévénement a été traité ou s'il était redondant.
	 * @throws EvenementOrganisationException si le traitement de l'événement est impossible pour une raison ou pour une autre.
	 */
	@NotNull
	public final HandleStatus handle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		if (options.isSeulementEvtFiscaux() && typeImpact == TypeImpact.AVEC_IMPACT_UNIREG) {
			suivis.addSuivi(String.format("Opération de type %s avec impact Unireg ignorée car l'événement est forcé.", this.getClass().getSimpleName()));
			return HandleStatus.REDONDANT;
		} else {
			suivis.addSuivi(String.format("Opération de type %s lancée.", this.getClass().getSimpleName()));
		}

		this.doHandle(warnings, suivis);
		Assert.notNull(status, "Status inconnu après le traitement de l'événement interne!");
		return status;
	}

	enum TypeImpact {
		AVEC_IMPACT_UNIREG,
		SANS_IMPACT_UNIREG
	}



	/*
			Méthode à redéfinir pour implémenter le traitement concret. Voir ci-dessus handle().
		 */
	public abstract void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException;

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

	@NotNull
	protected MotifFor determineMotifOuvertureFor() throws EvenementOrganisationException {
		final MotifFor motifOuverture;
		Siege siegePrecedant = OrganisationHelper.siegePrincipalPrecedant(getOrganisation(), getDateEvt());
		if (siegePrecedant == null) {
			motifOuverture = MotifFor.DEBUT_EXPLOITATION;
		} else {
			switch (siegePrecedant.getTypeAutoriteFiscale()) {
			case COMMUNE_OU_FRACTION_VD:
				throw new EvenementOrganisationException(
						"L'organisation a un précédent siège sur Vaud. C'est donc une organisation existante inconnue jusque là (association nouvellement inscrite au RC?) Veuillez traiter le cas manuellement.");
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
		return motifOuverture;
	}

	/**
	 * Trouve le range en cours de validité et vérifie qu'il est ouvert.
	 *
	 * A utiliser pour s'assurer qu'on est en présence d'une situation "propre", c'est-à-dire d'une valeur en cours de validité. N'est valable
	 * que pour une liste représentant l'historique d'une donnée (une seule valeur à la fois). La liste n'a pas besoin d'être dans l'ordre chronologique.
	 *
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

	protected abstract void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException;

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

	public EvenementOrganisationContext getContext() {
		return context;
	}

	public EvenementOrganisationOptions getOptions() {
		return options;
	}

	public Long getNumeroEvenement() {
		return numeroEvenement;
	}

	public RegDate getDateEvt() {
		return dateEvt;
	}

	public long getNoOrganisation() {
		return noOrganisation;
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

	protected Etablissement getEtablissementByNumeroSite(long numeroSite) {
		return context.getTiersDAO().getEtablissementByNumeroSite(numeroSite);
	}

	protected void programmeReindexation(Entreprise pm, EvenementOrganisationSuiviCollector suivis) {
		Audit.info(getNumeroEvenement(), String.format("Déclenchement de la réindexation pour l'entreprise %s.", pm.getNumero()));
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
		return (Entreprise) context.getTiersDAO().save(entreprise);
	}

	protected void createEntreprise(RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		Assert.notNull(organisation);
		Assert.notNull(dateDebut);

		final Entreprise entreprise = createEntreprise(noOrganisation);
		suivis.addSuivi(String.format("Entreprise créée avec le numéro %s pour l'organisation %s", entreprise.getNumero(), noOrganisation));
		setEntreprise(entreprise);
		raiseStatusTo(HandleStatus.TRAITE);

		openRegimesFiscauxOrdinairesCHVD(entreprise, organisation, dateDebut, suivis);
	}

	/**
	 * En fonction de la catégorie d'entreprise, détermination du régime fiscal par défaut
	 * @param organisation données civiles
	 * @param date       date de référence
	 * @return le type de régime fiscal par défaut...
	 */
	protected final TypeRegimeFiscal getRegimeFiscalParDefaut(Organisation organisation, RegDate date) {
		final CategorieEntreprise categorie = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, date);
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case AUTRE:
		case DPPM:
		case PM:
			return getRegimeFiscalParDefaultPM();
		case DP:
		case APM:
		case FP:
			return getRegimeFiscalParDefaultAPM();
		case SP:
		case PP:
			return null;
		default:
			throw new IllegalArgumentException("Catégorie entreprise inconnue : " + categorie);
		}
	}

	private TypeRegimeFiscal getRegimeFiscalParDefaultPM() {
		final List<TypeRegimeFiscal> regimes = context.getServiceInfra().getRegimesFiscaux();
		for (TypeRegimeFiscal regime : regimes) {
			if (regime.isDefaultPourPM()) {
				return regime;
			}
		}
		throw new IllegalArgumentException("Impossible de déterminer le régime fiscal par défaut (PM).");
	}

	private TypeRegimeFiscal getRegimeFiscalParDefaultAPM() {
		final List<TypeRegimeFiscal> regimes = context.getServiceInfra().getRegimesFiscaux();
		for (TypeRegimeFiscal regime : regimes) {
			if (regime.isDefaultPourAPM()) {
				return regime;
			}
		}
		throw new IllegalArgumentException("Impossible de déterminer le régime fiscal par défaut (APM).");
	}

	protected void openRegimesFiscauxOrdinairesCHVD(Entreprise entreprise, Organisation organisation, RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		// Le régime fiscal VD + CH
		final TypeRegimeFiscal typeRegimeFiscal = getRegimeFiscalParDefaut(organisation, dateDebut);
		if (typeRegimeFiscal != null) {
			context.getTiersService().openRegimeFiscal(entreprise, RegimeFiscal.Portee.CH, typeRegimeFiscal, dateDebut);
			context.getTiersService().openRegimeFiscal(entreprise, RegimeFiscal.Portee.VD, typeRegimeFiscal, dateDebut);
			suivis.addSuivi(
					String.format("Régimes fiscaux ordinaires VD et CH ouverts pour l'entreprise %s (civil: %d)", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), noOrganisation));
		}
		else {
			suivis.addSuivi(String.format("Aucun régime fiscal ouvert pour l'entreprise %s (civil: %d)", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), noOrganisation));
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	protected void closeRegimesFiscauxOrdinairesCHVD(RegimeFiscal regimeFiscalCH, RegimeFiscal regimeFiscalVD, RegDate dateFin, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// Le régime fiscal VD + CH
		context.getTiersService().closeRegimeFiscal(regimeFiscalCH, dateFin);
		context.getTiersService().closeRegimeFiscal(regimeFiscalVD, dateFin);

		suivis.addSuivi(String.format("Régimes fiscaux VD et CH fermés pour l'entreprise numéro %s (civil: %s)", entreprise.getNumero(), noOrganisation));
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
					String.format("Refus d'écraser l'instance d'entreprise existante [no: %s, no organisation: %s]. Arrêt du traitement de l'événement.",
					              entreprise.getNumero(),
					              entreprise.getNumeroEntreprise())
			);
		}
		this.entreprise = entreprise;
	}

	/**
	 * Créer un établissement, avec toutes ses caractéristiques usuelles, et le rattacher à l'entreprise en cours au
	 * moyen d'un rapport entre tiers d'activité économique.
	 * @param numeroSite Le numéro du site sur lequel porte l'établissement
	 * @param autoriteFiscale La commune politique de domicile de l'établissement
	 * @param principal Si l'établissement est principal ou secondaire
	 * @param suivis       Le collector pour le suivi
	 * @param dateDebut Date de début
	 */
	protected void createAddEtablissement(Long numeroSite, Siege autoriteFiscale, boolean principal, RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		Assert.notNull(numeroSite);
		Assert.notNull(autoriteFiscale);
		Assert.notNull(dateDebut);

		// L'établissement
		Etablissement etablissement = (Etablissement) context.getTiersDAO().save(createEtablissement(numeroSite));
		// Le domicile
		context.getTiersDAO().addAndSave(etablissement, new DomicileEtablissement(dateDebut, null, autoriteFiscale.getTypeAutoriteFiscale(), autoriteFiscale.getNoOfs(), etablissement));
		// L'activité économique
		getContext().getTiersService().addRapport(new ActiviteEconomique(dateDebut, null, entreprise, etablissement, principal), getEntreprise(), etablissement);

		final String commune = DateRangeHelper.rangeAt(context.getServiceInfra().getCommuneHistoByNumeroOfs(autoriteFiscale.getNoOfs()), dateDebut).getNomOfficielAvecCanton();

		suivis.addSuivi(String.format("Etablissement %s créé avec le numéro %s pour le site %s, domicile %s (ofs: %s), à partir du %s",
		                              principal ? "principal" : "secondaire",
		                              etablissement.getNumero(),
		                              numeroSite,
		                              commune,
		                              autoriteFiscale.getNoOfs(),
		                              RegDateHelper.dateToDisplayString(dateDebut)));
		raiseStatusTo(HandleStatus.TRAITE);
	}

	private Etablissement createEtablissement(Long numeroSite) {
		final Etablissement etablissement = new Etablissement();
		etablissement.setNumeroEtablissement(numeroSite);
		return (Etablissement) context.getTiersDAO().save(etablissement);
	}

	/**
	 * Ouvre un nouveau for fiscal principal.
	 *
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param autoriteFiscale          l'autorité fiscale sur laquelle est ouvert le nouveau for.
	 * @param rattachement             le motif de rattachement du nouveau for
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @param suivis       Le collector pour le suivi
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipalPM openForFiscalPrincipal(final RegDate dateOuverture, Siege autoriteFiscale,
	                                                      MotifRattachement rattachement, MotifFor motifOuverture, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) {
		Assert.notNull(motifOuverture, "Le motif d'ouverture est obligatoire sur un for principal dans le canton");

		final Commune commune = context.getServiceInfra().getCommuneByNumeroOfs(autoriteFiscale.getNoOfs(), dateOuverture);
		if (!commune.isPrincipale()) {
			suivis.addSuivi(String.format("Ouverture d'un for fiscal principal pour l'entreprise no %s avec le no organisation civil %s, à partir de %s, motif ouverture %s, rattachement %s.",
			                              entreprise.getNumero(), entreprise.getNumeroEntreprise(),
			                              RegDateHelper.dateToDisplayString(dateOuverture), motifOuverture, rattachement));
			raiseStatusTo(HandleStatus.TRAITE);
			return context.getTiersService().openForFiscalPrincipal(entreprise, dateOuverture, rattachement, autoriteFiscale.getNoOfs(), autoriteFiscale.getTypeAutoriteFiscale(), motifOuverture);
		} else {
			warnings.addWarning(
					String.format("Ouverture de for fiscal principal sur une commune faîtière de fractions, %s: Veuillez saisir le for fiscal principal manuellement.",
					              commune.getNomOfficielAvecCanton()));
		}
		return null;
	}

	/**
	 * Ouvre un nouveau for fiscal secondaire. // TODO: verifier qu'on n'a pas besoin de créer une classe spécifique PM pour les for secondiares établissements.
	 *
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param autoriteFiscale          l'autorité fiscale sur laquelle est ouvert le nouveau for.
	 * @param rattachement             le motif de rattachement du nouveau for
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @param suivis       Le collector pour le suivi
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalSecondaire openForFiscalSecondaire(final RegDate dateOuverture, Siege autoriteFiscale,
	                                                      MotifRattachement rattachement, MotifFor motifOuverture, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) {
		final Commune commune = context.getServiceInfra().getCommuneByNumeroOfs(autoriteFiscale.getNoOfs(), dateOuverture);
		if (!commune.isPrincipale()) {
			Assert.notNull(motifOuverture, "Le motif d'ouverture est obligatoire sur un for secondaire dans le canton"); // TODO: is it?
			suivis.addSuivi(String.format("Ouverture d'un for fiscal secondaire pour l'entreprise no %s avec le no organisation civil %s, à partir de %s, motif ouverture %s, rattachement %s.",
			                              entreprise.getNumero(), entreprise.getNumeroEntreprise(),
			                              RegDateHelper.dateToDisplayString(dateOuverture), motifOuverture, rattachement));
			raiseStatusTo(HandleStatus.TRAITE);
			return context.getTiersService().openForFiscalSecondaire(entreprise, dateOuverture, rattachement, autoriteFiscale.getNoOfs(), autoriteFiscale.getTypeAutoriteFiscale(), motifOuverture);
		} else {
			warnings.addWarning(
					String.format("Ouverture de for fiscal secondaire sur une commune faîtière de fractions, %s: Veuillez saisir le for fiscal secondaire manuellement.",
					              commune.getNomOfficielAvecCanton()));
		}
		return null;
	}

	/**
	 * Ferme l'actuel for principal.
	 *
	 * @param dateDeFermeture la date à laquelle l'ancien for est fermé
	 * @param motifFermeture  le motif de fermeture du for fiscal principal
	 * @param suivis       Le collector pour le suivi
	 * @return
	 */
	protected ForFiscalPrincipal closeForFiscalPrincipal(RegDate dateDeFermeture, MotifFor motifFermeture, EvenementOrganisationSuiviCollector suivis) {

		suivis.addSuivi(String.format("Fermeture du for principal pour l'entreprise %s (civil: %s), en date du %s, motif fermeture %s",
		                              entreprise.getNumero(), entreprise.getNumeroEntreprise(), RegDateHelper.dateToDisplayString(dateDeFermeture), motifFermeture));

		raiseStatusTo(HandleStatus.TRAITE);
		return context.getTiersService().closeForFiscalPrincipal(entreprise, dateDeFermeture, motifFermeture);
	}

	protected void reopenForFiscalPrincipal(ForFiscalPrincipal forFiscalPrincipal, EvenementOrganisationSuiviCollector suivis) {
		suivis.addSuivi(String.format("Réouverture du for principal pour l'entreprise %s (civil: %s), qui commençait le %s%s.",
		                              entreprise.getNumero(), entreprise.getNumeroEntreprise(),
		                              forFiscalPrincipal.getDateDebut(),
		                              forFiscalPrincipal.getDateFin() != null ? ", et terminait le " + forFiscalPrincipal.getDateFin() : ""));
		context.getTiersService().annuleForFiscal(forFiscalPrincipal);
		context.getTiersService().reopenFor(forFiscalPrincipal, forFiscalPrincipal.getTiers());
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * Ajoute un bouclement en bonne et due forme.
	 * @param dateDebut Date de début du bouclement
	 */
	protected void createAddBouclement(RegDate dateDebut, EvenementOrganisationSuiviCollector suivis) {
		final Bouclement bouclement;
		boolean isCreationPure = OrganisationHelper.siegePrincipalPrecedant(organisation, getDateEvt()) == null;

		if (isCreationPure) {
			bouclement = BouclementHelper.createBouclement3112SelonSemestre(dateDebut);
		} else {
			bouclement = BouclementHelper.createBouclement3112(RegDate.get(dateDebut.year() - 1, 12, 31));
		}
		bouclement.setEntreprise(entreprise);
		context.getTiersDAO().addAndSave(entreprise, bouclement);
		RegDate premierBouclement = RegDate.get(bouclement.getDateDebut().year(), bouclement.getAncrage().month(), bouclement.getAncrage().day());
		suivis.addSuivi(String.format("Bouclement créé avec une périodicité de %s mois à partir du %s",
		                              bouclement.getPeriodeMois(), RegDateHelper.dateToDisplayString(premierBouclement)));
		raiseStatusTo(HandleStatus.TRAITE);
	}

	/**
	 * Opère un changement de domicile sur un établissement donné.
	 *
	 * Note: La méthode utilise les dates avant/après de l'événement interne en cours de traitement.
	 *
	 * @param etablissement L'établissement concerné par le changement de domicile
	 * @param siegeApres    Le siège d'où extrapoler le domicile.
	 * @param dateAvant     La date du dernier jour du domicile précédant
	 * @param dateApres     La date du premier jour du nouveau domicile
	 * @param suivis        Le collector pour le suivi
	 */
	protected void changeDomicileEtablissement(@NotNull Etablissement etablissement, @NotNull Siege siegeApres, @NotNull RegDate dateAvant, @NotNull RegDate dateApres, EvenementOrganisationSuiviCollector suivis) {
		final DomicileEtablissement domicilePrecedant = DateRangeHelper.rangeAt(etablissement.getSortedDomiciles(false), dateApres);
		context.getTiersService().closeDomicileEtablissement(domicilePrecedant, dateAvant);
		context.getTiersService().addDomicileEtablissement(etablissement, siegeApres.getTypeAutoriteFiscale(),
		                                                   siegeApres.getNoOfs(), dateApres, null);

		Commune communePrecedante = context.getServiceInfra().getCommuneByNumeroOfs(domicilePrecedant.getNumeroOfsAutoriteFiscale(), dateAvant);
		Commune nouvelleCommune = context.getServiceInfra().getCommuneByNumeroOfs(siegeApres.getNoOfs(), dateApres);

		suivis.addSuivi(
		           String.format("Changement du domicile de l'établissement no %s (civil: %s) de %s (civil: %s) vers %s (civil: %s).",
		                         etablissement.getNumero(), etablissement.getNumeroEtablissement(),
		                         communePrecedante.getNomOfficielAvecCanton(), domicilePrecedant.getNumeroOfsAutoriteFiscale(),
		                         nouvelleCommune.getNomOfficielAvecCanton(), nouvelleCommune.getNoOFS())
		);

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

}
