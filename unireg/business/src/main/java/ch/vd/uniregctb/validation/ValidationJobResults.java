package ch.vd.uniregctb.validation;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;

/**
 * Contient les données brutes permettant de générer le rapport de l'exécution de la validation des tiers.
 */
public class ValidationJobResults extends JobResults {

	public enum ErreurType {
		INVALIDE("le contribuable ne valide pas."), // -----------------------------------------------------
		ASSUJETTISSEMENT("l'assujettissement ne peut pas être calculé"), // --------------------------------
		ADRESSES("les adresses n'ont pas pu être calculées"), // -------------------------------------------
		DI("Incohérence dans les dates de DI"), // ---------------------------------------------------------
		AUTORITE_FOR_FISCAL("L'autorité fiscale du for est incorrecte");

		private String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {

		public final ErreurType raison;

		public Erreur(long noCtb, Integer officeImpotID, ValidationResults erreur) {
			super(noCtb, officeImpotID, buildDetails(erreur));
			this.raison = ErreurType.INVALIDE;
		}

		public Erreur(long numero, Integer oid, String message, ErreurType raison) {
			super(numero, oid, message);
			this.raison = raison;
		}

		public Erreur(long numero, Integer oid, Exception exception, ErreurType raison) {
			super(numero, oid, exception.getMessage());
			this.raison = raison;
		}

		private static String buildDetails(ValidationResults erreur) {

			StringBuilder message = new StringBuilder();
			final List<String> errors = erreur.getErrors();
			final List<String> warnings = erreur.getWarnings();
			message.append(errors.size()).append(" erreur(s) - ").append(warnings.size()).append(" warning(s):\n");

			for (String e : errors) {
				message.append(" [E] ").append(e).append('\n');
			}

			for (String w : warnings) {
				message.append(" [W] ").append(w).append('\n');
			}

			return message.toString();

		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public final RegDate dateTraitement;
	public final boolean calculateAssujettissements;
	public final boolean coherenceAssujetDi;
	public final boolean calculateAdresses;
	public final boolean coherenceAutoritesForsFiscaux;

	public int nbCtbsTotal;
	public List<Erreur> erreursValidation = new ArrayList<Erreur>();
	public List<Erreur> erreursAssujettissement = new ArrayList<Erreur>();
	public List<Erreur> erreursCoherenceDI = new ArrayList<Erreur>();
	public List<Erreur> erreursAdresses = new ArrayList<Erreur>();
	public List<Erreur> erreursAutoritesForsFiscaux = new ArrayList<Erreur>();
	public boolean interrompu;

	public ValidationJobResults(RegDate dateTraitement, boolean calculateAssujettissements, boolean coherenceAssujetDi,
			boolean calculateAdresses, boolean coherenceCommunesForsFiscaux) {
		this.dateTraitement = dateTraitement;
		this.calculateAssujettissements = calculateAssujettissements;
		this.coherenceAssujetDi = coherenceAssujetDi;
		this.calculateAdresses = calculateAdresses;
		this.coherenceAutoritesForsFiscaux = coherenceCommunesForsFiscaux;
	}

	public int getNbCtbsTotal() {
		synchronized (this) {
			return nbCtbsTotal;
		}
	}

	public int getNbErreursValidation() {
		synchronized (this) {
			return erreursValidation.size();
		}
	}

	public int getNbErreursAssujettissement() {
		synchronized (this) {
			return erreursAssujettissement.size();
		}
	}

	public int getNbErreursCoherenceDI() {
		synchronized (this) {
			return erreursCoherenceDI.size();
		}
	}

	public int getNbErreursAdresses() {
		synchronized (this) {
			return erreursAdresses.size();
		}
	}

	public int getNbErreursAutoritesFiscales() {
		synchronized (this) {
			return erreursAutoritesForsFiscaux.size();
		}
	}

	public void incCtbsTotal() {
		synchronized (this) {
			nbCtbsTotal++;
		}
	}

	public void addErrorCtbInvalide(Contribuable ctb, ValidationResults erreur) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), erreur);
		synchronized (this) {
			erreursValidation.add(e);
		}
	}

	public void addErrorAssujettissement(Contribuable ctb, int annee, Exception exception) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), "Année " + annee + ": " + exception.getMessage(),
				ErreurType.ASSUJETTISSEMENT);
		synchronized (this) {
			erreursAssujettissement.add(e);
		}
	}

	public void addErrorCoherenceDi(Contribuable ctb, String message) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), message, ErreurType.DI);
		synchronized (this) {
			erreursCoherenceDI.add(e);
		}
	}

	public void addErrorAdresses(Contribuable ctb, Exception exception) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), exception, ErreurType.ADRESSES);
		synchronized (this) {
			erreursAdresses.add(e);
		}
	}

	public void addErrorAutoriteForFiscal(Contribuable ctb, ForFiscal ff, String message) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), "For fiscal n°" + ff.getId() + ": " + message,
				ErreurType.AUTORITE_FOR_FISCAL);
		synchronized (this) {
			erreursAutoritesForsFiscaux.add(e);
		}
	}

	public void addErrorAutoriteForFiscal(Contribuable ctb, ForFiscal ff, Exception exception) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), "For fiscal n°" + ff.getId() + ": " + exception.getMessage(),
				ErreurType.AUTORITE_FOR_FISCAL);
		synchronized (this) {
			erreursAutoritesForsFiscaux.add(e);
		}
	}
}
