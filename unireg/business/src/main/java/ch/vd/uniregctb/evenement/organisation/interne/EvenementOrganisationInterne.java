package ch.vd.uniregctb.evenement.organisation.interne;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.helper.BouclementHelper;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

/**
 *
 * Implémentation des événements organisation en provenance du RCEnt.
 */
public abstract class EvenementOrganisationInterne {

//	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationInterne.class);

	private final long noOrganisation;
	private Entreprise entreprise;
	private Organisation organisation;
	private String organisationDescription;

	private final RegDate dateEvt;
	private final Long numeroEvenement;

	private HandleStatus status;

	protected final EvenementOrganisationContext context;
	private final EvenementOrganisationOptions options;

	protected static final String MSG_GENERIQUE_A_VERIFIER = "Veuillez vérifier que le traitement automatique de création de l'entreprise donne bien le résultat escompté.";

	protected EvenementOrganisationInterne(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws EvenementOrganisationException {
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
	}

	public final void validate(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		validateCommon(erreurs);
		if (!erreurs.hasErreurs()) {
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
	 *
	 * @param warnings une liste de warnings qui sera remplie - si nécessaire - par la méthode.
	 * @return un code de status permettant de savoir si lévénement a été traité ou s'il était redondant.
	 * @throws EvenementOrganisationException si le traitement de l'événement est impossible pour une raison ou pour une autre.
	 */
	@NotNull
	public final HandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		this.doHandle(warnings);
		if (status == null) {
			throw new EvenementOrganisationException("Status inconnu après le traitement de l'événement interne!");
		}
		return status;
	}

	/*
	Méthode à redéfinir pour implémenter le traitement concret.
	 */
	public abstract void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException;

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

	/**
	 * Védifie qu'il n'existe pas déjà un établissement pour le site donné. Lance une exception dans
	 * le cas contraire.
	 * @param numeroSite Le numéro de registre civil du site
	 * @param date La date pour laquelle la présence doit être contrôlée
	 * @throws EvenementOrganisationException Si l'établissement existe déjà
	 */
	protected void ensureNotExistsEtablissement(long numeroSite, RegDate date) throws EvenementOrganisationException {
		Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroSite(numeroSite);
		if (etablissement != null) {
			throw new EvenementOrganisationException(
					String.format("Trouvé un établissement existant %s pour l'organisation en création %s %s. Impossible de continuer.",
					              numeroSite, getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), date)));
		}
	}

	/**
	 * Determiner le siège principal pour la date
	 * @param sitePrincipal
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@NotNull
	protected Siege determineAutoriteFiscalePrincipale(SiteOrganisation sitePrincipal, RegDate date) throws EvenementOrganisationException {
		final RegDate theDate = date != null ? date : RegDate.get();
		final Siege siegePrincipal = sitePrincipal.getSiege(theDate);
		if (siegePrincipal == null) { // Indique un établissement "probablement" à l'étranger. Nous ne savons pas traiter ce cas pour l'instant.
			throw new EvenementOrganisationException(
					String.format(
							"Autorité fiscale (siège) introuvable pour le site principal %s de l'organisation %s %s. Site probablement à l'étranger. Impossible de créer le domicile de l'établissement principal.",
							sitePrincipal.getNumeroSite(), getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), theDate)));
		}
		return siegePrincipal;
	}

	/**
	 * Déterminer le siège secondaire pour la date
	 * @param site
	 * @param date
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@NotNull
	protected Siege determineAutoriteFiscaleSiteSecondaire(SiteOrganisation site, RegDate date) throws EvenementOrganisationException {
		final RegDate theDate = date != null ? date : RegDate.get();
		final Siege siege = site.getSiege(theDate);
		if (siege == null) {
			throw new EvenementOrganisationException(
					String.format(
							"Autorité fiscale (siège) introuvable pour le site secondaire %s de l'organisation %s %s. Site probablement à l'étranger. Impossible pour le moment de créer le domicile de l'établissement secondaire.",
							site.getNumeroSite(), getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), theDate)));
		}
		return siege;
	}


	protected void createEntreprise(Long noOrganisation, RegDate dateDebut) {
		Assert.notNull(noOrganisation);
		Assert.notNull(dateDebut);

		final Entreprise entreprise = new Entreprise();
		// Le numéro
		entreprise.setNumeroEntreprise(noOrganisation);
		// Le régime fiscal VD + CH
		entreprise.addRegimeFiscal(new RegimeFiscal(dateDebut, null, RegimeFiscal.Portee.CH, TypeRegimeFiscal.ORDINAIRE));
		entreprise.addRegimeFiscal(new RegimeFiscal(dateDebut, null, RegimeFiscal.Portee.VD, TypeRegimeFiscal.ORDINAIRE));
		// Persistence
		setEntreprise((Entreprise) context.getTiersDAO().save(entreprise));

		Audit.info(String.format("Entreprise créée avec le numéro %s pour l'organisation %s", getEntreprise().getNumero(), noOrganisation));
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
	 * @param dateDebut Date de début
	 */
	protected void createAddEtablissement(Long numeroSite, Siege autoriteFiscale, boolean principal, RegDate dateDebut) {
		Assert.notNull(numeroSite);
		Assert.notNull(autoriteFiscale);
		Assert.notNull(dateDebut);

		// L'établissement
		Etablissement etablissement = (Etablissement) context.getTiersDAO().save(createEtablissement(numeroSite, principal));
		// Le domicile
		context.getTiersDAO().addAndSave(etablissement, new DomicileEtablissement(dateDebut, null, autoriteFiscale.getTypeAutoriteFiscale(), autoriteFiscale.getNoOfs(), etablissement));
		// L'activité économique
		getContext().getTiersService().addRapport(new ActiviteEconomique(dateDebut, null, entreprise, etablissement), getEntreprise(), etablissement);

		final String commune = DateRangeHelper.rangeAt(context.getServiceInfra().getCommuneHistoByNumeroOfs(autoriteFiscale.getNoOfs()), dateDebut).getNomOfficielAvecCanton();

		Audit.info(String.format("Etablissement %s créé avec le numéro %s pour le site %s, domicile %s (ofs: %s), à partir du %s",
		                         principal ? "principal" : "secondaire",
		                         etablissement.getNumero(),
		                         numeroSite,
		                         commune,
		                         autoriteFiscale.getNoOfs(),
		                         RegDateHelper.dateToDisplayString(dateDebut)));
		raiseStatusTo(HandleStatus.TRAITE);
	}

