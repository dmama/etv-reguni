package ch.vd.uniregctb.evenement.civil.interne;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Implémentation des événement civils en provenance du host.
 */
public abstract class EvenementCivilInterneBase implements EvenementCivilInterne {

	// L'individu principal.
	private Long noIndividu;
	private Long principalPPId;
	private Individu individuPrincipal;

	// Le conjoint (mariage ou pacs).
	private Long noIndividuConjoint;
	private Long conjointPPId;
	private Individu conjoint;

	private TypeEvenementCivil type;
	private RegDate date;
	private Long numeroEvenement;
	private Integer numeroOfsCommuneAnnonce;

	// Info pour initialiser les individus de manière lazy
	private int anneeReference;
	private AttributeIndividu[] parts;
	protected EvenementCivilContext context;

	/**
	 * Construit un événement civil interne sur la base d'un événement civil externe.
	 *
	 * @param evenement un événement civil externe
	 * @param context   le context d'exécution de l'événement
	 * @throws EvenementCivilInterneException si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */
	protected EvenementCivilInterneBase(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		this.context = context;

		/* récupération des informations liés à l'événement civil */
		this.type = evenement.getType();
		this.date = evenement.getDateEvenement();
		this.numeroEvenement = evenement.getId();
		this.numeroOfsCommuneAnnonce = evenement.getNumeroOfsCommuneAnnonce();
		this.noIndividu = evenement.getNumeroIndividuPrincipal();
		this.principalPPId = evenement.getHabitantPrincipalId();
		this.noIndividuConjoint = evenement.getNumeroIndividuConjoint();
		this.conjointPPId = evenement.getHabitantConjointId();

		/*
		 * Récupération de l'année de l'événement (on s'intéresse à tout ce qui s'est passé avant)
		 */
		anneeReference = date.year();

		/*
		 * Récupération des informations sur l'individu depuis le host. En plus des états civils, on peut vouloir les adresses, le conjoint,
		 * les enfants... (enfin, chaque adapteur d'événement sait ce dont il a besoin en plus...)
		 */
		final Set<AttributeIndividu> requiredParts = new HashSet<AttributeIndividu>();
		if (evenement.getNumeroIndividuConjoint() != null || (context.isRefreshCache() && forceRefreshCacheConjoint())) {
			requiredParts.add(AttributeIndividu.CONJOINT);
		}
		fillRequiredParts(requiredParts);
		parts = requiredParts.toArray(new AttributeIndividu[requiredParts.size()]);

		if (context.isRefreshCache()) {

			// on doit d'abord invalider le cache de l'individu de l'événement afin que l'appel à getIndividu() soit pertinent
			context.getDataEventService().onIndividuChange(noIndividu);

			// si demandé par le type d'événement, le cache des invididus conjoints doit être rafraîchi lui-aussi
			if (forceRefreshCacheConjoint()) {

				// récupération du numéro de l'individu conjoint (en fait, on va prendre tous les conjoints connus)
				final Set<Long> conjoints = new HashSet<Long>();
				final Individu individu = getIndividu();
				for (EtatCivil etatCivil : individu.getEtatsCivils()) {
					final Long numeroConjoint = etatCivil.getNumeroConjoint();
					if (numeroConjoint != null) {
						conjoints.add(numeroConjoint);
					}
				}

				// nettoyage du cache pour tous ces individus
				for (Long noInd : conjoints) {
					context.getDataEventService().onIndividuChange(noInd);
				}
			}
		}
	}
	
	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected EvenementCivilInterneBase(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, TypeEvenementCivil typeEvenementCivil, RegDate dateEvenement,
	                                    Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		this.context = context;

		this.individuPrincipal = individu;
		this.noIndividu = (individu == null ? null : individu.getNoTechnique());
		this.principalPPId = principalPPId;

		this.conjoint = conjoint;
		this.noIndividuConjoint = (conjoint == null ? null : conjoint.getNoTechnique());
		this.conjointPPId = conjointPPId;

		this.type = typeEvenementCivil;
		this.date = dateEvenement;
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected EvenementCivilInterneBase(Individu individu, Individu conjoint, TypeEvenementCivil typeEvenementCivil, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce,
	                                 EvenementCivilContext context) {
		this.context = context;

		this.individuPrincipal = individu;
		this.noIndividu = (individu == null ? null : individu.getNoTechnique());
		this.principalPPId = (individu == null ? null : context.getTiersDAO().getNumeroPPByNumeroIndividu(individu.getNoTechnique(), true));

		this.conjoint = conjoint;
		this.noIndividuConjoint = (conjoint == null ? null : conjoint.getNoTechnique());
		this.conjointPPId = (conjoint == null ? null : context.getTiersDAO().getNumeroPPByNumeroIndividu(conjoint.getNoTechnique(), true));

		this.type = typeEvenementCivil;
		this.date = dateEvenement;
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public final void validate(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		validateCommon(erreurs);
		if (erreurs.isEmpty()) {
			validateSpecific(erreurs, warnings);
		}
	}

	/**
	 * Validation commune l'objet target passé en paramètre.
	 *
	 * @param erreurs les éventuelles erreurs trouvées (out)
	 * @param warnings les éventuels warnings trouvés (out)
	 */
	protected abstract void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings);

	private void validateCommon(List<EvenementCivilExterneErreur> erreurs) {

		/*
		 * Vérifie que les éléments de base sont renseignés
		 */
		if (getDate() == null) {
			erreurs.add(new EvenementCivilExterneErreur("L'événement n'est pas daté"));
			return;
		}

		if (getNumeroOfsCommuneAnnonce() == null) {
			erreurs.add(new EvenementCivilExterneErreur("La commune d'annonce n'est pas renseignée"));
			return;
		}

		/*
		 * La date de l'événement se situe dans le futur.
		 */
		if (getDate().isAfter(RegDate.get())) {
			erreurs.add(new EvenementCivilExterneErreur("La date de l'événement est dans le futur"));
		}

		/*
		 * Cas particulier de l'arrivée ou de la naissance : le ou les contribuables ne sont en principe pas présents.
		 */
		if (isContribuableObligatoirementConnuAvantTraitement()) {
			/*
			 * Il n’existe pas de tiers contribuable correspondant à l’individu, assujetti ou non (mineur, conjoint) correspondant à
			 * l’individu.
			 */
			if (getPrincipalPPId() == null) {
				erreurs.add(new EvenementCivilExterneErreur("Aucun tiers contribuable ne correspond au numero d'individu " + getNoIndividu()));
			}

			/*
			 * Il n’existe pas de tiers contribuable correspondant au conjoint, assujetti ou non (mineur, conjoint) correspondant à
			 * l’individu.
			 */
			if (getNoIndividuConjoint() != null && getConjointPPId() == null) {
				erreurs.add(new EvenementCivilExterneErreur("Aucun tiers contribuable ne correspond au numero d'individu du conjoint " + getNoIndividuConjoint()));
			}
		}

		// en tout cas, l'individu devrait exister dans le registre civil !
		final Individu individu = getIndividu();
		if (individu == null) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu est introuvable dans le registre civil!"));
		}
	}

