package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Résultats détaillés de l'exécution du job de fusion de communes.
 */
public class FusionDeCommunesResults extends JobResults<Long, FusionDeCommunesResults> {

	public enum ErreurType {
		UNKNOWN_EXCEPTION("Une exception a été levée."),
		VALIDATION("Le contribuable ne valide pas.");

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

		public Erreur(long noCtb, Integer officeImpotID, ErreurType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;

		public Ignore(long noCtb, Integer officeImpotID, IgnoreType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
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
	public final List<Long> tiersTraites = new ArrayList<>();
	public final List<Ignore> tiersIgnores = new ArrayList<>();
	public final List<Erreur> tiersEnErrors = new ArrayList<>();
	public boolean interrompu;

	public FusionDeCommunesResults(Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateFusion = dateFusion;
		this.anciensNoOfs = anciensNoOfs;
		this.nouveauNoOfs = nouveauNoOfs;
		this.dateTraitement = dateTraitement;
	}

	public int getNbTiersTotal() {
		return tiersTraites.size() + tiersIgnores.size() + tiersEnErrors.size();
	}

	@Override
	public void addAll(FusionDeCommunesResults right) {
		this.tiersTraites.addAll(right.tiersTraites);
		this.tiersIgnores.addAll(right.tiersIgnores);
		this.tiersEnErrors.addAll(right.tiersEnErrors);
	}

	public void addOnCommitException(Long habitantId, Exception e) {
		tiersEnErrors.add(new Erreur(habitantId, null, ErreurType.UNKNOWN_EXCEPTION, e.getMessage(), getNom(habitantId)));
	}

	public void addTiersInvalide(Long tiersId, ValidationException e) {
		tiersEnErrors.add(new Erreur(tiersId, null, ErreurType.VALIDATION, e.getMessage(), getNom(tiersId)));
	}

	public void addTiersIgnoreDejaSurCommuneResultante(Tiers tiers) {
		tiersIgnores.add(new Ignore(tiers.getNumero(), tiers.getOfficeImpotId(), IgnoreType.FORS_DEJA_SUR_COMMUNE_RESULTANTE, null, getNom(tiers.getNumero())));
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		if (e instanceof ValidationException) {
			addTiersInvalide(element, (ValidationException) e);
		}
		else {
			addOnCommitException(element, e);
		}
	}
}
