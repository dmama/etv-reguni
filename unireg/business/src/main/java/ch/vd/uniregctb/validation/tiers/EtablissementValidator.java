package ch.vd.uniregctb.validation.tiers;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Etablissement;

public class EtablissementValidator extends ContribuableValidator<Etablissement> {

	@Override
	public ValidationResults validate(Etablissement etb) {
		final ValidationResults vr = super.validate(etb);
		if (!etb.isAnnule()) {
			vr.merge(validateDomiciles(etb));
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

	@Override
	public Class<Etablissement> getValidatedClass() {
		return Etablissement.class;
	}
}
