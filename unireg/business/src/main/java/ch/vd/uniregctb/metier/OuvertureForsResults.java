package ch.vd.uniregctb.metier;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

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
		INCOHERENCE_ETAT_CIVIL("une incohérence avec l'état civil a été détectée");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Traite extends Info {

		public final RegDate dateOuverture;
		public final MotifFor motifOuverture;
		public final ModeImposition modeImposition;

		public Traite(long noCtb, Integer officeImpotID, RegDate dateOuverture, MotifFor motifOuverture, ModeImposition modeImposition, String nomCtb) {
			super(noCtb, officeImpotID, null, nomCtb);
			this.dateOuverture = dateOuverture;
			this.motifOuverture = motifOuverture;
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

	public enum IgnoreType {
		AUCUNE_ADRESSE("le contribuable ne possède aucune adresse"),
		ADRESSE_DOMICILE_EST_DEFAUT("l'adresse de domicile du contribuable est une adresse par défaut"),
		FOR_PRINCIPAL_EXISTANT("le contribuable possède déjà un for principal"),
		MINEUR("le contribuable est mineur"),
		DECEDE("le contribauble est décédé"),
		HORS_VD("le contribuable n'est pas résident vaudois");

		private final String description;

		IgnoreType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;

		public Ignore(long noCtb, @Nullable Integer officeImpotID, IgnoreType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
		}

		public IgnoreType getRaison() {
			return raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public final RegDate dateTraitement;
	public int nbHabitantsTotal;
	public final List<Traite> habitantTraites = new LinkedList<>();
	public final List<Erreur> habitantEnErrors = new LinkedList<>();
	public final List<Ignore> contribuablesIgnores = new LinkedList<>();
	public boolean interrompu;

	public OuvertureForsResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	@Override
	public void addAll(OuvertureForsResults right) {
		this.nbHabitantsTotal += right.nbHabitantsTotal;
		this.habitantTraites.addAll(right.habitantTraites);
		this.habitantEnErrors.addAll(right.habitantEnErrors);
		this.contribuablesIgnores.addAll(right.contribuablesIgnores);
	}

	public void addHabitantTraite(PersonnePhysique h, Integer officeImpotId, RegDate dateOuverture, MotifFor motifOuverture, ModeImposition modeImposition) {
		habitantTraites.add(new Traite(h.getNumero(), officeImpotId, dateOuverture, motifOuverture, modeImposition, getNom(h.getNumero())));
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

	public void addContribuableIgnore(PersonnePhysique pp, IgnoreType raison, String details) {
		contribuablesIgnores.add(new Ignore(pp.getNumero(), pp.getOfficeImpotId(), raison, details, getNom(pp)));
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

	public void addOuvertureForsException(OuvertureForsErreurException e) {
		final PersonnePhysique h = e.getHabitant();
		habitantEnErrors.add(new Erreur(h.getNumero(), h.getOfficeImpotId(), e.getType(), e.getMessage(), getNom(h.getNumero())));
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		addOnCommitException(element, e);
	}
}
