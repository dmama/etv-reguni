package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsEnMasseProcessor.LotContribuables;
import ch.vd.uniregctb.tiers.Contribuable;

public class EnvoiAnnexeImmeubleResults<R extends EnvoiAnnexeImmeubleResults> extends JobResults<Long, R> {

	public static enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), // --------------------------------------------------------------
		ROLLBACK("Le traitement du lot a échoué et a été rollbacké"),

		FOR_GESTION_NUL("le contribuable ne possède pas de for de gestion");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static enum IgnoreType {
		CTB_NON_ASSUJETTI("le ctb n'est pas assujétti pour la période fiscale"), // -----
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

	// Paramètres d'entrée
	public final int annee;
	public final RegDate dateTraitement;
	public final String nomFichier;
	public final int nombreAnnexe;

	// Données de processing
	public int nbCtbsTotal;
	public final List<Long> ctbsTraites = new ArrayList<Long>();
	public final List<Ignore> ctbsIgnores = new ArrayList<Ignore>();
	public final List<Erreur> ctbsEnErrors = new ArrayList<Erreur>();
	public final List<Erreur> ctbsRollback = new ArrayList<Erreur>();
	public boolean interrompu;

	public EnvoiAnnexeImmeubleResults(int annee, RegDate dateTraitement, String nomFichier, int nombreAnnexe) {
		this.annee = annee;
		this.dateTraitement = dateTraitement;
		this.nomFichier = nomFichier;
		this.nombreAnnexe = nombreAnnexe;
	}

	public void addCtbTraites(Long noCtb) {
		ctbsTraites.add(noCtb);
	}


	@Override
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

	public void addErrorForGestionNul(Contribuable ctb, RegDate dateDebut, RegDate dateFin, String details) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.FOR_GESTION_NUL, details));
	}


	public void addIgnoreCtbNonAssujetti(Contribuable ctb, int periode) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.CTB_NON_ASSUJETTI,"Non assujetti pour la fin de période fiscale "+ periode));
	}



	@Override
	public void addAll(R rapport) {
		if (rapport != null) {
			this.nbCtbsTotal += rapport.nbCtbsTotal;
			this.ctbsTraites.addAll(rapport.ctbsTraites);
			this.ctbsEnErrors.addAll(rapport.ctbsEnErrors);
			this.ctbsIgnores.addAll(rapport.ctbsIgnores);
		}
	}
}
