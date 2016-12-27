package ch.vd.uniregctb.validation;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Contient les données brutes permettant de générer le rapport de l'exécution de la validation des tiers.
 */
public class ValidationJobResults extends JobResults<Long, ValidationJobResults> {

	public enum ErreurType {
		INVALIDE("le tiers ne valide pas."),
		PERIODES_IMPOSITION("les périodes d'imposition ne peuvent pas être calculées"),
		ADRESSES("les adresses n'ont pas pu être calculées"),
		DI("Incohérence dans les dates de DI");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {

		public final ErreurType raison;

		public Erreur(long noTiers, Integer officeImpotID, ValidationResults erreur, String nomTiers) {
			super(noTiers, officeImpotID, buildDetails(erreur), nomTiers);
			this.raison = ErreurType.INVALIDE;
		}

		public Erreur(long numero, Integer oid, String message, ErreurType raison, String nomTiers) {
			super(numero, oid, message, nomTiers);
			this.raison = raison;
		}

		public Erreur(long numero, Integer oid, Exception exception, ErreurType raison, String nomTiers) {
			super(numero, oid, exception.getMessage(), nomTiers);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	private static String buildDetails(ValidationResults erreur) {

		StringBuilder message = new StringBuilder();
		final List<String> errors = erreur.getErrors();
		final List<String> warnings = erreur.getWarnings();
		message.append(errors.size()).append(" erreur(s) - ").append(warnings.size()).append(" avertissement(s):\n");

		for (String e : errors) {
			message.append(" [E] ").append(e).append('\n');
		}

		for (String w : warnings) {
			message.append(" [W] ").append(w).append('\n');
		}

		return message.toString();

	}

	public final RegDate dateTraitement;
	public final boolean calculatePeriodesImposition;
	public final boolean coherencePeriodesImpositionWrtDIs;
	public final boolean calculateAdresses;
	public final boolean modeStrict;

	public int nbTiersTotal;
	public final List<Erreur> erreursValidation = new ArrayList<>();
	public final List<Erreur> erreursPeriodesImposition = new ArrayList<>();
	public final List<Erreur> erreursCoherenceDI = new ArrayList<>();
	public final List<Erreur> erreursAdresses = new ArrayList<>();
	public boolean interrompu;

	public ValidationJobResults(RegDate dateTraitement, boolean calculatePeriodesImposition, boolean coherencePeriodesImpositionWrtDIs,
	                            boolean calculateAdresses, boolean modeStrict, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
		this.calculatePeriodesImposition = calculatePeriodesImposition;
		this.coherencePeriodesImpositionWrtDIs = coherencePeriodesImpositionWrtDIs;
		this.calculateAdresses = calculateAdresses;
		this.modeStrict = modeStrict;
	}

	public int getNbTiersTotal() {
		synchronized (this) {
			return nbTiersTotal;
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

	public void incTiersTotal() {
		synchronized (this) {
			nbTiersTotal++;
		}
	}

	public void addErrorTiersInvalide(Tiers tiers, ValidationResults erreur) {
		final Erreur e = new Erreur(tiers.getNumero(), tiers.getOfficeImpotId(), erreur, getNom(tiers.getNumero()));
		synchronized (this) {
			erreursValidation.add(e);
		}
	}

	public void addErrorPeriodeImposition(Contribuable ctb, int annee, Exception exception) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), "Année " + annee + ": " + exception.getMessage(),
				ErreurType.PERIODES_IMPOSITION, getNom(ctb.getNumero()));
		synchronized (this) {
			erreursPeriodesImposition.add(e);
		}
	}

	public void addErrorCoherenceDi(Contribuable ctb, String message) {
		final Erreur e = new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), message, ErreurType.DI, getNom(ctb.getNumero()));
		synchronized (this) {
			erreursCoherenceDI.add(e);
		}
	}

	public void addErrorAdresses(Tiers tiers, Exception exception) {
		final Erreur e = new Erreur(tiers.getNumero(), tiers.getOfficeImpotId(), exception, ErreurType.ADRESSES, getNom(tiers.getNumero()));
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
