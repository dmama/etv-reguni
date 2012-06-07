package ch.vd.uniregctb.validation;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Contient les données brutes permettant de générer le rapport de l'exécution de la validation des tiers.
 */
public class ValidationJobResults extends JobResults<Long, ValidationJobResults> {

	public enum ErreurType {
		INVALIDE("le contribuable ne valide pas."),
		PERIODES_IMPOSITION("les périodes d'imposition ne peuvent pas être calculées"),
		ADRESSES("les adresses n'ont pas pu être calculées"),
		DI("Incohérence dans les dates de DI");

		private final String description;

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
	public final boolean calculatePeriodesImposition;
	public final boolean coherencePeriodesImpositionWrtDIs;
	public final boolean calculateAdresses;
	public final boolean modeStrict;

	public int nbCtbsTotal;
	public final List<Erreur> erreursValidation = new ArrayList<Erreur>();
	public final List<Erreur> erreursPeriodesImposition = new ArrayList<Erreur>();
	public final List<Erreur> erreursCoherenceDI = new ArrayList<Erreur>();
	public final List<Erreur> erreursAdresses = new ArrayList<Erreur>();
	public boolean interrompu;

	public ValidationJobResults(RegDate dateTraitement, boolean calculatePeriodesImposition, boolean coherencePeriodesImpositionWrtDIs,
	                            boolean calculateAdresses, boolean modeStrict) {
		this.dateTraitement = dateTraitement;
		this.calculatePeriodesImposition = calculatePeriodesImposition;
		this.coherencePeriodesImpositionWrtDIs = coherencePeriodesImpositionWrtDIs;
		this.calculateAdresses = calculateAdresses;
		this.modeStrict = modeStrict;
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

	public int getNbErreursPeriodesImposition() {
		synchronized (this) {
			return erreursPeriodesImposition.size();
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

	public void addErrorPeriodeImposition(Contribuable ctb, int annee, Exception exception) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), "Année " + annee + ": " + exception.getMessage(),
				ErreurType.PERIODES_IMPOSITION);
		synchronized (this) {
			erreursPeriodesImposition.add(e);
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

	@Override
	public void addErrorException(Long element, Exception e) {
		throw new NotImplementedException();
	}

	@Override
	public void addAll(ValidationJobResults rapport) {
		throw new NotImplementedException();
	}
}
