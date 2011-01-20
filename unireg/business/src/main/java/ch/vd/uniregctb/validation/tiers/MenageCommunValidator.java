package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class MenageCommunValidator extends ContribuableValidator<MenageCommun> {

	@Override
	protected ValidationResults validateRapports(MenageCommun mc) {
		final ValidationResults vr = super.validateRapports(mc);

		// vérifie que le ménage commun ne comporte au plus que 2 personnes physiques distinctes
		final Set<RapportEntreTiers> rapports = mc.getRapportsObjet();
		if (rapports != null) {
		    final Set<Long> idComposants = new HashSet<Long>(4);
		    for (RapportEntreTiers r : rapports) {
		        if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType()) {
		            final Long id = r.getSujetId();
		            if (id != null) {
		                idComposants.add(id);
		            }
		        }
		    }
		    if (idComposants.size() > 2) {
		        vr.addError("Le ménage commun est lié avec plus de 2 personnes physiques distinctes [n°="
		                + ArrayUtils.toString(idComposants.toArray()) + "]");
		    }
		}

		return vr;
	}

	@Override
	protected ValidationResults validateFors(MenageCommun mc) {
		final ValidationResults vr = super.validateFors(mc);

		/*
		 * On n'autorise la présence de fors que durant la ou les périodes de validité du couple.
		 */
		// Détermine les périodes de validités ininterrompues du ménage commun
		final List<RapportEntreTiers> rapportsMenages = new ArrayList<RapportEntreTiers>();
		final Set<RapportEntreTiers> rapports = mc.getRapportsObjet();
		if (rapports != null) {
		    for (RapportEntreTiers r : rapports) {
		        if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType()) {
		            rapportsMenages.add(r);
		        }
		    }
		}
		Collections.sort(rapportsMenages, new DateRangeComparator<RapportEntreTiers>());
		final List<DateRange> periodes = DateRangeHelper.collateRange(rapportsMenages);

		// Vérifie que chaque for est entièrement défini à l'intérieur d'une période de validité
		final Set<ForFiscal> fors = mc.getForsFiscaux();
		if (fors != null) {
		    for (ForFiscal f : fors) {
		        if (f.isAnnule()) {
		            continue;
		        }
		        DateRange intersection = DateRangeHelper.intersection(f, periodes);
		        if (intersection == null || !DateRangeHelper.equals(f, intersection)) {
		            vr.addError("Le for fiscal [" + f
		                    + "] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [" + mc.getNumero() + "]");
		        }
		    }
		}

		return vr;
	}

	@Override
	protected ValidationResults validateTypeAdresses(MenageCommun mc) {
		final ValidationResults results = new ValidationResults();

		final Set<AdresseTiers> adresses = mc.getAdressesTiers();
		if (adresses != null) {
		    for (AdresseTiers a : adresses) {
		        if (a.isAnnule()) {
		            continue;
		        }
		        if (a instanceof AdressePM) {
		            results.addError(String.format("L'adresse de type 'personne morale' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur un ménage commun.",
				            a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
		        } else if (a instanceof AdresseCivile) {
		            results.addError(String.format("L'adresse de type 'personne civile' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur un ménage commun.",
				            a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
		        }
		    }
		}
		return results;
	}

	public Class<MenageCommun> getValidatedClass() {
		return MenageCommun.class;
	}
}
