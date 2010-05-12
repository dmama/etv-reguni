package ch.vd.uniregctb.evenement.tutelle;

import org.apache.log4j.Logger;

import ch.vd.registre.civil.model.EnumTypeTutelle;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Modélise un événement de mise ou levee de tutelle, curatelle ou conseil
 * légal.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class TutelleAdapter extends GenericEvenementAdapter implements Tutelle {

	protected static Logger LOGGER = Logger.getLogger(TutelleAdapter.class);

	/**
	 * L'Office du Tuteur General.
	 */
	private TuteurGeneral tuteurGeneral;

	/**
	 * Le tuteur de l'individu.
	 */
	private Individu tuteur = null;

	/**
	 * Le type de tutelle
	 */
	private TypeTutelle typeTutelle = null;

	/**
	 * @return Returns the tuteurGeneral.
	 */
	public final TuteurGeneral getTuteurGeneral() {
		return tuteurGeneral;
	}

	/**
	 * @return Returns the tuteur.
	 */
	public final Individu getTuteur() {
		return tuteur;
	}

	/**
	 * @param tuteur
	 *            The tuteur to set.
	 */
	public final void setTuteur(Individu tuteur) {
		this.tuteur = tuteur;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.tutelle.Tutelle#getTypeTutelle()
	 */
	public TypeTutelle getTypeTutelle() {
		return typeTutelle;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.GenericEvenementAdapter#init(ch.vd.uniregctb.evenement.EvenementCivilData,
	 *      ch.vd.registre.civil.service.ServiceCivil,
	 *      ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService)
	 */
	@Override
	public void init(EvenementCivilData evenementCivilData, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenementCivilData, serviceCivil, infrastructureService);

		/*
		 * Récupération de l'année de l'événement
		 */
		final int anneeEvenement = evenementCivilData.getDateEvenement().year();

		/*
		 * Récupération de la tutelle.
		 */
		ch.vd.uniregctb.interfaces.model.Tutelle tutelle = serviceCivil.getTutelle(getIndividu().getNoTechnique(), anneeEvenement);

		/*
		 * Initialisation du type de tutelle.
		 */
		final EnumTypeTutelle typeTutelleHost = tutelle.getTypeTutelle();
		if ( EnumTypeTutelle.TUTELLE.equals(typeTutelleHost) ) {
			this.typeTutelle = TypeTutelle.TUTELLE;
		}
		else if ( EnumTypeTutelle.CURATELLE.equals(typeTutelleHost) ) {
			this.typeTutelle = TypeTutelle.CURATELLE;
		}
		else if ( EnumTypeTutelle.CONSEIL_LEGAL.equals(typeTutelleHost) ) {
			this.typeTutelle = TypeTutelle.CONSEIL_LEGAL_CODE;
		}
		else {
			throw new EvenementAdapterException("Ce type de tutelle n'est pas pris en charge : " + typeTutelleHost);
		}

		/*
		 * Récupération du tuteur ou/et autorité tutellaire
		 */
		this.tuteurGeneral = tutelle.getTuteurGeneral();
		this.tuteur = tutelle.getTuteur();
	}

}
