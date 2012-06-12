package ch.vd.uniregctb.identification.individus;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public class IdentificationIndividusNonMigresResults {

	public static enum ErreurType {
		EXCEPTION(JobResults.EXCEPTION_DESCRIPTION),
		INDIVIDU_REGPP_INCONNU("L'individu est inconnu dans RegPP"),
		INDIVIDU_RCPERS_INCONNU("L'individu est inconnu dans RcPers, alors que la recherche l'avait retourné"),
		INDIVIDU_REGPP_NOM_VIDE("Le nom de l'individu est vide dans RegPP");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static enum NonIdentificationType {
		AUCUN_INDIVIDU_RCPERS("Aucune correspondance de l'individu n'a été trouvée dans RcPers"),
		PLUSIEURS_INDIVIDUS_RCPERS("Plusieurs correspondances de l'individu ont été trouvées dans RcPers");

		private final String description;

		private NonIdentificationType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static enum IgnoreType {
		INDIVIDU_RCPERS_CONNU("L'individu est connu dans RcPers");

		private final String description;

		private IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur {
		public final long noCtb;
		public final IdentificationIndividu identiteRegPP;
		public final Long noIndRcpers;
		public final ErreurType raison;
		public final String details;

		public Erreur(long noCtb, IdentificationIndividu identiteRegPP, ErreurType raison, String details) {
			this.noCtb = noCtb;
			this.identiteRegPP = identiteRegPP;
			this.noIndRcpers = null;
			this.raison = raison;
			this.details = details;
		}

		public Erreur(long noCtb, IdentificationIndividu identiteRegPP, Long noIndRcpers, ErreurType raison, String details) {
			this.noCtb = noCtb;
			this.identiteRegPP = identiteRegPP;
			this.noIndRcpers = noIndRcpers;
			this.raison = raison;
			this.details = details;
		}
	}

	public static class Ignore {
		public final long noCtb;
		public final IdentificationIndividu identiteRegPP;
		public final IdentificationIndividu identiteRcPers;
		public final IgnoreType raison;

		public Ignore(long noCtb, IdentificationIndividu identiteRegPP, IdentificationIndividu identiteRcPers,
		              IgnoreType raison) {
			this.noCtb = noCtb;
			this.identiteRegPP = identiteRegPP;
			this.identiteRcPers = identiteRcPers;
			this.raison = raison;
		}
	}

	public static class Identifie {
		public final long noCtb;
		public final IdentificationIndividu identiteRegPP;
		public final IdentificationIndividu identiteRcPers;
		public final String strategie;
		public final String remarque;
		public final List<Long> tiersDejaLies;

		public Identifie(long noCtb, IdentificationIndividu identiteRegPP, IdentificationIndividu identiteRcPers, String strategie, String remarque, List<Long> tiersDejaLies) {
			this.noCtb = noCtb;
			this.identiteRegPP = identiteRegPP;
			this.identiteRcPers = identiteRcPers;
			this.strategie = strategie;
			this.remarque = remarque;
			this.tiersDejaLies = tiersDejaLies;
		}
	}

	public static class NonIdentifie {
		public final long noCtb;
		public final IdentificationIndividu identiteRegPP;
		public final NonIdentificationType raison;
		public final String details;

		public NonIdentifie(long noCtb, IdentificationIndividu identiteRegPP, NonIdentificationType raison, String details) {
			this.noCtb = noCtb;
			this.identiteRegPP = identiteRegPP;
			this.raison = raison;
			this.details = details;
		}
	}

	public final long startTime = System.currentTimeMillis();
	public long endTime = 0;

	// Paramètres d'entrée
	public final RegDate dateTraitement;

	// Données de processing
	public final int nbCtbsTotal;
	public final List<Identifie> identifies = new ArrayList<Identifie>();
	public final List<NonIdentifie> nonIdentifies = new ArrayList<NonIdentifie>();
	public final List<Ignore> ignores = new ArrayList<Ignore>();
	public final List<Erreur> erreurs = new ArrayList<Erreur>();
	public boolean interrompu;

	public IdentificationIndividusNonMigresResults(RegDate dateTraitement, int nbCtbsTotal) {
		this.dateTraitement = dateTraitement;
		this.nbCtbsTotal = nbCtbsTotal;
	}

	public void addIdentifie(Long noCtb, IdentificationIndividu identiteRegPP, IdentificationIndividu identiteRcPers, String strategie, String remarque, List<Long> tiersDejaLies) {
		identifies.add(new Identifie(noCtb, identiteRegPP, identiteRcPers, strategie, remarque, tiersDejaLies));
	}

	public void addNonIdentifie(long noCtb, IdentificationIndividu identRegPP, NonIdentificationType raison) {
		nonIdentifies.add(new NonIdentifie(noCtb, identRegPP, raison, null));
	}

	public void addNonIdentifie(long noCtb, IdentificationIndividu identRegPP, NonIdentificationType raison, String details) {
		nonIdentifies.add(new NonIdentifie(noCtb, identRegPP, raison, details));
	}

	public void addIgnore(long noCtb, IdentificationIndividu identiteRegPP, IdentificationIndividu identiteRcPers, IgnoreType raison) {
		ignores.add(new Ignore(noCtb, identiteRegPP, identiteRcPers, raison));
	}

	public void addError(long noCtb, long noInd, ErreurType raison) {
		erreurs.add(new Erreur(noCtb, new IdentificationIndividu(noInd), raison, null));
	}

	public void addError(long noCtb, IdentificationIndividu identRegPP, ErreurType raison) {
		erreurs.add(new Erreur(noCtb, identRegPP, raison, null));
	}

	public void addError(long noCtb, IdentificationIndividu identRegPP, long noIndRcpers, ErreurType raison) {
		erreurs.add(new Erreur(noCtb, identRegPP, noIndRcpers, raison, null));
	}

	public void addException(long idCtb, long noInd, Exception e) {
		erreurs.add(new Erreur(idCtb, new IdentificationIndividu(noInd), ErreurType.EXCEPTION, e.getMessage()));
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}
}
