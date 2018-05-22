package ch.vd.unireg.evenement.organisation.interne.adresse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.unireg.evenement.organisation.interne.HandleStatus;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2016-04-11
 */
public class Adresse extends EvenementOrganisationInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(Adresse.class);
	private final RegDate dateApres;

	private final AdresseEffectiveRCEnt nouvelleAdresseEffective;
	private final AdresseLegaleRCEnt nouvelleAdresseLegale;

	private final AdresseLegaleRCEnt adresseLegaleApres;

	public Adresse(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	               EvenementOrganisationContext context,
	               EvenementOrganisationOptions options,
	               AdresseEffectiveRCEnt nouvelleAdresseEffective, AdresseLegaleRCEnt nouvelleAdresseLegale) {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();

		this.nouvelleAdresseEffective = nouvelleAdresseEffective;
		this.nouvelleAdresseLegale = nouvelleAdresseLegale;

		this.adresseLegaleApres = organisation.getSitePrincipal(dateApres).getPayload().getDonneesRC().getAdresseLegale(dateApres);

	}

	@Override
	public String describe() {
		return "Changement d'adresse";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		if (nouvelleAdresseEffective!= null) {
			traiteTransitionAdresseEffective(warnings, suivis, this.dateApres, adresseLegaleApres == null);
		}
		if (nouvelleAdresseLegale != null) {
			traiteTransitionAdresseLegale(warnings, suivis, this.dateApres);
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		/*
		 Erreurs techniques fatale
		  */
		if (dateApres == null) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		if (getEntreprise() == null) {
			throw new IllegalArgumentException();
		}
	}
}
