package ch.vd.unireg.evenement.civil.interne;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersException;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Implémentation des événement civils en provenance du host.
 */
public abstract class EvenementCivilInterne {

//	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilInterne.class);

	public static final long NO_OFS_FRACTION_SENTIER = 8000;

	// L'individu principal.
	private final Long noIndividu;
	private final PersonnePhysique principalPP;
	private Individu individuPrincipal;

	// Le conjoint (mariage ou pacs).
	private final Long noIndividuConjoint;
	private final PersonnePhysique conjointPP;
	private Individu conjoint;

	private final RegDate date;
	private final Long numeroEvenement;
	private final Integer numeroOfsCommuneAnnonce;

	private AttributeIndividu[] parts;
	protected final EvenementCivilContext context;

	/**
	 * Construit un événement civil interne sur la base d'un événement civil externe.
	 *
	 * @param evenement un événement civil externe
	 * @param context   le context d'exécution de l'événement
	 * @param options les options de traitement de l'événement
	 * @throws ch.vd.unireg.evenement.civil.common.EvenementCivilException si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */
	protected EvenementCivilInterne(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		this.context = context;

		/* récupération des informations liés à l'événement civil */
		this.date = evenement.getDateEvenement();
		this.numeroEvenement = evenement.getId();
		this.numeroOfsCommuneAnnonce = evenement.getNumeroOfsCommuneAnnonce();
		this.noIndividu = evenement.getNumeroIndividuPrincipal();
		this.principalPP = this.noIndividu == null ? null : context.getTiersDAO().getPPByNumeroIndividu(this.noIndividu, true);
		this.noIndividuConjoint = evenement.getNumeroIndividuConjoint();
		this.conjointPP = this.noIndividuConjoint == null ? null : context.getTiersDAO().getPPByNumeroIndividu(this.noIndividuConjoint, true);

		if (noIndividu != null && options.isRefreshCache()) {
			refreshIndividuCache(noIndividu, context);
		}
	}

