package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

/**
 * Contient les données brutes permettant de générer le document de rapport de l'exécution du processeur.
 */
public class DeterminationDIsPPResults extends JobResults<Long, DeterminationDIsPPResults> {

	public enum ErreurType {
		CTB_INVALIDE("le contribuable ne valide pas."), // -----------------------------------------------------
		DONNEES_INCOHERENTES("les données sont incohérentes"), // ----------------------------------------------
		EXCEPTION(EXCEPTION_DESCRIPTION), // -------------------------------------------------------------------
		COLLISION_DECLARATION("une DI existe déjà, mais elle ne correspond pas à la période d'imposition calculée");

		private final String description;

		ErreurType(String description) {
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

		private final String description;

		IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(Long noCtb, Integer officeImpotID, ErreurType raison, String details, String nomCtb) {
			super((noCtb == null ? 0 : noCtb), officeImpotID, details, nomCtb);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;

		public Ignore(long noCtb, Integer officeImpotID, IgnoreType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public enum TraiteType {
		TACHE_ENVOI_CREEE("Une tâche d'envoi de déclaration a été créée."),
		TACHE_ENVOI_ANNULEE("La tâche d'envoi de déclaration pré-existante a été annulée."),
		TACHE_ANNULATION_CREE("Une tâche d'annulation de la déclaration pré-existante a été créée.");

		private final String description;

		TraiteType(String description) {
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
		public final Integer codeSegment;
		public final TypeDocument typeDocument;
		public final TypeContribuable typeContribuable;
		public final TraiteType raison;

		public Traite(long noCtb, Integer officeImpotID, RegDate dateDebut, RegDate dateFin, TraiteType raison, Integer codeSegment, TypeDocument typeDocument, TypeContribuable typeContribuable) {
			this.noCtb = noCtb;
			this.officeImpotID = officeImpotID;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.raison = raison;
			this.codeSegment = codeSegment;
			this.typeDocument = typeDocument;
			this.typeContribuable = typeContribuable;
		}
	}

	public final int annee;
	public final RegDate dateTraitement;
	public final int nbThreads;
	public int nbCtbsTotal;
	public final List<Traite> traites = new ArrayList<>();
	public final List<Ignore> ignores = new ArrayList<>();
	public final List<Erreur> erreurs = new ArrayList<>();
	public boolean interrompu;

	public DeterminationDIsPPResults(int annee, RegDate dateTraitement, int nbThreads, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.annee = annee;
		this.dateTraitement = dateTraitement;
		this.nbThreads = nbThreads;
	}

	public void addTacheEnvoiCreee(ContribuableImpositionPersonnesPhysiques ctb, TacheEnvoiDeclarationImpotPP tache) {
		traites.add(new Traite(ctb.getNumero(), tache.getCollectiviteAdministrativeAssignee().getNumeroCollectiviteAdministrative(), tache.getDateDebut(), tache.getDateFin(), TraiteType.TACHE_ENVOI_CREEE, tache.getCodeSegment(), tache.getTypeDocument(), tache.getTypeContribuable()));
	}

	public void addTacheEnvoiAnnulee(ContribuableImpositionPersonnesPhysiques ctb, TacheEnvoiDeclarationImpotPP tache) {
		traites.add(new Traite(ctb.getNumero(), tache.getCollectiviteAdministrativeAssignee().getNumeroCollectiviteAdministrative(), tache.getDateDebut(), tache.getDateFin(), TraiteType.TACHE_ENVOI_ANNULEE, null, null, null));
	}

	public void addTacheAnnulationCreee(Contribuable ctb, TacheAnnulationDeclarationImpot tache) {
		traites.add(new Traite(ctb.getNumero(), tache.getCollectiviteAdministrativeAssignee().getNumeroCollectiviteAdministrative(), tache.getDeclaration().getDateDebut(), tache.getDeclaration().getDateFin(),
		                       TraiteType.TACHE_ANNULATION_CREE, null, null, null));
	}

	public void addErrorCtbInvalide(Contribuable ctb) {
		erreurs.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.CTB_INVALIDE, null, getNom(ctb.getNumero())));
	}

	public void addCtbErrorDonneesIncoherentes(Contribuable ctb, String details) {
		erreurs.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DONNEES_INCOHERENTES, details, getNom(ctb.getNumero())));
	}

	@Override
	public void addErrorException(Long idCtb, Exception e) {
		erreurs.add(new Erreur(idCtb, null, ErreurType.EXCEPTION, e.getMessage(), getNom(idCtb)));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		erreurs.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, e.getMessage(), getNom(ctb.getNumero())));
	}

	public void addErrorDeclarationCollision(Contribuable ctb, String details) {
		erreurs.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.COLLISION_DECLARATION, details, getNom(ctb.getNumero())));
	}

	public void addIgnorePasAssujetti(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.PAS_ASSUJETTI, null, getNom(ctb.getNumero())));
	}

	public void addIgnoreTacheEnvoiDejaExistante(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.TACHE_ENVOI_DEJA_EXISTANTE, null, getNom(ctb.getNumero())));
	}

	public void addIgnoreTacheAnnulationDejaExistante(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.TACHE_ANNULATION_DEJA_EXISTANTE, null, getNom(ctb.getNumero())));
	}

	public void addIgnoreDiplomate(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DIPLOMATE, null, getNom(ctb.getNumero())));
	}

	public void addIgnoreDeclarationOptionnelle(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.OPTIONNELLE, null, getNom(ctb.getNumero())));
	}

	public void addIgnoreDeclarationDejaExistante(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DECL_DEJA_EXISTANTE, null, getNom(ctb.getNumero())));
	}

	public void addIgnoreDeclarationRemplaceeParNote(Contribuable ctb) {
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.REMPLACEE_PAR_NOTE, null, getNom(ctb.getNumero())));
	}

	@Override
	public void addAll(DeterminationDIsPPResults right) {
		this.nbCtbsTotal += right.nbCtbsTotal;
		this.traites.addAll(right.traites);
		this.ignores.addAll(right.ignores);
		this.erreurs.addAll(right.erreurs);
	}
}
