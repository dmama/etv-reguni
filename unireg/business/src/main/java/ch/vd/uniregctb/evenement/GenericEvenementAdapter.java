package ch.vd.uniregctb.evenement;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.interfaces.model.Adresse;
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
	 * L'adresse principale de l'individu .
	 */
	private Adresse adressePrincipale;

	/**
	 * L'adresse secondaire de l'individu.
	 */
	private Adresse adresseSecondaire;

	/**
	 * L'adresse courrier de l'individu .
	 */
	private Adresse adresseCourrier;

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
	 * @param infrastructureService
	 * @param serviceCivil2
	 * @param evenementCivil2
	 * @throws EvenementAdapterException
	 */
	public void init(EvenementCivilRegroupe evenement, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {

		/* récupération des informations liés à l'événement civil regroupé */
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
		EnumAttributeIndividu[] attributs = null;
		int anneeReference = anneeVeille;

		switch (this.type) {
			case NAISSANCE :
			case CORREC_FILIATION :
				anneeReference = anneeEvenement;
				attributs =  new EnumAttributeIndividu[] {	EnumAttributeIndividu.ADRESSES,
															EnumAttributeIndividu.PARENTS };
				break;
			case NATIONALITE_SUISSE :
				anneeReference = anneeVeille;
				attributs =  new EnumAttributeIndividu[] {	EnumAttributeIndividu.ADRESSES,
															EnumAttributeIndividu.CONJOINT,
															EnumAttributeIndividu.PERMIS };
				break;
			default:
				anneeReference = anneeVeille;
				attributs =  new EnumAttributeIndividu[] {	EnumAttributeIndividu.ADRESSES,
															EnumAttributeIndividu.CONJOINT };
		}
		final long noIndividu = evenement.getNumeroIndividuPrincipal();
		this.individuPrincipal = serviceCivil.getIndividu(noIndividu, anneeReference, attributs);
		Assert.notNull(individuPrincipal, "L'individu principal est null");

		// Distinction adresse principale et adresse courrier
		//On recupère les adresses à la date de l'évènement plus 1 jour
		final AdressesCiviles adresses = serviceCivil.getAdresses(individuPrincipal.getNoTechnique(), date.getOneDayAfter());
		Assert.notNull(adresses, "L'individu principal n'a pas d'adresses valide");
		this.adressePrincipale = adresses.principale;
		this.adresseSecondaire = adresses.secondaire;
		this.adresseCourrier = adresses.courrier;

		/* Récupération des informations sur le conjoint */
		if (evenement.getNumeroIndividuConjoint() != null) {
			long noIndividuConjoint = evenement.getNumeroIndividuConjoint();
			this.conjoint = serviceCivil.getIndividu(noIndividuConjoint, anneeVeille);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.Evenement#getAdressePrincipale()
	 */
	public Adresse getAdressePrincipale() {
		return adressePrincipale;
	}

	public Adresse getAdresseSecondaire() {
		return adresseSecondaire;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.Evenement#getAdresseCourrier()
	 */
	public Adresse getAdresseCourrier() {
		return adresseCourrier;
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
