package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;

/**
 * Contient les données brutes permettant de générer le document de rapport de l'exécution du processeur.
 */
public class DeterminationDIsResults extends JobResults<Long, DeterminationDIsResults> {

	public enum ErreurType {
		CTB_INVALIDE("le contribuable ne valide pas."), // -----------------------------------------------------
		DONNEES_INCOHERENTES("les données sont incohérente"), // -----------------------------------------------
		EXCEPTION(EXCEPTION_DESCRIPTION), // -------------------------------------------------------------------
		COLLISION_DECLARATION("une DI existe déjà, mais elle ne correspond pas à la période d'imposition calculée");

		private String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		DIPLOMATE("le contribuable est un diplomate Suisse basé à l'étranger"), // -----------------------------
		OPTIONNELLE("la déclaration est optionnelle"), // ------------------------------------------------------
		REMPLACEE_PAR_NOTE("la déclaration est remplacée par une note à l'administration fiscale"), // ---------
		TACHE_ENVOI_DEJA_EXISTANTE("une tâche d'envoi de déclaration existe déjà"),// -------------------------------------------------------
		TACHE_ANNULATION_DEJA_EXISTANTE("une tâche d'annulation de la déclaration existe déjà"),// -------------
		DECL_DEJA_EXISTANTE("la déclaration existe déjà"), // --------------------------------------------------
		PAS_ASSUJETTI("le contribuable n'est pas assujetti au rôle ordinaire");

		private String description;

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
			super((noCtb == null ? 0 : noCtb), officeImpotID, details);
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

	public enum TraiteType {
		TACHE_ENVOI_CREEE("Une tâche d'envoi de déclaration a été créée."),
		TACHE_ENVOI_ANNULEE("La tâche d'envoi de déclaration préexistante a été annulée."),
		TACHE_ANNULATION_CREE("Une tâche d'annulation de la déclaration préexistante a été créée.");

		private String description;

		private TraiteType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Traite {
		public final long noCtb;
		public final Integer officeImpotID;
		public final RegDate dateDebut;
		public final RegDate dateFin;
		public final TraiteType raison;

		public Traite(long noCtb, Integer officeImpotID, RegDate dateDebut, RegDate dateFin, TraiteType raison) {
			this.noCtb = noCtb;
			this.officeImpotID = officeImpotID;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.raison = raison;
		}
	}

	public final int annee;
	public final RegDate dateTraitement;
	public int nbCtbsTotal;
	public List<Traite> traites = new ArrayList<Traite>();
	public List<Ignore> ignores = new ArrayList<Ignore>();
	public List<Erreur> erreurs = new ArrayList<Erreur>();
	public boolean interrompu;

	public DeterminationDIsResults(int annee, RegDate dateTraitement) {
		this.annee = annee;
		this.dateTraitement = dateTraitement;
	}

	public void addTacheEnvoiCreee(Contribuable ctb, TacheEnvoiDeclarationImpot tache) {
		traites.add(new Traite(ctb.getNumero(), ctb.getOfficeImpotId(), tache.getDateDebut(), tache.getDateFin(), TraiteType.TACHE_ENVOI_CREEE));
	}

	public void addTacheEnvoiAnnulee(Contribuable ctb, TacheEnvoiDeclarationImpot tache) {
		traites.add(new Traite(ctb.getNumero(), ctb.getOfficeImpotId(), tache.getDateDebut(), tache.getDateFin(), TraiteType.TACHE_ENVOI_ANNULEE));
	}

	public void addTacheAnnulationCreee(Contribuable ctb, TacheAnnulationDeclarationImpot tache) {
		traites.add(new Traite(ctb.getNumero(), ctb.getOfficeImpotId(), tache.getDeclarationImpotOrdinaire().getDateDebut(), tache.getDeclarationImpotOrdinaire().getDateFin(),
				TraiteType.TACHE_ANNULATION_CREE));
	}

	public void addErrorCtbInvalide(Contribuable ctb) {
		erreurs.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.CTB_INVALIDE, null));
	}

	public void addCtbErrorDonneesIncoherentes(Contribuable ctb, String details) {
		erreurs.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DONNEES_INCOHERENTES, details));
	}

	public void addErrorException(Long idCtb, Exception e) {
		erreurs.add(new Erreur(idCtb, null, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		erreurs.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorDeclarationCollision(Contribuable ctb, String details) {
		erreurs.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.COLLISION_DECLARATION, details));
	}

	public void addIgnorePasAssujetti(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.PAS_ASSUJETTI, null));
	}

	public void addIgnoreTacheEnvoiDejaExistante(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.TACHE_ENVOI_DEJA_EXISTANTE, null));
	}

	public void addIgnoreTacheAnnulationDejaExistante(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.TACHE_ANNULATION_DEJA_EXISTANTE, null));
	}

	public void addIgnoreDiplomate(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DIPLOMATE, null));
	}

	public void addIgnoreDeclarationOptionnelle(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.OPTIONNELLE, null));
	}

	public void addIgnoreDeclarationDejaExistante(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DECL_DEJA_EXISTANTE, null));
	}

	public void addIgnoreDeclarationRemplaceeParNote(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.REMPLACEE_PAR_NOTE, null));
	}

	public void addAll(DeterminationDIsResults right) {
		this.nbCtbsTotal += right.nbCtbsTotal;
		this.traites.addAll(right.traites);
		this.ignores.addAll(right.ignores);
		this.erreurs.addAll(right.erreurs);
	}
}