	protected EvenementCivilInterne(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		this.context = context;

		/* récupération des informations liés à l'événement civil */
		this.date = evenement.getDateEvenement();
		this.numeroEvenement = evenement.getId();
		this.numeroOfsCommuneAnnonce = null;
		this.noIndividu = evenement.getNumeroIndividu();
		this.principalPP = this.noIndividu == null ? null : getPersonnePhysiqueOrNull(this.noIndividu, true);
		this.noIndividuConjoint = null;
		this.conjointPP = null;

		if (noIndividu != null && options.isRefreshCache()) {
			// pas la peine de rafraîchir le cache de l'individu, cela a déjà été fait bien avant
			refreshConjointCache(noIndividu, context);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected EvenementCivilInterne(Individu individu, @Nullable Individu conjoint, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		this.context = context;

		this.individuPrincipal = individu;
		this.noIndividu = (individu == null ? null : individu.getNoTechnique());
		this.principalPP = this.noIndividu == null ? null : context.getTiersDAO().getPPByNumeroIndividu(this.noIndividu, true);

		this.conjoint = conjoint;
		this.noIndividuConjoint = (conjoint == null ? null : conjoint.getNoTechnique());
		this.conjointPP = this.noIndividuConjoint == null ? null : context.getTiersDAO().getPPByNumeroIndividu(this.noIndividuConjoint, true);

		this.date = dateEvenement;
		this.numeroEvenement = 0L;
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;

		if (noIndividu != null && context.getCivilDataEventNotifier() != null) {
			refreshIndividuCache(noIndividu, context);
		}
	}

	private void refreshIndividuCache(Long noIndividu, EvenementCivilContext context) {

		// on doit d'abord invalider le cache de l'individu de l'événement afin que l'appel à getIndividu() soit pertinent
		context.getCivilDataEventNotifier().notifyIndividuChange(noIndividu);

		// éventuellement, on rafraîchit également le cache des individus conjoints
		refreshConjointCache(noIndividu, context);
	}

	private void refreshConjointCache(Long noIndividu, EvenementCivilContext context) {

		// si demandé par le type d'événement, le cache des invididus conjoints doit être rafraîchi lui-aussi
		if (forceRefreshCacheConjoint()) {

			// récupération du numéro de l'individu conjoint (en fait, on va prendre tous les conjoints connus)
			final Set<Long> conjoints = context.getServiceCivil().getNumerosIndividusConjoint(noIndividu);

			// nettoyage du cache pour tous ces individus
			if (conjoints != null && conjoints.size() > 0) {
				for (Long noInd : conjoints) {
					context.getCivilDataEventNotifier().notifyIndividuChange(noInd);
				}
			}
		}
	}

	public final void validate(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		validateCommon(erreurs);
		if (!erreurs.hasErreurs()) {
			validateSpecific(erreurs, warnings);
		}
	}

	/**
	 * Effectue le traitement métier voulu pour l'événement civil courant.
	 * <p/>
	 * Cette méthode lève une exception en cas d'erreur inattendue dans le traitement (la majorité des erreurs prévisibles devraient avoir été traitées dans la méthode {@link
	 * #validate(ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector, ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector)}). Les éventuels avertissement sont renseignés dans la
	 * collection de warnings passée en paramètre. Cette méthode retourne un status qui permet de savoir si l'événement est redondant ou non.
	 * <p/>
	 * En fonction des différentes valeurs renseignées par cette méthode, le framework de traitement des événements civils va déterminer l'état de l'événement civil de la manière suivante : <ul>
	 * <li>exception => état de l'événement = EN_ERREUR</li><li>status = TRAITE et pas de warnings => état de l'événement = TRAITE</li> <li>status = REDONDANT et pas de warnings => état de l'événement =
	 * REDONDANT</li> <li>status = TRAITE avec warnings => état de l'événement = A_VERIFIER</li> <li>status = REDONDANT avec warnings => état de l'événement = A_VERIFIER</li> </ul>
	 *
	 * @param warnings une liste de warnings qui sera remplie - si nécessaire - par la méthode.
	 * @return un code de status permettant de savoir si lévénement a été traité ou s'il était redondant.
	 * @throws EvenementCivilException si le traitement de l'événement est impossible pour une raison ou pour une autre.
	 */
	@NotNull
	public abstract HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException;

	/**
	 * Validation commune l'objet target passé en paramètre.
	 *
	 *
	 * @param erreurs  les éventuelles erreurs trouvées (out)
	 * @param warnings les éventuels warnings trouvés (out)
	 * @throws ch.vd.unireg.evenement.civil.common.EvenementCivilException
	 *          en cas d'erreur dans le traitement de l'événement civil.
	 */
	protected abstract void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException;

	protected void validateCommon(EvenementCivilErreurCollector erreurs) {

		/*
		 * Vérifie que les éléments de base sont renseignés
		 */
		if (getDate() == null) {
			erreurs.addErreur("L'événement n'est pas daté");
			return;
		}

		/*
		 * La date de l'événement se situe dans le futur.
		 */
		if (getDate().isAfter(RegDate.get())) {
			erreurs.addErreur("La date de l'événement est dans le futur");
		}

		/*
		 * Cas particulier de l'arrivée ou de la naissance : le ou les contribuables ne sont en principe pas présents.
		 */
		if (isContribuableObligatoirementConnuAvantTraitement()) {
			/*
			 * Il n’existe pas de tiers contribuable correspondant à l’individu, assujetti ou non (mineur, conjoint) correspondant à
			 * l’individu.
			 */
			if (getPrincipalPP() == null) {
				erreurs.addErreur("Aucun tiers contribuable ne correspond au numéro d'individu " + getNoIndividu());
			}

			/*
			 * Il n’existe pas de tiers contribuable correspondant au conjoint, assujetti ou non (mineur, conjoint) correspondant à
			 * l’individu.
			 */
			if (getNoIndividuConjoint() != null && getConjointPP() == null) {
				erreurs.addErreur("Aucun tiers contribuable ne correspond au numéro d'individu du conjoint " + getNoIndividuConjoint());
			}
		}

		// en tout cas, l'individu devrait exister dans le registre civil !
		final Individu individu = getIndividu();
		if (individu == null) {
			erreurs.addErreur("L'individu est introuvable dans le registre civil!");
		}
	}

	protected boolean isContribuableObligatoirementConnuAvantTraitement() {
		return isContribuablePresentBefore();
	}

	protected boolean forceRefreshCacheConjoint() {
		return false;
	}

	/**
	 * @param etat l'état final de l'événement civil après (tentative de) traitement
	 * @param commentaireTraitement commentaire de traitement présent dans l'événement civil à la fin du traitement
	 * @return <code>true</code> si le commentaire de traitement doit être éliminé car il n'y a pas de sens de le garder compte tenu de l'état final de l'événement
	 */
	public boolean shouldResetCommentaireTraitement(EtatEvenementCivil etat, String commentaireTraitement) {
		return false;
	}

	/**
	 * Doit-être implémenté par les classes dérivées pour savoir quelles parts demander au service civil pour l'individu pointé par l'événement civil
	 *
	 * @param parts ensemble à remplir
	 */
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
	}

	public Long getNoIndividuConjoint() {
		return noIndividuConjoint;
	}

	public final Individu getConjoint() {
		if (conjoint == null && noIndividuConjoint != null) { // lazy init
			conjoint = context.getServiceCivil().getIndividu(noIndividuConjoint, date);
		}
		return conjoint;
	}

	public PersonnePhysique getConjointPP() {
		return conjointPP;
	}

	public RegDate getDate() {
		return date;
	}

	public Long getNoIndividu() {
		return noIndividu;
	}

	private AttributeIndividu[] getParts() {
		if (parts == null) {
			initParts(); //lazy init
		}
		return parts;
	}

	private void initParts() {
		/*
		 * Récupération des informations sur l'individu depuis le host. En plus des états civils, on peut vouloir les adresses, le conjoint,
		 * les enfants... (enfin, chaque adapteur d'événement sait ce dont il a besoin en plus...)
		 */
		final Set<AttributeIndividu> requiredParts = EnumSet.noneOf(AttributeIndividu.class);
		fillRequiredParts(requiredParts);
		parts = requiredParts.toArray(new AttributeIndividu[0]);
	}

	@Nullable
	public Individu getIndividu() {
		if (individuPrincipal == null && noIndividu != null) { // lazy init
			initIndividu();
		}
		return individuPrincipal;
	}

	@NotNull
	public Individu getIndividuOrThrowException() throws IndividuNotFoundException {
		if (individuPrincipal == null) { // lazy init
			if (noIndividu == null) {
				throw new IllegalArgumentException("Le numéro d'invidu principal est nul");
			}
			initIndividu();
			if (individuPrincipal == null) {
				throw new IndividuNotFoundException(noIndividu);
			}
		}
		return individuPrincipal;
	}

	private void initIndividu() {
		individuPrincipal = context.getServiceCivil().getIndividu(noIndividu, date, getParts());
	}

	public PersonnePhysique getPrincipalPP() {
		return principalPP;
	}

	public final Long getNumeroEvenement() {
		return numeroEvenement;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public boolean isContribuablePresentBefore() {
		return true;
	}

	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	@Nullable
	protected final PersonnePhysique getPersonnePhysiqueOrNull(long noIndividu, boolean doNotAutoFlush) {
		return context.getTiersDAO().getPPByNumeroIndividu(noIndividu, doNotAutoFlush);
	}

	/**
	 * @param noIndividu le numéro d'individu
	 * @param errors     la collection des erreurs qui sera remplie automatiquement si l'habitant n'existe pas
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu, ou <b>null<b> si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected final PersonnePhysique getPersonnePhysiqueOrFillErrors(long noIndividu, EvenementCivilErreurCollector errors) {
		final PersonnePhysique habitant = getPersonnePhysiqueOrNull(noIndividu, false);
		if (habitant == null) {
			errors.addErreur(String.format("L'habitant avec le numéro d'individu = %d n'existe pas dans le registre.", noIndividu));
		}
		return habitant;
	}

	/**
	 * @param noIndividu un numéro d'individu
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu.
	 * @throws ch.vd.unireg.evenement.civil.common.EvenementCivilException
	 *          si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected final PersonnePhysique getPersonnePhysiqueOrThrowException(long noIndividu) throws EvenementCivilException {
		return getPersonnePhysiqueOrThrowException(noIndividu, false);
	}

	/**
	 * @param noIndividu     un numéro d'individu
	 * @param doNotAutoFlush si vrai, les modifications courantes de la session hibernate ne sont pas flushées dans le base (évite d'incrémenter le numéro de version, mais si l'habitant vient d'être créé
	 *                       et n'existe pas encore en base, il ne sera pas trouvé).
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu.
	 * @throws EvenementCivilException si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected final PersonnePhysique getPersonnePhysiqueOrThrowException(long noIndividu, boolean doNotAutoFlush) throws EvenementCivilException {
		final PersonnePhysique habitant = getPersonnePhysiqueOrNull(noIndividu, doNotAutoFlush);
		if (habitant == null) {
			throw new EvenementCivilException(String.format("L'habitant avec le numéro d'individu = %d n'existe pas dans le registre.", noIndividu));
		}
		return habitant;
	}

	public TiersService getService() {
		return context.getTiersService();
	}

	/**
	 * Ferme les adresses temporaires du tiers, ou des tiers s'il s'agit d'un ménage commun.
	 *
	 * @param contribuable un contribuable
	 * @param date date de fermeture des adresses temporaires trouvées
	 */
	protected void fermeAdresseTiersTemporaire(Contribuable contribuable, RegDate date) {

		context.getTiersService().fermeAdresseTiersTemporaire(contribuable, date);

		// fermeture des adresses des tiers
		if (contribuable instanceof MenageCommun) {
			final Set<PersonnePhysique> pps = context.getTiersService().getPersonnesPhysiques((MenageCommun) contribuable);
			for (PersonnePhysique pp : pps) {
				fermeAdresseTiersTemporaire(pp, date);
			}
		}
	}

	/**
	 * Ouvre un nouveau for fiscal principal.
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau for.
	 * @param rattachement             le motif de rattachement du nouveau for
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @param modeImposition           le mode d'imposition du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipalPP openForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture,
	                                                      TypeAutoriteFiscale typeAutoriteFiscale, int numeroOfsAutoriteFiscale, MotifRattachement rattachement,
	                                                      MotifFor motifOuverture, ModeImposition modeImposition) {
		if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && motifOuverture == null) {
			throw new IllegalArgumentException("Le motif d'ouverture est obligatoire sur un for principal dans le canton");
		}
		return getService().openForFiscalPrincipal(contribuable, dateOuverture, rattachement, numeroOfsAutoriteFiscale, typeAutoriteFiscale, modeImposition, motifOuverture);
	}

	/**
	 * Ouvre un nouveau for fiscal principal déjà fermé.
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau for.
	 * @param rattachement             le motif de rattachement du nouveau for
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @param modeImposition           le mode d'imposition du for fiscal principal
	 * @param dateFermeture            la date à laquelle le nouveau for est fermé
	 * @param motifFermeture           le motif de fermeture du nouveau for
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipalPP openAndCloseForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture,
	                                                              TypeAutoriteFiscale typeAutoriteFiscale, int numeroOfsAutoriteFiscale, MotifRattachement rattachement,
	                                                              MotifFor motifOuverture, ModeImposition modeImposition, RegDate dateFermeture, MotifFor motifFermeture) {
		if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && motifOuverture == null) {
			throw new IllegalArgumentException("Le motif d'ouverture est obligatoire sur un for principal dans le canton");
		}
		else if (motifFermeture == null || dateFermeture == null) {
			throw new IllegalArgumentException("Le motif de fermeture et la date de fermeture sont tous deux obligatoires sur un for fiscal principal fermé");
		}
		return getService().openAndCloseForFiscalPrincipal(contribuable, dateOuverture, rattachement, numeroOfsAutoriteFiscale, typeAutoriteFiscale, modeImposition, motifOuverture, dateFermeture, motifFermeture);
	}

	/**
	 * Met-à-jour (= ferme l'ancien et ouvre un nouveau, ou annule et recrée selon les cas) le for fiscal principal d'un contribuable en fonction des valeurs spécifiées. Aucun changement n'est enregistré si les valeurs spécifiées sont identiques à celles du for fiscal principal courant.
	 *
	 * @param contribuable             le contribuable en question.
	 * @param dateChangement           la date de début de validité du nouveau for.
	 * @param typeAutorite             le type d'autorité fiscale
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale du nouveau for.
	 * @param motifRattachement        le motif de rattachement du nouveau for. Peut être <b>null</b> auquel cas le motif de rattachement de l'ancien for est utilisé.
	 * @param motifFermetureOuverture  le motif de fermeture du for existant et le motif d'ouverture du nouveau for
	 * @param modeImposition           le mode d'imposition du nouveau for. Peut être <b>null</b> auquel cas le mode d'imposition de l'ancien for est utilisé.
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipal updateForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable,
	                                                      final RegDate dateChangement,
	                                                      TypeAutoriteFiscale typeAutorite,
	                                                      int numeroOfsAutoriteFiscale,
	                                                      @Nullable MotifRattachement motifRattachement,
	                                                      MotifFor motifFermetureOuverture,
	                                                      @Nullable ModeImposition modeImposition) {

		ForFiscalPrincipalPP forFiscalPrincipal = contribuable.getForFiscalPrincipalAt(null);
		if (forFiscalPrincipal == null) {
			throw new IllegalArgumentException();
		}
		final Integer numeroOfsActuel = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();

		// On ne ferme et ouvre les fors que si nécessaire
		if (numeroOfsActuel == null || !numeroOfsActuel.equals(numeroOfsAutoriteFiscale) || typeAutorite != forFiscalPrincipal.getTypeAutoriteFiscale() ||
				(modeImposition != null && modeImposition != forFiscalPrincipal.getModeImposition())) {

			// on va fermer ou annuler le for principal courant
			if (dateChangement == forFiscalPrincipal.getDateDebut()) {
				// annulation
				forFiscalPrincipal.setAnnule(true);
				context.getEvenementFiscalService().publierEvenementFiscalAnnulationFor(forFiscalPrincipal);

				// s'il y avait un for principal avant, qui se fermait avec un motif différent du motif que l'on veut utiliser maintenant, il faut l'annuler
				// et le remplacer également
				final ForFiscalPrincipalPP forPrecedent = contribuable.getForFiscalPrincipalAt(forFiscalPrincipal.getDateDebut().getOneDayBefore());
				if (forPrecedent != null && forPrecedent.getMotifFermeture() != motifFermetureOuverture) {
					forPrecedent.setAnnule(true);
					context.getEvenementFiscalService().publierEvenementFiscalAnnulationFor(forPrecedent);
					openAndCloseForFiscalPrincipal(contribuable, forPrecedent.getDateDebut(), forPrecedent.getTypeAutoriteFiscale(),
					                               forPrecedent.getNumeroOfsAutoriteFiscale(), forPrecedent.getMotifRattachement(), forPrecedent.getMotifOuverture(),
					                               forPrecedent.getModeImposition(), forPrecedent.getDateFin(), motifFermetureOuverture);
				}
			}
			else {
				// fermeture
				closeForFiscalPrincipal(contribuable, dateChangement.getOneDayBefore(), motifFermetureOuverture);
			}

			if (modeImposition == null) {
				modeImposition = forFiscalPrincipal.getModeImposition();
			}
			if (motifRattachement == null) {
				motifRattachement = forFiscalPrincipal.getMotifRattachement();
			}
			forFiscalPrincipal = openForFiscalPrincipal(contribuable, dateChangement, typeAutorite, numeroOfsAutoriteFiscale, motifRattachement, motifFermetureOuverture, modeImposition);
		}
		return forFiscalPrincipal;
	}

	/**
	 * Ferme le for fiscal principal d'un contribuable.
	 * <p/>
	 * Note: cette méthode est définie à ce niveau par soucis de symétrie avec les méthodes openForFiscalPrincipal et updateForFiscalPrincipal.
	 *
	 * @param contribuable   le contribuable concerné
	 * @param dateFermeture  la date de fermeture du for
	 * @param motifFermeture le motif de fermeture du for
	 */
	protected void closeForFiscalPrincipal(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {
		context.getTiersService().closeForFiscalPrincipal(contribuable, dateFermeture, motifFermeture);
	}

	/**
	 * Ouvre un nouveau for fiscal principal.
	 * <p/>
	 * Cette méthode est une version spécialisée pour les événements fiscaux qui assume que: <ul> <li>le type d'autorité fiscale est toujours une commune vaudoise</li> <li>le motif de rattachement est
	 * toujours domicile/séjour</li> <li>le mode d'imposition est toujours ordinaire</li> </ul>
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipalPP openForFiscalPrincipalDomicileVaudoisOrdinaire(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture,
	                                                                            int numeroOfsAutoriteFiscale, MotifFor motifOuverture) {
		return openForFiscalPrincipal(contribuable, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsAutoriteFiscale, MotifRattachement.DOMICILE, motifOuverture,
				ModeImposition.ORDINAIRE);
	}

	public static void addValidationResults(EvenementCivilErreurCollector errors, EvenementCivilWarningCollector warnings, ValidationResults resultat) {
		if (resultat.hasErrors()) {
			for (String erreur : resultat.getErrors()) {
				errors.addErreur(erreur);
			}
		}
		if (resultat.hasWarnings()) {
			for (String warning : resultat.getWarnings()) {
				warnings.addWarning(warning);
			}
		}
	}

	protected void updateHabitantStatus(PersonnePhysique pp, RegDate date) throws EvenementCivilException {
		updateHabitantStatus(pp, getNoIndividu(), date);
	}

	protected void updateHabitantStatus(PersonnePhysique pp, long noIndividu, RegDate date) throws EvenementCivilException {
		try {
			context.getTiersService().updateHabitantStatus(pp, noIndividu, date, getNumeroEvenement());
		}
		catch (TiersException e) {
			throw new EvenementCivilException("Impossible de mettre à jour le flag 'habitant' de l'individu " + noIndividu, e);
		}
	}

	/**
	 * Permet de tester la presence d'une décision aci sur un contribuable qui peut être la personne concernée par l'évènement, son conjoint ou son ménage commun
	 * @param ctbToCheck le contribuable a vérifier
	 * @param ctbOfEvent le contribuable concerné par l'évenement civil , Peut être mis à  null si le contribuable concerné est le meme que celui qui est vérifié
	 * @param dateEvenement
	 * @throws EvenementCivilException
	 */
	protected void verifierPresenceDecisionEnCours(Contribuable ctbToCheck, @Nullable Contribuable ctbOfEvent, RegDate dateEvenement) throws EvenementCivilException {

		if (ctbToCheck != null) {
			//[SIFISC-12624]
			//Si une décision aci en cours est présente, on met l'évenement en erreur
			if (context.getTiersService().isSousInfluenceDecisions(ctbToCheck) || evenementIsAnterieurDecision(ctbToCheck, dateEvenement)) {
				String messageErreur = null;
				if (ctbOfEvent == null) {
					messageErreur = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
							FormatNumeroHelper.numeroCTBToDisplay(ctbToCheck.getNumero()));
				}
				else {
					if (ctbToCheck instanceof MenageCommun) {
						messageErreur = String.format("Le contribuable trouvé (%s) appartient à un ménage  (%s) qui fait l'objet d'une décision ACI",
								FormatNumeroHelper.numeroCTBToDisplay(ctbOfEvent.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(ctbToCheck.getNumero()));
					}
					else {
						// C'est le conjoint
						messageErreur = String.format("Le contribuable trouvé (%s) a un conjoint (%s) qui fait l'objet d'une décision ACI",
								FormatNumeroHelper.numeroCTBToDisplay(ctbOfEvent.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(ctbToCheck.getNumero()));
					}
				}
				throw new EvenementCivilException(messageErreur);
			}
		}
	}

	protected void verifierPresenceDecisionEnCours(Contribuable ctbToCheck, RegDate dateEvenement) throws EvenementCivilException {
		verifierPresenceDecisionEnCours(ctbToCheck,null,dateEvenement);
	}

	/**Detecte si une décision Aci est présente sur un couple d'une personne physique
	 *
	 * @param pp personne dont on veut vérifier les couples
	 * @throws EvenementCivilException levé si un couple trouvé possède une décision ACI
	 */
	protected void verifierPresenceDecisionsEnCoursSurCouple(PersonnePhysique pp) throws EvenementCivilException {
		final List<MenageCommun> allMenagesCommuns = context.getTiersService().getAllMenagesCommuns(pp);
		if (allMenagesCommuns != null) {
			for (MenageCommun mc : allMenagesCommuns) {
				verifierPresenceDecisionEnCours(mc,pp,getDate());
			}
		}
	}

	private boolean evenementIsAnterieurDecision(Contribuable c, RegDate date){
		final List<DecisionAci> decisions = c.getDecisionsSorted();
		if (!decisions.isEmpty()) {
			final DecisionAci firstDecision = decisions.get(0);
			return date.isBefore(firstDecision.getDateDebut());
		}
		return false;
	}
}
