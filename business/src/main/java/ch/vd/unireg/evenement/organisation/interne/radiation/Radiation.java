package ch.vd.uniregctb.evenement.organisation.interne.radiation;

import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
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
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class Radiation extends EvenementOrganisationInterneDeTraitement {

	private final RegDate dateApres;
	private final RegDate dateRadiation;

	protected Radiation(EvenementOrganisation evenement, Organisation organisation,
	                    Entreprise entreprise, EvenementOrganisationContext context,
	                    EvenementOrganisationOptions options,
	                    RegDate dateRadiation) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		this.dateRadiation = dateRadiation;

		dateApres = evenement.getDateEvenement();
	}

	@Override
	public String describe() {
		return "Radiation";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.RADIEE_RC, dateRadiation, suivis);
		if (isAssujettie(getEntreprise(), dateRadiation)) {
			warnings.addWarning("Vérification requise pour la radiation de l'entreprise encore assujettie.");
		}
		else if (isForPrincipalActif()) {
			warnings.addWarning(String.format("Vérification requise pour la radiation de l'entreprise encore dotée d'un for principal%s.", isForSecondaireActif() ? " ainsi que d'un ou plusieurs for secondaires" : ""));
		}
		else {
			suivis.addSuivi("L'entreprise a été radiée du registre du commerce.");
		}

		raiseStatusTo(HandleStatus.TRAITE);
	}

	private boolean isForSecondaireActif() {
		final Map<Integer, List<ForFiscalSecondaire>> forsFiscauxSecondairesParAutoriteeFiscale = getEntreprise().getForsFiscauxSecondairesActifsSortedMapped();
		if (forsFiscauxSecondairesParAutoriteeFiscale.isEmpty()) {
			return false;
		}
		else {
			long ct = forsFiscauxSecondairesParAutoriteeFiscale.entrySet().stream()
					.filter(e -> isActiveOrFuture(e.getValue(), dateRadiation))
					.map(Map.Entry::getKey)
					.count();
			return ct > 0;
		}
	}

	private boolean isForPrincipalActif() {
		final List<ForFiscalPrincipalPM> forsFiscauxPrincipaux = getEntreprise().getForsFiscauxPrincipauxActifsSorted();
		return !forsFiscauxPrincipaux.isEmpty() && isActiveOrFuture(forsFiscauxPrincipaux, dateRadiation);
	}

	private boolean isActiveOrFuture(List<? extends DateRange> ranges, RegDate date) {
		DateRange range = CollectionsUtils.getLastElement(ranges);
		return range != null && (range.getDateFin() == null || date.isBeforeOrEqual(range.getDateFin()));
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		Assert.isTrue(getOrganisation().isRadieeDuRC(dateApres), "L'organisation n'est pas radiée du RC!");
		Assert.notNull(dateRadiation, "Date de radiation introuvable!");

		final FormeLegale formeLegale = getOrganisation().getFormeLegale(dateApres);
		Assert.isTrue(formeLegale != FormeLegale.N_0109_ASSOCIATION && formeLegale != FormeLegale.N_0110_FONDATION, String.format("Mauvais type d'entreprise: %s (erreur de programmation).", formeLegale.getLibelle()));
	}
}
