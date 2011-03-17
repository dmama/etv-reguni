package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.validation.EntityValidatorImpl;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Classe de base des validateurs de tiers
 * @param <T>
 */
public abstract class TiersValidator<T extends Tiers> extends EntityValidatorImpl<T> {

	public ValidationResults validate(T tiers) {

		final ValidationResults results = new ValidationResults();

		// UNIREG-601 on ignore toutes les erreurs pour un tiers annulé
		if (!tiers.isAnnule()) {
			results.merge(validateAdresses(tiers));
			results.merge(validateFors(tiers));
			results.merge(validateDeclarations(tiers));
			results.merge(validateRapports(tiers));
		}

		return results;
	}

	protected ValidationResults validateRapports(T tiers) {
		// rien de spécial ici
		return new ValidationResults();
	}

	protected ValidationResults validateDeclarations(T tiers) {

		final ValidationService validationService = getValidationService();
		final ValidationResults results = new ValidationResults();

		final List<Declaration> decls = tiers.getDeclarationsSorted();
		if (decls != null) {
			Declaration last = null;
			for (Declaration d : decls) {
				if (d.isAnnule()) {
					continue;
				}
				// On valide la déclaration pour elle-même
				results.merge(validationService.validate(d));

				// Les plages de validité des déclarations ne doivent pas se chevaucher
				if (last != null && DateRangeHelper.intersect(last, d)) {
					final String message = String.format("La déclaration n°%d %s chevauche la déclaration précédente n°%d %s", d.getId(),
							DateRangeHelper.toString(d), last.getId(), DateRangeHelper.toString(last));
					results.addError(message);
				}
				last = d;
			}
		}

		return results;
	}

	protected ValidationResults validateAdresses(T tiers) {

		final ValidationResults results = new ValidationResults();

		results.merge(validateTypeAdresses(tiers));
		for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
			results.merge(validateAdresses(tiers.getAdressesTiersSorted(type)));
		}

		return results;
	}

	private ValidationResults validateAdresses(List<AdresseTiers> sorted) {

		if (sorted == null || sorted.size() == 0) {
			return null;
		}

		// on ignore les adresses annulées
		// [UNIREG-467] on crée une nouvelle liste pour avoir les indexes corrects
		List<AdresseTiers> list = new ArrayList<AdresseTiers>(sorted.size());
		for (AdresseTiers a : sorted) {
			if (!a.isAnnule()) {
				list.add(a);
			}
		}

		final ValidationResults results = new ValidationResults();
		final ValidationService validationService = getValidationService();

		RegDate lastDateFin = null;
		RegDate lastDateDebut = null;
		for (int i = 0; i < list.size(); i++) {
			final AdresseTiers adr = list.get(i);
			if (i > 0) {
				if (lastDateFin == null || adr.getDateDebut().isBeforeOrEqual(lastDateFin)) {
					// Overlap
					final String message =
							String.format("L'adresse fiscale numéro %d (type=%s début=%s fin=%s) chevauche l'adresse fiscale numéro %d (type=%s début=%s fin=%s)", i,
									adr.getUsage().name().toLowerCase(), RegDateHelper.dateToDisplayString(lastDateDebut), RegDateHelper.dateToDisplayString(lastDateFin), (i + 1),
									adr.getUsage().name().toLowerCase(), RegDateHelper.dateToDisplayString(adr.getDateDebut()), RegDateHelper.dateToDisplayString(adr.getDateFin()));
					results.addError(message);
				}
			}

			// validation de l'adresse pour elle-même
			results.merge(validationService.validate(adr));

			lastDateDebut = adr.getDateDebut();
			lastDateFin = adr.getDateFin();
		}

		return results;
	}

	protected abstract ValidationResults validateTypeAdresses(T tiers);

	protected ValidationResults validateFors(T tiers) {

		final ValidationResults results = new ValidationResults();

		// On valide tous les fors pour eux-mêmes
		final Set<ForFiscal> forsFiscaux = tiers.getForsFiscaux();
		if (forsFiscaux != null) {
			for (ForFiscal f : forsFiscaux) {
				results.merge(getValidationService().validate(f));
			}
		}

		return results;
	}
}
