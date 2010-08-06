package ch.vd.uniregctb.evenement;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
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
	private EnumAttributeIndividu[] parts;
	private ServiceCivilService serviceCivil;

	/**
	 * Initialise l'adresse principale et l'adresse courrier
	 *
	 * @param evenement             les données brutes de l'événement
	 * @param serviceCivil          le service civil
	 * @param infrastructureService le service infrastructure
	 * @param dataEventService      non null si le cache des individus doit être rafraîchi, null si ce n'est pas la peine
	 * @throws EvenementAdapterException si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */
	public void init(EvenementCivilData evenement, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService, DataEventService dataEventService) throws EvenementAdapterException {
		this.serviceCivil = serviceCivil;

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
		 * Récupération de l'année de l'événement (on se positionne a la
		 * veille de l'événement)
		 */
		final RegDate veille = date.getOneDayBefore();
		final int anneeEvenement = date.year();

		/*
		 * Récupération des informations sur l'individu depuis le host. En
		 * plus des états civils, on veut les adresses, le conjoint et les
		 * enfants
		 */
		anneeReference = veille.year();

		switch (this.type) {
		case NAISSANCE:
		case CORREC_FILIATION:
		case VEUVAGE:
		case DECES:
			anneeReference = anneeEvenement;
			break;
		}

		final Set<EnumAttributeIndividu> requiredParts = new HashSet<EnumAttributeIndividu>();
		if (evenement.getNumeroIndividuConjoint() != null || (dataEventService != null && forceRefreshCacheConjoint())) {
			requiredParts.add(EnumAttributeIndividu.CONJOINT);
		}
		fillRequiredParts(requiredParts);
		parts = requiredParts.toArray(new EnumAttributeIndividu[requiredParts.size()]);

		if (dataEventService != null) {

			// on doit d'abord invalider le cache de l'individu de l'événement afin que l'appel à getIndividu() soit pertinent
			dataEventService.onIndividuChange(noIndividu);

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
					dataEventService.onIndividuChange(noInd);
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
	protected void fillRequiredParts(Set<EnumAttributeIndividu> parts) {
	}

	public final TypeEvenementCivil getType() {
		return type;
	}

	public Long getNoIndividuConjoint() {
		return noIndividuConjoint;
	}

	public final Individu getConjoint() {
		if (conjoint == null && noIndividuConjoint != null) { // lazy init
			conjoint = serviceCivil.getIndividu(noIndividuConjoint, anneeReference);
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
			individuPrincipal = serviceCivil.getIndividu(noIndividu, anneeReference, parts);
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
