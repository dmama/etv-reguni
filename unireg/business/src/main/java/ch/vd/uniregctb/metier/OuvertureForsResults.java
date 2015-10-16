package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

public class OuvertureForsResults extends JobResults<Long, OuvertureForsResults> {

	public enum ErreurType {
		DATE_NAISSANCE_NULLE("l'habitant ne possède pas de date de naissance."), // ------------------------------------------------
		DOMICILE_INCONNU("le domicile de l'habitant est inconnu."), // -------------------------------------------------------------
		INFRA_EXCEPTION("le service infrastructure a retourné une exception"), // --------------------------------------------------
		ADRESSE_EXCEPTION("le service d'adresses a retourné une exception"), // ----------------------------------------------------
		CIVIL_EXCEPTION("le service civil a retourné une exception"), // ----------------------------------------------------------
		INDIVIDU_INCONNU("l'individu associé à l'habitant n'existe pas"), // -------------------------------------------------------
		UNKNOWN_EXCEPTION("une exception inconnue a été levée"), // ----------------------------------------------------------------
		CONSTRAINT_VIOLATION_EXCEPTION("une exception de violation de contrainte base de données a été levée"), // ----------------------------------------------------------------
		VALIDATION("le contribuable ne valide pas"), // ----------------------------------------------------------------------------
		VALIDATION_APRES_OUVERTURE("le contribuable ne valide plus après l'ouverture de son for de majorité"), // ------------------
		INCOHERENCE_FOR_FISCAUX("une incohérence avec les fors fiscaux a été détectée"), // ----------------------------------------
		INCOHERENCE_ETAT_CIVIL("une incohérence avec l'état civil a été détectée");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Traite extends Info {

		public final RegDate dateOuverture;
		public final ModeImposition modeImposition;

		public Traite(long noCtb, Integer officeImpotID, RegDate dateOuverture, ModeImposition modeImposition, String nomCtb) {
			super(noCtb, officeImpotID, null, nomCtb);
			this.dateOuverture = dateOuverture;
			this.modeImposition = modeImposition;
		}

		@Override
		public String getDescriptionRaison() {
			return "Mode d'imposition = " + modeImposition.texte();
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

	public final RegDate dateTraitement;
	public int nbHabitantsTotal;
	public int nbHabitantsMineurs;
	public int nbHabitantsDecedes;
	public int nbHabitantsHorsVD;
	public final List<Traite> habitantTraites = new ArrayList<>();
	public final List<Erreur> habitantEnErrors = new ArrayList<>();
	public boolean interrompu;

	public OuvertureForsResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	@Override
	public void addAll(OuvertureForsResults right) {
		this.nbHabitantsTotal += right.nbHabitantsTotal;
		this.nbHabitantsMineurs += right.nbHabitantsMineurs;
		this.nbHabitantsDecedes += right.nbHabitantsDecedes;
		this.nbHabitantsHorsVD += right.nbHabitantsHorsVD;
		this.habitantTraites.addAll(right.habitantTraites);
		this.habitantEnErrors.addAll(right.habitantEnErrors);
	}

	public void addHabitantTraite(PersonnePhysique h, Integer officeImpotId, RegDate dateOuverture, ModeImposition modeImposition) {
		habitantTraites.add(new Traite(h.getNumero(), officeImpotId, dateOuverture, modeImposition, getNom(h.getNumero())));
	}

	public void addUnknownException(PersonnePhysique h, Exception e) {
		if (e instanceof ServiceInfrastructureException) {
			habitantEnErrors.add(new Erreur(h.getNumero(), null, ErreurType.INFRA_EXCEPTION, e.getMessage(), getNom(h.getNumero())));
		}
		else if (e instanceof ConstraintViolationException) {
			habitantEnErrors.add(new Erreur(h.getNumero(), null, ErreurType.CONSTRAINT_VIOLATION_EXCEPTION, e.getMessage(), getNom(h.getNumero())));
		}
		else{
			habitantEnErrors.add(new Erreur(h.getNumero(), h.getOfficeImpotId(), ErreurType.UNKNOWN_EXCEPTION, e.getMessage(), getNom(h.getNumero())));
		}
	}

	public void addUnknownException(Long habitantId, Exception e) {
		habitantEnErrors.add(new Erreur(habitantId, null, ErreurType.UNKNOWN_EXCEPTION, e.getMessage(), getNom(habitantId)));
	}

	public void addOnCommitException(Long habitantId, Exception e) {
		if (e instanceof ValidationException) {
			habitantEnErrors.add(new Erreur(habitantId, null, ErreurType.VALIDATION_APRES_OUVERTURE, e.getMessage(), getNom(habitantId)));
		}
		else if (e instanceof ServiceInfrastructureException) {
			habitantEnErrors.add(new Erreur(habitantId, null, ErreurType.INFRA_EXCEPTION, e.getMessage(), getNom(habitantId)));
		}
		else if (e instanceof ConstraintViolationException) {
			habitantEnErrors.add(new Erreur(habitantId, null, ErreurType.CONSTRAINT_VIOLATION_EXCEPTION, e.getMessage(), getNom(habitantId)));
		}
		else {
			habitantEnErrors.add(new Erreur(habitantId, null, ErreurType.UNKNOWN_EXCEPTION, e.getMessage(), getNom(habitantId)));
		}
	}

	public void addOuvertureForsException(OuvertureForsException e) {
		final PersonnePhysique h = e.getHabitant();
		habitantEnErrors.add(new Erreur(h.getNumero(), h.getOfficeImpotId(), e.getType(), e.getMessage(), getNom(h.getNumero())));
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		addOnCommitException(element, e);
	}
}
