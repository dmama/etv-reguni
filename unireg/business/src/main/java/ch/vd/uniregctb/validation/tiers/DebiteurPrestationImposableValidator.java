package ch.vd.uniregctb.validation.tiers;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Validateur de Débiteur de Prestation Imposable
 */
public class DebiteurPrestationImposableValidator extends TiersValidator<DebiteurPrestationImposable> {

	@Override
	public ValidationResults validate(DebiteurPrestationImposable dpi) {
		final ValidationResults vr = super.validate(dpi);
		if (!dpi.isAnnule()) {
			validatePeriodicites(dpi);
		}
		return vr;
	}

	private ValidationResults validatePeriodicites(DebiteurPrestationImposable dpi) {
		final ValidationResults results = new ValidationResults();
		final List<Periodicite> periodicites = dpi.getPeriodicitesSorted();
		if (periodicites == null || periodicites.isEmpty()) {
			results.addWarning("ce débiteur n'a aucune périodicité");
		}
		else {

			final ValidationService validationService = getValidationService();

			// Les plages de validité des fors ne doivent pas se chevaucher
			Periodicite lastPeriodicite = null;
			for (Periodicite p : periodicites) {
				if (p.isAnnule()) {
					continue;
				}
				// on valide les périodicités pour elles-mêmes
				results.merge(validationService.validate(p));

				// on s'assure que les périodicités ne se chevauchent pas
				if (lastPeriodicite != null && DateRangeHelper.intersect(lastPeriodicite, p)) {
					results.addError("La périodicité qui commence le " + p.getDateDebut() + " et se termine le " + p.getDateFin() + " chevauche la périodicité précédente");
				}
				lastPeriodicite = p;
			}

			final ForDebiteurPrestationImposable premierForFiscal = dpi.getPremierForDebiteur();
			if (premierForFiscal != null) {
				final Periodicite premierePeriodicite = dpi.getPremierePeriodicite();
				if (premierForFiscal.getDateDebut().isBefore(premierePeriodicite.getDateDebut())) {
					results.addError(String.format("Aucune périodicité n'est définie entre le début d'activité (%s) et la date de début de la première périodicité (%s)",
							RegDateHelper.dateToDisplayString(premierForFiscal.getDateDebut()), RegDateHelper.dateToDisplayString(premierePeriodicite.getDateDebut())));
				}
			}

		}
		return results;
	}

	@Override
	protected ValidationResults validateFors(DebiteurPrestationImposable dpi) {

		final ValidationResults vr = super.validateFors(dpi);

		final ForsParType fors = dpi.getForsParType(true /* triés par ordre chronologique */);

		// Les plages de validité des fors ne doivent pas se chevaucher
		ForDebiteurPrestationImposable lastFor = null;
		for (ForDebiteurPrestationImposable fdpis : fors.dpis) {
			if (lastFor != null && DateRangeHelper.intersect(lastFor, fdpis)) {
				vr.addError(String.format("Le for DPI qui commence le %s et se termine le %s chevauche le for précédent",
						RegDateHelper.dateToDisplayString(fdpis.getDateDebut()), RegDateHelper.dateToDisplayString(fdpis.getDateFin())));
			}
			lastFor = fdpis;
			if (fdpis.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
				vr.addError("Les for DPI hors suisse ne sont pas autorisés.");
			}
		}

		// Seuls les for DPI sont autorisés
		for (ForFiscal f : fors.principaux) {
			vr.addError("Le for " + f + " n'est pas un type de for autorisé sur un débiteur de prestations imposables.");
		}
		for (ForFiscal f : fors.secondaires) {
			vr.addError("Le for " + f + " n'est pas un type de for autorisé sur un débiteur de prestations imposables.");
		}

		return vr;
	}

	@Override
	protected ValidationResults validateTypeAdresses(DebiteurPrestationImposable dpi) {
		final ValidationResults results = new ValidationResults();
		final Set<AdresseTiers> adresses = dpi.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdressePM) {
					results.addError(String.format("L'adresse de type 'personne morale' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur un débiteur de prestations imposables.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
				else if (a instanceof AdresseCivile) {
					results.addError(String.format("L'adresse de type 'personne civile' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur un débiteur de prestations imposables.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
			}
		}
		return results;
	}

	public Class<DebiteurPrestationImposable> getValidatedClass() {
		return DebiteurPrestationImposable.class;
	}
}
