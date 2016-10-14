package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class EtablissementValidator extends ContribuableValidator<Etablissement> {

	@Override
	public ValidationResults validate(Etablissement etb) {
		final ValidationResults vr = super.validate(etb);
		if (!etb.isAnnule()) {
			vr.merge(validateDomiciles(etb));
			vr.merge(validateLiensActiviteEconomique(etb));
		}
		return vr;
	}

	@Override
	protected ValidationResults validateTypeAdresses(Etablissement etb) {
		final ValidationResults results = new ValidationResults();

		final Set<AdresseTiers> adresses = etb.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdresseCivile) {
					results.addError(String.format("L'adresse de type 'personne civile' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur un établissement.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
			}
		}

		return results;
	}

	protected ValidationResults validateDomiciles(Etablissement etb) {
		final ValidationResults results = new ValidationResults();

		// on les valide un par un d'abord
		final List<DomicileEtablissement> domiciles = etb.getSortedDomiciles(false);        // il ne sert à rien de valider les domiciles annulés...
		for (DomicileEtablissement domicile : domiciles) {
			results.merge(getValidationService().validate(domicile));
		}

		// puis ensemble (ne doivent pas se chevaucher...)
		final MovingWindow<DomicileEtablissement> movingWindow = new MovingWindow<>(domiciles);
		while (movingWindow.hasNext()) {
			final MovingWindow.Snapshot<DomicileEtablissement> snapshot = movingWindow.next();
			final DomicileEtablissement current = snapshot.getCurrent();
			final DomicileEtablissement next = snapshot.getNext();
			if (current != null && next != null && DateRangeHelper.intersect(current, next)) {
				results.addError(String.format("Le domicile qui commence le %s chevauche le précédent", RegDateHelper.dateToDisplayString(next.getDateDebut())));
			}
		}

		return results;
	}

	/**
	 * Valide que, à tout moment, il n'y a au plus qu'une seule entité parente (au travers des liens d'activité économique)
	 * @param etb établissement à tester
	 * @return les résultats de la validation
	 */
	protected ValidationResults validateLiensActiviteEconomique(Etablissement etb) {
		final ValidationResults results = new ValidationResults();

		// collecte des rapports, calcul des intersections à la fin
		final Set<RapportEntreTiers> rapports = etb.getRapportsObjet();
		if (rapports != null && !rapports.isEmpty()) {
			final List<RapportEntreTiers> activitesEconomiques = new ArrayList<>(rapports.size());

			// collecte des liens d'activité économique non-annulés
			for (RapportEntreTiers ret : rapports) {
				if (ret.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE && !ret.isAnnule()) {
					activitesEconomiques.add(ret);
				}
			}

			// pas la peine de s'embêter plus loin s'il y a moins de deux rapports d'activité économique non-annulés
			if (activitesEconomiques.size() > 1) {
				// tri
				Collections.sort(activitesEconomiques, new DateRangeComparator<>());

				// calcul des intersections (deux à deux suffit, puisque la collection est triée)
				final MovingWindow<RapportEntreTiers> wnd = new MovingWindow<>(activitesEconomiques);
				while (wnd.hasNext()) {
					final MovingWindow.Snapshot<RapportEntreTiers> snap = wnd.next();
					final RapportEntreTiers current = snap.getCurrent();
					final RapportEntreTiers next = snap.getNext();
					if (current != null && next != null && DateRangeHelper.intersect(current, next)) {
						results.addError(String.format("Le lien d'activité économique qui commence le %s chevauche le précédent (%s)",
						                               RegDateHelper.dateToDisplayString(next.getDateDebut()),
						                               DateRangeHelper.toDisplayString(current)));
					}
				}
			}
		}

		return results;
	}

	@Override
	public Class<Etablissement> getValidatedClass() {
		return Etablissement.class;
	}
}
