package ch.vd.uniregctb.evenement.depart;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.evenement.EvenementAdapterAvecAdresses;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Modélise un événement de depart.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class DepartAdapter extends EvenementAdapterAvecAdresses implements Depart {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(DepartAdapter.class);


	/**
	 * l'adresse principale avant départ
	 */
	private Adresse ancienneAdressePrincipale;

	/**
	 * La commune de la nouvelle adresse principale.
	 */
	private CommuneSimple nouvelleCommunePrincipale;

	/**
	 * La commune de l'adresse principale avant départ
	 */
	private CommuneSimple ancienneCommunePrincipale;

	/**
	 * l'adresse secondaire avant départ
	 */
	private Adresse ancienneAdresseSecondaire;
	/**
	 * La commune de l'adresse secondaire avant départ
	 */
	private CommuneSimple ancienneCommuneSecondaire;

	/**
	 * adresse courrier avant départ
	 */
	private Adresse ancienneAdresseCourrier;

	private Pays paysInconnu;

	/**
	 * Indique si l'evenement est un ancien type de départ
	 */
	private boolean isAncienTypeDepart = false;

	private DepartHandler handler;

	protected DepartAdapter(EvenementCivilData evenement, EvenementCivilContext context, DepartHandler handler) throws EvenementAdapterException {
		super(evenement, context);
		this.handler = handler;

		if (evenement.getNumeroIndividuConjoint()!=null) {
			isAncienTypeDepart = true;
		}

		// on récupère les anciennes adresses (= à la date d'événement)
		final AdressesCiviles adresses;
		try {
			adresses = new AdressesCiviles(context.getServiceCivil().getAdresses(super.getNoIndividu(), evenement.getDateEvenement(), false));
		}
		catch (DonneesCivilesException e) {
			throw new EvenementAdapterException(e);
		}

		this.ancienneAdressePrincipale = adresses.principale;
		this.ancienneAdresseCourrier = adresses.courrier;
		this.ancienneAdresseSecondaire=adresses.secondaire;

		try {
			// on récupère la commune de l'adresse principale avant le départ
			this.ancienneCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(ancienneAdressePrincipale);

			// on récupère la commune de l'adresse principale avant le départ
			this.ancienneCommuneSecondaire = context.getServiceInfra().getCommuneByAdresse(ancienneAdresseSecondaire);

			// on récupère la commune de la nouvelle adresse principal
			this.nouvelleCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(getNouvelleAdressePrincipale());

			this.paysInconnu = context.getServiceInfra().getPaysInconnu();
		}
		catch (InfrastructureException e) {
			throw new EvenementAdapterException(e);
		}
	}

	/**
	 * @see ch.vd.uniregctb.evenement.depart.Depart#getNouvelleAdresseCourrier()
	 */
	public Adresse getNouvelleAdresseCourrier() {
		return getAdresseCourrier();
	}

	/**
	 * @see ch.vd.uniregctb.evenement.depart.Depart#getNouvelleAdressePrincipale()
	 */
	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public CommuneSimple getNouvelleCommunePrincipale() {
		return this.nouvelleCommunePrincipale;
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public CommuneSimple getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}

	public Adresse getAncienneAdresseCourrier() {
		return ancienneAdresseCourrier;
	}

	public Adresse getAncienneAdresseSecondaire() {
		return ancienneAdresseSecondaire;
	}

	public CommuneSimple getAncienneCommuneSecondaire() {
		return ancienneCommuneSecondaire;
	}

	public Pays getPaysInconnu() {
		return paysInconnu;
	}
	public boolean isAncienTypeDepart() {

		return isAncienTypeDepart;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
