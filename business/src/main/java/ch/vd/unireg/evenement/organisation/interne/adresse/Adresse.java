package ch.vd.unireg.evenement.organisation.interne.adresse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.evenement.organisation.interne.HandleStatus;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2016-04-11
 */
public class Adresse extends EvenementEntrepriseInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(Adresse.class);
	private final RegDate dateApres;

	private final AdresseEffectiveRCEnt nouvelleAdresseEffective;
	private final AdresseLegaleRCEnt nouvelleAdresseLegale;

	private final AdresseLegaleRCEnt adresseLegaleApres;

	public Adresse(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	               EvenementEntrepriseContext context,
	               EvenementEntrepriseOptions options,
	               AdresseEffectiveRCEnt nouvelleAdresseEffective, AdresseLegaleRCEnt nouvelleAdresseLegale) {
		super(evenement, entrepriseCivile, entreprise, context, options);

		dateApres = evenement.getDateEvenement();

		this.nouvelleAdresseEffective = nouvelleAdresseEffective;
		this.nouvelleAdresseLegale = nouvelleAdresseLegale;

		this.adresseLegaleApres = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload().getDonneesRC().getAdresseLegale(dateApres);

	}

	@Override
	public String describe() {
		return "Changement d'adresse";
	}


	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		if (nouvelleAdresseEffective!= null) {
			traiteTransitionAdresseEffective(warnings, suivis, this.dateApres, adresseLegaleApres == null);
		}
		if (nouvelleAdresseLegale != null) {
			traiteTransitionAdresseLegale(warnings, suivis, this.dateApres);
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
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
