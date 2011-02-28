package ch.vd.uniregctb.evenement;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Implémentation des événement civils en provenance du host.
 */
public abstract class GenericEvenementAdapter implements EvenementCivil {

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
	 * @throws EvenementAdapterException si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */
	protected GenericEvenementAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
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
}
