package ch.vd.uniregctb.identification.individus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.JobResults;

public class IdentificationIndividusNonMigresResults {

	public static enum ErreurType {
		EXCEPTION(JobResults.EXCEPTION_DESCRIPTION),
		INDIVIDU_REGPP_INCONNU("L'individu est inconnu dans RegPP"),
		INDIVIDU_RCPERS_INCONNU("L'individu est inconnu dans RCPers, alors que la recherche l'avait retourné"),
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
		AUCUN_INDIVIDU_RCPERS("Aucune correspondance de l'individu n'a été trouvée dans RCPers"),
		PLUSIEURS_INDIVIDUS_RCPERS("Plusieurs correspondances de l'individu ont été trouvées dans RCPers");

		private final String description;

		private NonIdentificationType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static enum IgnoreType {
		INDIVIDU_RCPERS_CONNU("L'individu est connu dans RCPers");

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

	public static class EnVueDeMigrationNH {

		public final long noCtb;
		public final IdentificationIndividuMigrationNH identiteRegPP;
		public final String remarque;

		public EnVueDeMigrationNH (NonIdentifie nonIdentifie) {
			this.noCtb = nonIdentifie.noCtb;
			this.identiteRegPP= (IdentificationIndividuMigrationNH) nonIdentifie.identiteRegPP;

			StringBuilder rmq = new StringBuilder("Habitant RegPP non migré dans RCPers, il n'a pas été possible de l'identifier.");
			if (nonIdentifie.raison == NonIdentificationType.PLUSIEURS_INDIVIDUS_RCPERS ) {
				rmq.append("Il pourrait cependant correspondre aux individus: ");
				rmq.append(nonIdentifie.details.substring(nonIdentifie.details.indexOf('[') + 1, nonIdentifie.details.lastIndexOf(']')));
			}

			this.remarque = rmq.toString();
		}

		public EnVueDeMigrationNH (Identifie identifie) {
			this.noCtb = identifie.noCtb;
			this.identiteRegPP = (IdentificationIndividuMigrationNH) identifie.identiteRegPP;
			StringBuilder rmq = new StringBuilder(String.format(
					"Habitant RegPP non migré dans RCPers, identifié comme pouvant être l'individu %s grâce à son %s. ",
					identifie.identiteRcPers.noInd,
					identifie.strategie));
			if (identifie.tiersDejaLies.isEmpty()) {
				rmq.append("Aucun tiers n'est lié à cet individu.");
			} else if (identifie.tiersDejaLies.size() == 1) {
				rmq.append(String.format("Potentiellement un doublon du tiers %s", FormatNumeroHelper.numeroCTBToDisplay(identifie.tiersDejaLies.iterator().next())));
			} else {
				rmq.append("Potentiellement un doublon, voir les tiers ");
				for (Iterator<Long> it = identifie.tiersDejaLies.iterator(); it.hasNext(); ) {
					rmq.append(it.next());
					if (it.hasNext()) {
						rmq.append(", ");
					}
				}
				rmq.append(" liés à cet individu");
			}
			this.remarque = rmq.toString();
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
	public final List<EnVueDeMigrationNH> enVueDeMigrationNH = new ArrayList<EnVueDeMigrationNH>();

	public boolean interrompu;
	final public boolean migrationNH;

	public IdentificationIndividusNonMigresResults(RegDate dateTraitement, int nbCtbsTotal, boolean migrationNH) {
		this.dateTraitement = dateTraitement;
		this.nbCtbsTotal = nbCtbsTotal;
		this.migrationNH = migrationNH;

	}
	public void addIdentifie(Long noCtb, IdentificationIndividu identiteRegPP, IdentificationIndividu identiteRcPers, String strategie, String remarque, List<Long> tiersDejaLies) {
		final Identifie i = new Identifie(noCtb, identiteRegPP, identiteRcPers, strategie, remarque, tiersDejaLies);
		identifies.add(i);
		if (migrationNH) {
			enVueDeMigrationNH.add(new EnVueDeMigrationNH(i));
		}
	}

	public void addNonIdentifie(long noCtb, IdentificationIndividu identRegPP, NonIdentificationType raison) {
		final NonIdentifie ni = new NonIdentifie(noCtb, identRegPP, raison, null);
		nonIdentifies.add(ni);
		if (migrationNH) {
			enVueDeMigrationNH.add(new EnVueDeMigrationNH(ni));
		}

	}

	public void addNonIdentifie(long noCtb, IdentificationIndividu identRegPP, NonIdentificationType raison, String details) {
		final NonIdentifie ni = new NonIdentifie(noCtb, identRegPP, raison, details);
		nonIdentifies.add(ni);
		if (migrationNH) {
			enVueDeMigrationNH.add(new EnVueDeMigrationNH(ni));
		}
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