	private Etablissement createEtablissement(Long numeroSite, boolean principal) {
		final Etablissement etablissement = new Etablissement();
		etablissement.setNumeroEtablissement(numeroSite);
		etablissement.setPrincipal(principal);
		return (Etablissement) context.getTiersDAO().save(etablissement);
	}

	/**
	 * Ouvre un nouveau for fiscal principal.
	 *
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param rattachement             le motif de rattachement du nouveau for
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipalPM openForFiscalPrincipal(final RegDate dateOuverture, TypeAutoriteFiscale typeAutoriteFiscale, int numeroOfsAutoriteFiscale,
	                                                      MotifRattachement rattachement, MotifFor motifOuverture, EvenementOrganisationWarningCollector warnings) {
		Assert.notNull(motifOuverture, "Le motif d'ouverture est obligatoire sur un for principal dans le canton");

		final Commune commune = context.getServiceInfra().getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, dateOuverture);
		if (!commune.isPrincipale()) {
			Audit.info(getNumeroEvenement(), String.format("Ouverture d'un for fiscal principal pour l'entreprise no %s avec le no organisation civil %s, à partir de %s, motif ouverture %s, rattachement %s.",
			                                               entreprise.getNumero(), entreprise.getNumeroEntreprise(),
			                                               RegDateHelper.dateToDisplayString(dateOuverture), motifOuverture, rattachement));
			return context.getTiersService().openForFiscalPrincipal(entreprise, dateOuverture, rattachement, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifOuverture);
		} else {
			warnings.addWarning(
					String.format("Ouverture de for fiscal principal sur une commune faîtière de fractions, %s: Veuillez saisir le for fiscal principal manuellement.",
					              commune.getNomOfficielAvecCanton()));
		}
		raiseStatusTo(HandleStatus.TRAITE);
		return null;
	}

	/**
	 * Ouvre un nouveau for fiscal secondaire. // TODO: verifier qu'on n'a pas besoin de créer une classe spécifique PM pour les for secondiares établissements.
	 *
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param rattachement             le motif de rattachement du nouveau for
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalSecondaire openForFiscalSecondaire(final RegDate dateOuverture, TypeAutoriteFiscale typeAutoriteFiscale, int numeroOfsAutoriteFiscale,
	                                                      MotifRattachement rattachement, MotifFor motifOuverture, EvenementOrganisationWarningCollector warnings) {
		final Commune commune = context.getServiceInfra().getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, dateOuverture);
		if (!commune.isPrincipale()) {
			Assert.notNull(motifOuverture, "Le motif d'ouverture est obligatoire sur un for secondaire dans le canton"); // TODO: is it?
			Audit.info(getNumeroEvenement(), String.format("Ouverture d'un for fiscal secondaire pour l'entreprise no %s avec le no organisation civil %s, à partir de %s, motif ouverture %s, rattachement %s.",
			                                               entreprise.getNumero(), entreprise.getNumeroEntreprise(),
			                                               RegDateHelper.dateToDisplayString(dateOuverture), motifOuverture, rattachement));
			return context.getTiersService().openForFiscalSecondaire(entreprise, dateOuverture, rattachement, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifOuverture);
		} else {
			warnings.addWarning(
					String.format("Ouverture de for fiscal secondaire sur une commune faîtière de fractions, %s: Veuillez saisir le for fiscal secondaire manuellement.",
					              commune.getNomOfficielAvecCanton()));
		}
		raiseStatusTo(HandleStatus.TRAITE);
		return null;
	}

	/**
	 * Ajoute un bouclement en bonne et due forme.
	 * @param dateDebut Date de début du bouclement
	 */
	protected void createAddBouclement(RegDate dateDebut) {
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
		Audit.info(getNumeroEvenement(), String.format("Bouclement créé avec une périodicité de %s mois à partir du %s",
		                                               bouclement.getPeriodeMois(), RegDateHelper.dateToDisplayString(premierBouclement)));
		raiseStatusTo(HandleStatus.TRAITE);
	}
}
