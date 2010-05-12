package ch.vd.uniregctb.evenement;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Implémentation des événement civils en provenance du host.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public abstract class GenericEvenementAdapter implements EvenementCivil {

	/**
	 * L'individu principal.
	 */
	private Individu individuPrincipal;

	/**
	 * Le conjoint (mariage ou pacs).
	 */
	private Individu conjoint;

	/**
	 * Les enfants.
	 */
	//private List<Individu> enfants;

	private TypeEvenementCivil type;

	private RegDate date;

	private Long numeroEvenement;

	private Integer numeroOfsCommuneAnnonce;

	/**
	 * Initialise l'adresse principale et l'adresse courrier
	 * 
	 * @throws EvenementAdapterException
	 */
	public void init(EvenementCivilData evenement, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {

		/* récupération des informations liés à l'événement civil */
		this.type = evenement.getType();
		this.date = evenement.getDateEvenement();
		this.numeroEvenement = evenement.getId();
		this.numeroOfsCommuneAnnonce = evenement.getNumeroOfsCommuneAnnonce();

		/*
		 * Récupération de l'année de l'événement (on se positionne a la
		 * veille de l'événement)
		 */
		final RegDate veille = date.getOneDayBefore();
		final int anneeEvenement = date.year();
		final int anneeVeille = veille.year();

		/*
		 * Récupération des informations sur l'individu depuis le host. En
		 * plus des états civils, on veut les adresses, le conjoint et les
		 * enfants
		 */
		int anneeReference = anneeVeille;

		switch (this.type) {
			case NAISSANCE :
			case CORREC_FILIATION :
				anneeReference = anneeEvenement;
				break;
		}

		final Set<EnumAttributeIndividu> requiredParts = new HashSet<EnumAttributeIndividu>();
		if (evenement.getNumeroIndividuConjoint() != null) {
			requiredParts.add(EnumAttributeIndividu.CONJOINT);
		}
		fillRequiredParts(requiredParts);
		final EnumAttributeIndividu[] parts = requiredParts.toArray(new EnumAttributeIndividu[requiredParts.size()]);

		final long noIndividu = evenement.getNumeroIndividuPrincipal();
		this.individuPrincipal = serviceCivil.getIndividu(noIndividu, anneeReference, parts);
		Assert.notNull(individuPrincipal, "L'individu principal est null");

		/* Récupération des informations sur le conjoint */
		if (evenement.getNumeroIndividuConjoint() != null) {
			long noIndividuConjoint = evenement.getNumeroIndividuConjoint();
			this.conjoint = serviceCivil.getIndividu(noIndividuConjoint, anneeVeille);
		}
	}

	/**
	 * Doit-être implémenté par les classes dérivées pour savoir quelles parts
	 * demander au service civil pour l'individu pointé par l'événement civil
	 * @param parts ensemble à remplir
	 */
	protected void fillRequiredParts(Set<EnumAttributeIndividu> parts) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.Evenement#getCode()
	 */
	public final TypeEvenementCivil getType() {
		return type;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.Evenement#getConjoint()
	 */
	public final Individu getConjoint() {
		return conjoint;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.Evenement#getDate()
	 */
	public RegDate getDate() {
		return date;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.Evenement#getIndividu()
	 */
	public Individu getIndividu() {
		return individuPrincipal;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.Evenement#getNumeroEvenement()
	 */
	public final Long getNumeroEvenement() {
		return numeroEvenement;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
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
