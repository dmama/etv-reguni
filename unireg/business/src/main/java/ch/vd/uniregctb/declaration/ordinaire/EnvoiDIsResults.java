package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsEnMasseProcessor.LotContribuables;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;
import ch.vd.uniregctb.tiers.Contribuable;

public class EnvoiDIsResults extends JobResults {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), // --------------------------------------------------------------
		ROLLBACK("Le traitement du lot a échoué et a été rollbacké"),
		COLLISION_DI("une déclaration existe déjà, mais elle ne correspond pas à celle calculée"), // -----
		FOR_GESTION_NUL("le contribuable ne possède pas de for de gestion");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		DI_DEJA_EXISTANTE("la déclaration existe déjà"),   // --------------------------------------------
		CTB_EXCLU("le contribuable est exclu des envois automatiques");

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

		public Erreur(Long noCtb, Integer officeImpotID, ErreurType raison, String details) {
			super((noCtb == null ? 0 : noCtb.longValue()), officeImpotID, details);
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

	// Paramètres d'entrée
	public final int annee;
	public final TypeContribuableDI type;
	public final RegDate dateTraitement;
	public final int nbMax;
	public final Long noCtbMin;
	public final Long noCtbMax;

	// Données de processing
	public int nbCtbsTotal;
	public List<Long> ctbsTraites = new ArrayList<Long>();
	public List<Long> ctbsIndigents = new ArrayList<Long>();
	public List<Ignore> ctbsIgnores = new ArrayList<Ignore>();
	public List<Erreur> ctbsEnErrors = new ArrayList<Erreur>();
	public List<Erreur> ctbsRollback = new ArrayList<Erreur>();
	public boolean interrompu;

	public EnvoiDIsResults(int annee, TypeContribuableDI type, RegDate dateTraitement, int nbMax, Long noCtbMin, Long noCtbMax) {
		this.annee = annee;
		this.type = type;
		this.dateTraitement = dateTraitement;
		this.nbMax = nbMax;
		this.noCtbMin = noCtbMin;
		this.noCtbMax = noCtbMax;
	}

	public void addCtbTraites(Long noCtb) {
		ctbsTraites.add(noCtb);
	}

	public void addCtbIndigent(Long noCtb) {
		ctbsIndigents.add(noCtb);
	}

	public void addErrorException(Long idCtb, Exception e) {
		ctbsEnErrors.add(new Erreur(idCtb, null, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addRollback(LotContribuables lot, Exception e) {
		List<Long> ids = lot.ids;
		for (Long id : ids) {
			ctbsRollback.add(new Erreur(id, null, ErreurType.ROLLBACK, e.getMessage()));
		}
	}
	public void addErrorDICollision(Contribuable ctb, RegDate dateDebut, RegDate dateFin, String details) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.COLLISION_DI, details));
	}

	public void addErrorForGestionNul(Contribuable ctb, RegDate dateDebut, RegDate dateFin, String details) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.FOR_GESTION_NUL, details));
	}

	public void addIgnoreDIDejaExistante(Contribuable ctb, RegDate dateDebut, RegDate dateFin) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DI_DEJA_EXISTANTE, null));
	}

	public void addIgnoreCtbExclu(Contribuable ctb, RegDate dateDebut, RegDate dateFin, RegDate dateLimiteExclusion) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.CTB_EXCLU, "Date limite de l'exclusion = "
				+ RegDateHelper.dateToDisplayString(dateLimiteExclusion)));
	}

	public void add(EnvoiDIsResults rapport) {
		if (rapport != null) {
			this.nbCtbsTotal += rapport.nbCtbsTotal;
			this.ctbsTraites.addAll(rapport.ctbsTraites);
			this.ctbsIndigents.addAll(rapport.ctbsIndigents);
			this.ctbsEnErrors.addAll(rapport.ctbsEnErrors);
			this.ctbsIgnores.addAll(rapport.ctbsIgnores);
		}
	}
}
