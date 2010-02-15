package ch.vd.uniregctb.metier;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Résultats détaillés de l'exécution du job de fusion de communes.
 */
public class FusionDeCommunesResults extends JobResults {

	public enum ErreurType {
		UNKNOWN_EXCEPTION("une exception inconnue a été levée"),
		VALIDATION("le contribuable ne valide pas");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		FORS_DEJA_SUR_COMMUNE_RESULTANTE("Les fors du contribuable sont déjà sur la commune résultante.");

		private final String description;

		private IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(long noCtb, Integer officeImpotID, ErreurType raison, String details) {
			super(noCtb, officeImpotID, details);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;

		public Ignore(long noCtb, Integer officeImpotID, IgnoreType raison, String details) {
			super(noCtb, officeImpotID, details);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	// paramètres d'entrée
	public final RegDate dateTraitement;
	public final RegDate dateFusion;
	public final Set<Integer> anciensNoOfs;
	public final int nouveauNoOfs;

	// résultats
	public int nbTiersTotal;
	public List<Long> tiersTraites = new ArrayList<Long>();
	public List<Ignore> tiersIgnores = new ArrayList<Ignore>();
	public List<Erreur> tiersEnErrors = new ArrayList<Erreur>();
	public boolean interrompu;

	public FusionDeCommunesResults(Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, RegDate dateTraitement) {
		this.dateFusion = dateFusion;
		this.anciensNoOfs = anciensNoOfs;
		this.nouveauNoOfs = nouveauNoOfs;
		this.dateTraitement = dateTraitement;
	}

	public void add(FusionDeCommunesResults right) {
		this.nbTiersTotal += right.nbTiersTotal;
		this.tiersTraites.addAll(right.tiersTraites);
		this.tiersIgnores.addAll(right.tiersIgnores);
		this.tiersEnErrors.addAll(right.tiersEnErrors);
	}

	public void addOnCommitException(Long habitantId, Exception e) {
		tiersEnErrors.add(new Erreur(habitantId, null, ErreurType.UNKNOWN_EXCEPTION, e.getMessage()));
	}

	public void addTiersInvalide(Tiers t, ValidationResults results) {
		tiersEnErrors.add(new Erreur(t.getNumero(), t.getOfficeImpotId(), ErreurType.VALIDATION, results.toString()));
	}

	public void addTiersIgnoreDejaSurCommuneResultante(Tiers tiers) {
		tiersIgnores.add(new Ignore(tiers.getNumero(), tiers.getOfficeImpotId(), IgnoreType.FORS_DEJA_SUR_COMMUNE_RESULTANTE, null));
	}
}
