package ch.vd.uniregctb.evenement.organisation.interne.adresse;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeAdresseTiers;

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
			final AdresseSupplementaire adresseCourrier = getAdresseTiers(TypeAdresseTiers.COURRIER, dateApres);
			if (adresseCourrier != null) {
				if (adresseCourrier.isPermanente()) {
					warnings.addWarning("L'adresse fiscale de courrier, permanente, est maintenue malgré le changement de l'adresse effective civile (issue de l'IDE).");
				} else {
					getContext().getAdresseService().fermerAdresse(adresseCourrier, dateApres.getOneDayBefore());
					suivis.addSuivi("L'adresse fiscale de courrier, non-permanente, a été fermée. L'adresse de courrier est maintenant donnée par l'adresse effective civile (issue de l'IDE).");
				}
			} else {
				suivis.addSuivi("L'adresse de courrier a changé suite au changement de l'adresse effective civile (issue de l'IDE).");
			}
			final AdresseSupplementaire adresseRepresentation = getAdresseTiers(TypeAdresseTiers.REPRESENTATION, dateApres);
			if (adresseRepresentation != null) {
				if (adresseRepresentation.isPermanente()) {
					warnings.addWarning("L'adresse fiscale de représentation, permanente, est maintenue malgré le changement de l'adresse effective civile (issue de l'IDE).");
				} else {
					getContext().getAdresseService().fermerAdresse(adresseRepresentation, dateApres.getOneDayBefore());
					suivis.addSuivi("L'adresse fiscale de représentation, non-permanente, a été fermée. L'adresse de représentation est maintenant donnée par l'adresse effective civile (issue de l'IDE).");
				}
			} else {
				suivis.addSuivi("L'adresse de représentation a changé suite au changement de l'adresse effective civile (issue de l'IDE).");
			}
			final AdresseSupplementaire adressePoursuite = getAdresseTiers(TypeAdresseTiers.POURSUITE, dateApres);
			if (adresseLegaleApres == null) {
				if (adressePoursuite != null) {
					if (adressePoursuite.isPermanente()) {
						warnings.addWarning("L'adresse fiscale poursuite, permanente, est maintenue malgré le changement de l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).");
					}
					else {
						getContext().getAdresseService().fermerAdresse(adressePoursuite, dateApres.getOneDayBefore());
						suivis.addSuivi("L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).");
					}
				}
				else {
					suivis.addSuivi("L'adresse de poursuite a changé suite au changement de l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).");
				}
			}
		}
		if (nouvelleAdresseLegale != null) {
			final AdresseSupplementaire adressePoursuite = getAdresseTiers(TypeAdresseTiers.POURSUITE, dateApres);
			if (adressePoursuite != null) {
				if (adressePoursuite.isPermanente()) {
					warnings.addWarning("L'adresse fiscale de poursuite, permanente, est maintenue malgré le changement de l'adresse légale civile (issue du RC).");
				} else {
					getContext().getAdresseService().fermerAdresse(adressePoursuite, dateApres.getOneDayBefore());
					suivis.addSuivi("L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse légale civile (issue du RC).");
				}
			} else {
				suivis.addSuivi("L'adresse de poursuite a changé suite au changement de l'adresse légale civile (issue du RC).");
			}
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Nullable
	private AdresseSupplementaire getAdresseTiers(TypeAdresseTiers type, RegDate date) throws EvenementOrganisationException {
		final List<AdresseTiers> adressesTiersSorted = AnnulableHelper.sansElementsAnnules(getEntreprise().getAdressesTiersSorted(type));
		if (adressesTiersSorted.isEmpty()) {
			return null;
		}
		final AdresseTiers adresseTiers = CollectionsUtils.getLastElement(adressesTiersSorted);
		if (adresseTiers != null) {
			if (adresseTiers.getDateFin() != null) {
				return null;
			}
			if (adresseTiers.getDateDebut().isAfter(date)) {
				throw new EvenementOrganisationException(String.format("L'adresse valide à la date demandée %s n'est pas la dernière de l'historique!", RegDateHelper.dateToDisplayString(date)));
			}
			if (adresseTiers instanceof AdresseSupplementaire) {
				return (AdresseSupplementaire) adresseTiers;
			}
			throw new EvenementOrganisationException(String.format("l'adresse %s trouvée n'est pas de type AdresseSupplementaire! Elle est de type %s", type, adresseTiers.getClass()));
		}
		return null;
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		/*
		 Erreurs techniques fatale
		  */
		Assert.notNull(dateApres);

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		Assert.notNull(getEntreprise());
	}
}
