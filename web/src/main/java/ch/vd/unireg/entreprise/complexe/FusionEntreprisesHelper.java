package ch.vd.unireg.entreprise.complexe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Petit recueil de méthodes pratiques autour de la fusion d'entreprises
 */
public abstract class FusionEntreprisesHelper {

	/**
	 * Couple de dates bilan/contrat qui définissent, quelque part, une fusion d'entreprises
	 */
	public static class DatesFusion {

		public final RegDate dateBilan;
		public final RegDate dateContrat;

		public DatesFusion(RegDate dateBilan, RegDate dateContrat) {
			this.dateBilan = dateBilan;
			this.dateContrat = dateContrat;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final DatesFusion that = (DatesFusion) o;
			return that.dateBilan == this.dateBilan && that.dateContrat == this.dateContrat;
		}

		@Override
		public int hashCode() {
			int result = dateBilan != null ? dateBilan.hashCode() : 0;
			result = 31 * result + (dateContrat != null ? dateContrat.hashCode() : 0);
			return result;
		}
	}

	/**
	 * @param absorbante une entreprise susceptible d'avoir absorbé d'autres entreprises
	 * @return les couples de dates bilan/contrat des absorptions trouvées (ensemble vide possible) avec les entreprises absorbées correspondantes
	 */
	@NotNull
	public static Map<DatesFusion, List<Entreprise>> getAbsorptions(Entreprise absorbante, TiersService tiersService) {
		final Map<DatesFusion, List<Entreprise>> map = new HashMap<>();
		for (RapportEntreTiers ret : absorbante.getRapportsObjet()) {
			if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.FUSION_ENTREPRISES) {
				final RegDate dateBilan = ret.getDateDebut();
				final Tiers absorbee = tiersService.getTiers(ret.getSujetId());
				if (absorbee == null || !(absorbee instanceof Entreprise)) {
					throw new TiersNotFoundException(ret.getSujetId());
				}
				final EtatEntreprise dd = getEtatAbsorbee((Entreprise) absorbee);
				if (dd != null) {
					final RegDate dateContrat = dd.getDateObtention();
					final DatesFusion key = new DatesFusion(dateBilan, dateContrat);
					final List<Entreprise> absorbees = map.computeIfAbsent(key, k -> new ArrayList<>());
					absorbees.add((Entreprise) absorbee);
				}
			}
		}
		return map;
	}

	@Nullable
	private static EtatEntreprise getEtatAbsorbee(Entreprise entreprise) {
		final Set<EtatEntreprise> etats = entreprise.getEtats();
		for (EtatEntreprise etat : etats) {
			if (!etat.isAnnule() && etat.getType() == TypeEtatEntreprise.ABSORBEE) {
				return etat;
			}
		}
		return null;
	}

}
