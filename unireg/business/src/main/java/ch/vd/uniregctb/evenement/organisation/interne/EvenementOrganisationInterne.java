package ch.vd.uniregctb.evenement.organisation.interne;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 *
 * // FIXME: Beaucoup de travail la dedans.
 * Implémentation des événements organisation en provenance du RCEnt.
 */
public abstract class EvenementOrganisationInterne {

//	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationInterne.class);

	private final long noOrganisation;
	private final Entreprise entreprise;
	private Organisation organisation;

	private final RegDate date;
	private final Long numeroEvenement;

	protected final EvenementOrganisationContext context;
	private final EvenementOrganisationOptions options;

	protected EvenementOrganisationInterne(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws EvenementOrganisationException {
		this.context = context;
		this.options = options;

		/* récupération des informations liés à l'événement */
		this.date = evenement.getDateEvenement();
		this.numeroEvenement = evenement.getId();
		this.noOrganisation = evenement.getNoOrganisation();
		this.organisation = organisation;
		this.entreprise = entreprise;
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
	public abstract HandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException;

	protected abstract void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException;

	protected void validateCommon(EvenementOrganisationErreurCollector erreurs) {

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

	public RegDate getDate() {
		return date;
	}

	public long getNoOrganisation() {
		return noOrganisation;
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	public Entreprise getEntreprise() {
		return entreprise;
	}

	/** // TODO: Checkthis
	 * Ouvre un nouveau for fiscal principal.
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param typeAutoriteFiscale      le type d'autorité fiscale.
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param rattachement             le motif de rattachement du nouveau for
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipalPM openForFiscalPrincipal(ContribuableImpositionPersonnesMorales contribuable, final RegDate dateOuverture,
	                                                    TypeAutoriteFiscale typeAutoriteFiscale, int numeroOfsAutoriteFiscale, MotifRattachement rattachement,
	                                                    MotifFor motifOuverture) {
		Assert.notNull(motifOuverture, "Le motif d'ouverture est obligatoire sur un for principal dans le canton");
		return context.getTiersService().openForFiscalPrincipal(contribuable, dateOuverture, rattachement, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifOuverture);
	}

}
