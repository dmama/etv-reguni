package ch.vd.uniregctb.role;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;

public class RolesPM extends Roles<InfoContribuablePM, InfoCommunePM, Entreprise> {

	@Override
	public void digestInfoFor(InfoFor infoFor, Entreprise entreprise, Assujettissement assujettissement, RegDate dateFinAssujettissementPrecedent, int annee, int noOfsCommune, AdresseService adresseService, TiersService tiersService) {
		// l'entreprise peut, exceptionnellement, mais quand-même, faire plusieurs bouclements dans l'année...
		// (auquel cas il doit y avoir plusieurs lignes dans le rapport)
		final InfoCommunePM infoCommune = getOrCreateInfoCommune(noOfsCommune);
		final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
		for (ExerciceCommercial exercice : exercices) {
			final RegDate dateFinExercice = exercice.getDateFin();
			if (dateFinExercice.year() == annee && dateFinExercice.isAfter(dateFinAssujettissementPrecedent)) {
				final DateRange periodeCritique = assujettissement != null ? assujettissement : infoFor;
				if (DateRangeHelper.intersect(exercice, periodeCritique)) {
					final InfoContribuable infoCtb = infoCommune.getOrCreateInfoPourContribuable(entreprise, dateFinExercice, adresseService, tiersService);
					infoCtb.addFor(infoFor);
					if (assujettissement == null) {
						// dans les cas de non-assujettissement, seul le premier exercice commercial de l'année doit être signalé
						break;
					}
				}
			}
		}
	}

	@Override
	public List<DateRange> getPeriodesFiscales(Entreprise entreprise, TiersService tiersService) {
		final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
		return new ArrayList<>(exercices);
	}

	@Override
	protected InfoCommunePM createInfoCommune(int noOfsCommune) {
		return new InfoCommunePM(noOfsCommune);
	}

	@Override
	protected Pair<Long, RegDate> buildInfoContribuableKey(InfoContribuablePM infoContribuable) {
		return Pair.of(infoContribuable.noCtb, infoContribuable.getDateBouclement());
	}
}
