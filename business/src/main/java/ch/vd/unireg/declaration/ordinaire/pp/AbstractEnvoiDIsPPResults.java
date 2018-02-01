package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPP;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;

public abstract class AbstractEnvoiDIsPPResults<R extends AbstractEnvoiDIsPPResults<R>> extends JobResults<Long, R> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), // --------------------------------------------------------------
		ROLLBACK("Le traitement du lot a échoué et a été rollbacké"),
		COLLISION_DI("une déclaration existe déjà, mais elle ne correspond pas à celle calculée"), // -----
		FOR_GESTION_NUL("le contribuable ne possède pas de for de gestion");

		private final String description;

		ErreurType(String description) {
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

	// Paramètres d'entrée
	public final int annee;
	public final CategorieEnvoiDIPP categorie;
	public final RegDate dateTraitement;
	public final int nbMax;
	public final Long noCtbMin;
	public final Long noCtbMax;
	public final RegDate dateExclureDecede;
	public final int nbThreads;

	// Données de processing
	public int nbCtbsTotal;
	public final List<Long> ctbsAvecDiGeneree = new LinkedList<>();
	public final List<Long> ctbsIndigents = new LinkedList<>();
	public final List<Ignore> ctbsIgnores = new LinkedList<>();
	public final List<Erreur> ctbsEnErrors = new LinkedList<>();
	public boolean interrompu;

	public AbstractEnvoiDIsPPResults(int annee, CategorieEnvoiDIPP categorie, RegDate dateTraitement, int nbMax, @Nullable Long noCtbMin, @Nullable Long noCtbMax, @Nullable RegDate dateExclureDecede,
	                                 int nbThreads, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.annee = annee;
		this.categorie = categorie;
		this.dateTraitement = dateTraitement;
		this.nbMax = nbMax;
		this.noCtbMin = noCtbMin;
		this.noCtbMax = noCtbMax;
		this.dateExclureDecede = dateExclureDecede;
		this.nbThreads = nbThreads;
	}

	public void addDeclarationTraitee(Long noCtb) {
		ctbsAvecDiGeneree.add(noCtb);
	}

	public void addCtbIndigent(Long noCtb) {
		ctbsIndigents.add(noCtb);
	}

	@Override
	public void addErrorException(Long idCtb, Exception e) {
		ctbsEnErrors.add(new Erreur(idCtb, null, ErreurType.EXCEPTION, e.getMessage(), getNom(idCtb)));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, e.getMessage(), getNom(ctb.getNumero())));
	}

	public void addErrorDICollision(Contribuable ctb, RegDate dateDebut, RegDate dateFin, String details) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.COLLISION_DI, details, getNom(ctb.getNumero())));
	}

	public void addErrorForGestionNul(Contribuable ctb, @Nullable RegDate dateDebut, @Nullable RegDate dateFin, String details) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.FOR_GESTION_NUL, details, getNom(ctb.getNumero())));
	}

	public void addIgnoreDIDejaExistante(Contribuable ctb, RegDate dateDebut, RegDate dateFin) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DI_DEJA_EXISTANTE, null, getNom(ctb.getNumero())));
	}

	public void addIgnoreCtbExclu(Contribuable ctb, RegDate dateLimiteExclusion) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.CTB_EXCLU, "Date limite de l'exclusion = "
				+ RegDateHelper.dateToDisplayString(dateLimiteExclusion), getNom(ctb.getNumero())));
	}

	public void addIgnoreCtbExcluDecede(Contribuable ctb) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.CTB_EXCLU, "Décédé en fin d'année", getNom(ctb.getNumero())));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addAll(R rapport) {
		if (rapport != null) {
			this.nbCtbsTotal += rapport.nbCtbsTotal;
			this.ctbsAvecDiGeneree.addAll(rapport.ctbsAvecDiGeneree);
			this.ctbsIndigents.addAll(rapport.ctbsIndigents);
			this.ctbsEnErrors.addAll(rapport.ctbsEnErrors);
			this.ctbsIgnores.addAll(rapport.ctbsIgnores);
		}
	}
}
