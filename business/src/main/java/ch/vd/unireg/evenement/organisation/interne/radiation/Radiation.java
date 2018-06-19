package ch.vd.unireg.evenement.organisation.interne.radiation;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.evenement.organisation.interne.HandleStatus;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.TypeEtatEntreprise;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class Radiation extends EvenementEntrepriseInterneDeTraitement {

	private final RegDate dateApres;
	private final RegDate dateRadiation;

	protected Radiation(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                    Entreprise entreprise, EvenementEntrepriseContext context,
	                    EvenementEntrepriseOptions options,
	                    RegDate dateRadiation) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);

		this.dateRadiation = dateRadiation;

		dateApres = evenement.getDateEvenement();
	}

	@Override
	public String describe() {
		return "Radiation";
	}


	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

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
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		if (!getEntrepriseCivile().isRadieeDuRC(dateApres)) {
			throw new IllegalArgumentException("L'entreprise n'est pas radiée du RC!");
		}
		if (dateRadiation == null) {
			throw new IllegalArgumentException("Date de radiation introuvable!");
		}

		final FormeLegale formeLegale = getEntrepriseCivile().getFormeLegale(dateApres);
		if (formeLegale == FormeLegale.N_0109_ASSOCIATION || formeLegale == FormeLegale.N_0110_FONDATION) {
			throw new IllegalArgumentException(String.format("Mauvais type d'entreprise: %s (erreur de programmation).", formeLegale.getLibelle()));
		}
	}
}
