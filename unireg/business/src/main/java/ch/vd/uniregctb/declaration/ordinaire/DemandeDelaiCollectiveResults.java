package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Contient les données brutes permettant de générer le document de rapport de l'exécution du processeur.
 */
public class DemandeDelaiCollectiveResults extends JobResults<Long, DemandeDelaiCollectiveResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), // -----------------------------------------------------------------
		CTB_INCONNU("Le contribuable spécifié est inconnu."), // ---------------------------------------------
		CONTRIBUABLE_SANS_DI("Le contribuable ne possède pas de déclaration"), // ----------------------------
		DI_ANNULEE("La déclaration du contribuable est annulée"), // -----------------------------------------
		DI_RETOURNEE("La déclaration a déjà été retournée"), // ----------------------------------------------
		DI_SOMMEE("La déclaration a déjà été sommée"), // ----------------------------------------------------
		DI_ECHUE("La déclaration est déjà échue");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		DELAI_DEJA_SUPERIEUR("Le délai accordé est supérieur à celui spécifié");

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

	public static class Traite {
		public final long ctbId;
		public final long diId;

		public Traite(long ctbId, long diId) {
			this.ctbId = ctbId;
			this.diId = diId;
		}
	}

	// paramètre d'entrée
	public final int annee;
	public final RegDate dateDelai;
	public final RegDate dateTraitement;
	public final List<Long> ctbsIds;

	// données de sortie
	public int nbCtbsTotal;
	public final List<Traite> traites = new ArrayList<Traite>();
	public final List<Ignore> ignores = new ArrayList<Ignore>();
	public final List<Erreur> errors = new ArrayList<Erreur>();
	public boolean interrompu;

	public DemandeDelaiCollectiveResults(int annee, RegDate dateDelai, List<Long> ctbsIds, RegDate dateTraitement) {
		this.annee = annee;
		this.dateDelai = dateDelai;
		this.ctbsIds = ctbsIds;
		this.dateTraitement = dateTraitement;
	}

	public void addDeclarationTraitee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		traites.add(new Traite(ctb.getNumero(), di.getId()));
	}

	public void addErrorException(Long idCtb, Exception e) {
		errors.add(new Erreur(idCtb, null, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorCtbSansDI(Contribuable ctb) {
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.CONTRIBUABLE_SANS_DI, null));
	}

	public void addErrorDIAnnulee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DI_ANNULEE, buildDeclarationDetails(di)));
	}

	public void addErrorDIRetournee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DI_RETOURNEE, buildDeclarationDetails(di)));
	}

	public void addErrorDISommee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DI_SOMMEE, buildDeclarationDetails(di)));
	}

	public void addErrorDIEchue(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DI_ECHUE, buildDeclarationDetails(di)));
	}

	public void addIgnoreDIDelaiSuperieur(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DELAI_DEJA_SUPERIEUR, buildDeclarationDetails(di)));
	}

	public void addErrorCtbInconnu(Long id) {
		errors.add(new Erreur(id, null, ErreurType.CTB_INCONNU, null));
	}

	public void addAll(DemandeDelaiCollectiveResults right) {
		this.nbCtbsTotal += right.nbCtbsTotal;
		this.traites.addAll(right.traites);
		this.ignores.addAll(right.ignores);
		this.errors.addAll(right.errors);
	}

	private static String buildDeclarationDetails(Declaration di) {
		return "Déclaration id=" + di.getId() + " (du " + RegDateHelper.dateToDisplayString(di.getDateDebut()) + " au "
				+ RegDateHelper.dateToDisplayString(di.getDateFin()) + ")";
	}
}
