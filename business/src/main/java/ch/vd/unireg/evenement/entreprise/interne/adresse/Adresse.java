package ch.vd.unireg.evenement.entreprise.interne.adresse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.evenement.entreprise.interne.HandleStatus;
import ch.vd.unireg.interfaces.entreprise.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
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
