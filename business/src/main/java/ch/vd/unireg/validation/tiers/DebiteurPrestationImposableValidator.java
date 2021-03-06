package ch.vd.unireg.validation.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseCivile;
import ch.vd.unireg.adresse.AdressePM;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Validateur de Débiteur de Prestation Imposable
 */
public class DebiteurPrestationImposableValidator extends TiersValidator<DebiteurPrestationImposable> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull DebiteurPrestationImposable dpi) {
		final ValidationResults vr = super.validate(dpi);
		if (!dpi.isAnnule()) {
			vr.merge(validatePeriodicites(dpi));
			vr.merge(validateLRCouverteParFor(dpi));
		}
		return vr;
	}

	private ValidationResults validatePeriodicites(DebiteurPrestationImposable dpi) {
		final ValidationResults results = new ValidationResults();
		final List<Periodicite> periodicites = dpi.getPeriodicitesSorted();
		if (periodicites.isEmpty()) {
			results.addWarning("ce débiteur n'a aucune périodicité");
		}
		else {

			final ValidationService validationService = getValidationService();

			// Les plages de validité des périodicités ne doivent pas se chevaucher
			Periodicite lastPeriodicite = null;
			for (Periodicite p : periodicites) {
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
				if (RegDateHelper.isBefore(premierForFiscal.getDateDebut(), premierePeriodicite.getDateDebut(), NullDateBehavior.EARLIEST)) {
					results.addError(String.format("Aucune périodicité n'est définie entre le début d'activité (%s) et la date de début de la première périodicité (%s)",
							RegDateHelper.dateToDisplayString(premierForFiscal.getDateDebut()), RegDateHelper.dateToDisplayString(premierePeriodicite.getDateDebut())));
				}
			}

		}
		return results;
	}

	private ValidationResults validateLRCouverteParFor(DebiteurPrestationImposable dpi) {
		final ValidationResults vr = new ValidationResults();
		final List<DeclarationImpotSource> lesLRs = dpi.getDeclarationsTriees(DeclarationImpotSource.class, false);
		if (!lesLRs.isEmpty()) {
			final List<ForFiscal> fors = dpi.getForsFiscauxNonAnnules(true);
			final List<DateRange> lrs = new ArrayList<>(lesLRs);
			final List<DateRange> periodeNonCouverte = DateRangeHelper.subtract(lrs, fors, new DateRangeAdapterCallback());
			if (!periodeNonCouverte.isEmpty()) {
				for (DateRange dateRange : periodeNonCouverte) {
					vr.addError(String.format("La période qui débute le (%s) et se termine le (%s) contient des LRs alors qu'elle n'est couverte par aucun for valide",
							RegDateHelper.dateToDisplayString(dateRange.getDateDebut()), RegDateHelper.dateToDisplayString(dateRange.getDateFin())));
				}
			}
		}
		return vr;
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
				vr.addError("Les for DPI hors Suisse ne sont pas autorisés.");
			}
		}

		// Seuls les for DPI sont autorisés
		for (ForFiscal f : fors.principauxPP) {
			vr.addError("Le for " + f + " n'est pas un type de for autorisé sur un débiteur de prestations imposables.");
		}
		for (ForFiscal f : fors.principauxPM) {
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

	@Override
	public Class<DebiteurPrestationImposable> getValidatedClass() {
		return DebiteurPrestationImposable.class;
	}
}
