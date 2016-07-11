package ch.vd.uniregctb.metier;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Résultats détaillés de l'exécution du job de fusion de communes.
 */
public class FusionDeCommunesResults extends JobResults<Long, FusionDeCommunesResults> {

	public enum ResultatTraitement {
		TRAITE,
		DEJA_BONNE_COMMUNE,
		RIEN_A_FAIRE
	}

	public enum ErreurType {
		UNKNOWN_EXCEPTION("Une exception a été levée."),
		VALIDATION("Le contribuable ne valide pas.");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		DEJA_SUR_COMMUNE_RESULTANTE("Déjà sur la commune résultante.");

		private final String description;

		IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public abstract static class Info {
		public final long noTiers;

		public Info(long noTiers) {
			this.noTiers = noTiers;
		}

		public abstract String getDescriptionRaison();
	}

	public static class Erreur extends Info {
		public final ErreurType raison;
		public final String details;

		public Erreur(long noCtb, ErreurType raison, String details) {
			super(noCtb);
			this.raison = raison;
			this.details = details;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;

		public Ignore(long noCtb, IgnoreType raison) {
			super(noCtb);
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
	public final List<Erreur> tiersEnErreur = new LinkedList<>();

	public final List<Long> tiersTraitesPourFors = new LinkedList<>();
	public final List<Ignore> tiersIgnoresPourFors = new LinkedList<>();

	public final List<Long> tiersTraitesPourDecisions = new LinkedList<>();
	public final List<Ignore> tiersIgnoresPourDecisions = new LinkedList<>();

	public final List<Long> tiersTraitesPourDomicilesEtablissement = new LinkedList<>();
	public final List<Ignore> tiersIgnoresPourDomicilesEtablissement = new LinkedList<>();

	public final List<Long> tiersTraitesPourAllegementsFiscaux = new LinkedList<>();
	public final List<Ignore> tiersIgnoresPourAllegementsFiscaux = new LinkedList<>();

	public int nbTiersExamines = 0;
	public boolean interrompu = false;

	public FusionDeCommunesResults(Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateFusion = dateFusion;
		this.anciensNoOfs = anciensNoOfs;
		this.nouveauNoOfs = nouveauNoOfs;
		this.dateTraitement = dateTraitement;
	}

	public int getNbTiersInspectesPourFors() {
		return tiersTraitesPourFors.size() + tiersIgnoresPourFors.size();
	}

	public int getNbTiersInspectesPourDecisions() {
		return tiersTraitesPourDecisions.size() + tiersIgnoresPourDecisions.size();
	}

	public int getNbTiersInspectesPourDomicilesEtablissement() {
		return tiersTraitesPourDomicilesEtablissement.size() + tiersIgnoresPourDomicilesEtablissement.size();
	}

	@Override
	public void addAll(FusionDeCommunesResults right) {
		this.tiersEnErreur.addAll(right.tiersEnErreur);

		this.tiersTraitesPourFors.addAll(right.tiersTraitesPourFors);
		this.tiersIgnoresPourFors.addAll(right.tiersIgnoresPourFors);

		this.tiersTraitesPourDecisions.addAll(right.tiersTraitesPourDecisions);
		this.tiersIgnoresPourDecisions.addAll(right.tiersIgnoresPourDecisions);

		this.tiersTraitesPourDomicilesEtablissement.addAll(right.tiersTraitesPourDomicilesEtablissement);
		this.tiersIgnoresPourDomicilesEtablissement.addAll(right.tiersIgnoresPourDomicilesEtablissement);

		this.tiersTraitesPourAllegementsFiscaux.addAll(right.tiersTraitesPourAllegementsFiscaux);
		this.tiersIgnoresPourAllegementsFiscaux.addAll(right.tiersIgnoresPourAllegementsFiscaux);

		nbTiersExamines += right.nbTiersExamines;
	}

	private static void addResultat(Long tiersId, List<Long> traites, List<Ignore> ignores, ResultatTraitement resultat) {
		if (resultat == ResultatTraitement.TRAITE) {
			traites.add(tiersId);
		}
		else if (resultat == ResultatTraitement.DEJA_BONNE_COMMUNE) {
			ignores.add(new Ignore(tiersId, IgnoreType.DEJA_SUR_COMMUNE_RESULTANTE));
		}
	}

	public void addResultat(Long tiersId, ResultatTraitement fors, ResultatTraitement decisions, ResultatTraitement domicilesEtablissement, ResultatTraitement allegements) {
		++ nbTiersExamines;
		addResultat(tiersId, tiersTraitesPourFors, tiersIgnoresPourFors, fors);
		addResultat(tiersId, tiersTraitesPourDecisions, tiersIgnoresPourDecisions, decisions);
		addResultat(tiersId, tiersTraitesPourDomicilesEtablissement, tiersIgnoresPourDomicilesEtablissement, domicilesEtablissement);
		addResultat(tiersId, tiersTraitesPourAllegementsFiscaux, tiersIgnoresPourAllegementsFiscaux, allegements);
	}

	@Override
	public void addErrorException(Long tiersId, Exception e) {
		++ nbTiersExamines;
		tiersEnErreur.add(new Erreur(tiersId, e instanceof ValidationException ? ErreurType.VALIDATION : ErreurType.UNKNOWN_EXCEPTION, e.getMessage()));
	}
}