	protected boolean isContribuableObligatoirementConnuAvantTraitement() {
		return isContribuablePresentBefore();
	}

	protected boolean forceRefreshCacheConjoint() {
		return false;
	}

	/**
	 * Doit-être implémenté par les classes dérivées pour savoir quelles parts demander au service civil pour l'individu pointé par l'événement civil
	 *
	 * @param parts ensemble à remplir
	 */
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
	}

	public final TypeEvenementCivil getType() {
		return type;
	}

	public Long getNoIndividuConjoint() {
		return noIndividuConjoint;
	}

	public final Individu getConjoint() {
		if (conjoint == null && noIndividuConjoint != null) { // lazy init
			conjoint = context.getServiceCivil().getIndividu(noIndividuConjoint, anneeReference);
		}
		return conjoint;
	}

	public Long getConjointPPId() {
		return conjointPPId;
	}

	public RegDate getDate() {
		return date;
	}

	public Long getNoIndividu() {
		return noIndividu;
	}

	public Individu getIndividu() {
		if (individuPrincipal == null && noIndividu != null) { // lazy init
			individuPrincipal = context.getServiceCivil().getIndividu(noIndividu, anneeReference, parts);
		}
		return individuPrincipal;
	}

	public Long getPrincipalPPId() {
		return principalPPId;
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

	/**
	 * @param noIndividu le numéro d'individu
	 * @param errors     la collection des erreurs qui sera remplie automatiquement si l'habitant n'existe pas
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu, ou <b>null<b> si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected PersonnePhysique getPersonnePhysiqueOrFillErrors(Long noIndividu, List<EvenementCivilExterneErreur> errors) {
		final PersonnePhysique habitant = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(noIndividu);
		if (habitant == null) {
			errors.add(new EvenementCivilExterneErreur("L'habitant avec le numéro d'individu = " + noIndividu + " n'existe pas dans le registre."));
		}
		return habitant;
	}

	/**
	 * @param noIndividu un numéro d'individu
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu.
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException
	 *          si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected PersonnePhysique getPersonnePhysiqueOrThrowException(Long noIndividu) throws EvenementCivilHandlerException {
		return getPersonnePhysiqueOrThrowException(noIndividu, false);
	}

	/**
	 * @param noIndividu     un numéro d'individu
	 * @param doNotAutoFlush si vrai, les modifications courantes de la session hibernate ne sont pas flushées dans le base (évite d'incrémenter le numéro de version, mais si l'habitant vient d'être créé
	 *                       et n'existe pas encore en base, il ne sera pas trouvé).
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu.
	 * @throws EvenementCivilHandlerException si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected PersonnePhysique getPersonnePhysiqueOrThrowException(Long noIndividu, boolean doNotAutoFlush) throws EvenementCivilHandlerException {
		final PersonnePhysique habitant = context.getTiersDAO().getPPByNumeroIndividu(noIndividu, doNotAutoFlush);
		if (habitant == null) {
			throw new EvenementCivilHandlerException("L'habitant avec le numéro d'individu = " + noIndividu
					+ " n'existe pas dans le registre.");
		}
		return habitant;
	}

	public TiersService getService() {
		return context.getTiersService();
	}
}
