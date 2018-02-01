package ch.vd.unireg.declaration.ordinaire.common;

import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;

/**
 * Contient les données brutes permettant de générer le document de rapport de l'exécution du processeur.
 */
public class DemandeDelaiCollectiveResults extends JobResults<Long, DemandeDelaiCollectiveResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), // -----------------------------------------------------------------
		CTB_INCONNU("Le contribuable spécifié est inconnu."), // ---------------------------------------------
		CONTRIBUABLE_SANS_DI("Le contribuable ne possède pas de déclaration"), // ----------------------------
		DECL_ANNULEE("La déclaration du contribuable est annulée"), // -----------------------------------------
		DECL_RETOURNEE("La déclaration a déjà été retournée"), // ----------------------------------------------
		DECL_SOMMEE("Une sommation a déjà été émise"), // ----------------------------------------------------
		DECL_RAPPELEE("Un rappel a déjà été émis"), // ----------------------------------------------------
		DECL_SUSPENDUE("La déclaration est suspendue"), // ----------------------------------------------------
		DECL_ECHUE("La déclaration est déjà échue");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		DELAI_DEJA_SUPERIEUR("Le délai accordé est supérieur à celui spécifié");

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
	public final List<Traite> traites = new LinkedList<>();
	public final List<Ignore> ignores = new LinkedList<>();
	public final List<Erreur> errors = new LinkedList<>();
	public boolean interrompu;

	public DemandeDelaiCollectiveResults(int annee, RegDate dateDelai, List<Long> ctbsIds, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.annee = annee;
		this.dateDelai = dateDelai;
		this.ctbsIds = ctbsIds;
		this.dateTraitement = dateTraitement;
	}

	public void addDeclarationTraitee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		traites.add(new Traite(ctb.getNumero(), di.getId()));
	}

	@Override
	public void addErrorException(Long idCtb, Exception e) {
		errors.add(new Erreur(idCtb, null, ErreurType.EXCEPTION, e.getMessage(), getNom(idCtb)));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, e.getMessage(), getNom(ctb.getNumero())));
	}

	public void addErrorCtbSansDI(Contribuable ctb) {
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.CONTRIBUABLE_SANS_DI, null, getNom(ctb.getNumero())));
	}

	public void addErrorDeclarationAnnulee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DECL_ANNULEE, buildDeclarationDetails(di), getNom(ctb.getNumero())));
	}

	public void addErrorDeclarationRetournee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DECL_RETOURNEE, buildDeclarationDetails(di), getNom(ctb.getNumero())));
	}

	public void addErrorDeclarationSommee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DECL_SOMMEE, buildDeclarationDetails(di), getNom(ctb.getNumero())));
	}

	public void addErrorDeclarationEchue(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DECL_ECHUE, buildDeclarationDetails(di), getNom(ctb.getNumero())));
	}

	public void addErrorDeclarationRappelee(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DECL_ECHUE, buildDeclarationDetails(di), getNom(ctb.getNumero())));
	}

	public void addErrorDeclarationSuspendue(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		errors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.DECL_ECHUE, buildDeclarationDetails(di), getNom(ctb.getNumero())));
	}

	public void addIgnoreDelaiSuperieur(Declaration di) {
		final Contribuable ctb = (Contribuable) di.getTiers();
		ignores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DELAI_DEJA_SUPERIEUR, buildDeclarationDetails(di), getNom(ctb.getNumero())));
	}

	public void addErrorCtbInconnu(Long id) {
		errors.add(new Erreur(id, null, ErreurType.CTB_INCONNU, null, getNom(id)));
	}

	@Override
	public void addAll(DemandeDelaiCollectiveResults right) {
		this.nbCtbsTotal += right.nbCtbsTotal;
		this.traites.addAll(right.traites);
		this.ignores.addAll(right.ignores);
		this.errors.addAll(right.errors);
	}

	private static String buildDeclarationDetails(Declaration di) {
		return "Déclaration id=" + di.getId() + " (du " + RegDateHelper.dateToDisplayString(di.getDateDebut()) + " au "
				+ RegDateHelper.dateToDisplayString(di.getDateFin()) + ')';
	}
}
