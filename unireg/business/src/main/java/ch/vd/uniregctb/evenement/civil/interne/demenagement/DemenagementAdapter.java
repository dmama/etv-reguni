package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneAvecAdressesBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Modélise un événement de déménagement.
 *
 * @author Ludovic Bertin </a>
 */
public class DemenagementAdapter extends EvenementCivilInterneAvecAdressesBase implements Demenagement {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(DemenagementAdapter.class);

	/**
	 * L'adresse de départ.
	 */
	private Adresse ancienneAdressePrincipale;

	/**
	 * La commune de la nouvelle adresse principale.
	 */
	private CommuneSimple nouvelleCommunePrincipale;

	private DemenagementHandler handler;

	protected DemenagementAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, DemenagementHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
		this.handler = handler;

		// il faut récupérer les adresses actuelles, ce seront les nouvelles
		// adresses

		// Distinction adresse principale et adresse courrier
		final AdressesCiviles adresses;
		try {
			adresses = new AdressesCiviles(context.getServiceCivil().getAdresses(super.getNoIndividu(), evenement.getDateEvenement().getOneDayBefore(), false));
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilInterneException(e);
		}
		this.ancienneAdressePrincipale = adresses.principale;

		// on recupere la commune de la nouvelle adresse
		try {
			this.nouvelleCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(getNouvelleAdressePrincipale());
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilInterneException(e);
		}
	}

	/**
	 * @see ch.vd.uniregctb.evenement.civil.interne.demenagement.Demenagement#getNouvelleCommuneAdressePrincipale()
	 */
	public CommuneSimple getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.civil.interne.demenagement.Demenagement#getNouvelleAdresseCourrier()
	 */
	public Adresse getNouvelleAdresseCourrier() {
		return getAdresseCourrier();
	}

	/**
	 * @see ch.vd.uniregctb.evenement.civil.interne.demenagement.Demenagement#getNouvelleAdressePrincipale()
	 */
	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
