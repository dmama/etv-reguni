package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;

public class EnvoiSommationLRsResults extends JobResults<Long, EnvoiSommationLRsResults> {

	public static class Traite extends Info {

		public final RegDate dateDebutLR;
		
		public Traite(long noCtb, RegDate dateDebutLR) {
			super(noCtb, null, dateDebutLR.toString());
			this.dateDebutLR = dateDebutLR;
		}

		@Override
		public String getDescriptionRaison() {
			return "LR du " + dateDebutLR.toString() + " a été sommée";
		}
	}
	
	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION),
		ROLLBACK("Le traitement du lot a échoué et a été rollbacké");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}
	
	public static class Erreur extends Info {
		public final ErreurType raison;
		public final RegDate dateDebutLR;
		
		public Erreur(long noCtb, RegDate dateDebutLR, ErreurType raison, String details) {
			super(noCtb, null, details);
			this.raison = raison;
			this.dateDebutLR = dateDebutLR;
		}

		@Override
		public String getDescriptionRaison() {
			if (dateDebutLR != null)
				return raison.description + " pour la LR du " + dateDebutLR;
			else 
				return raison.description;
		}
	}
	
	public final RegDate dateTraitement;
	public int nbLRsTotal;//nombre de LR analysé
	public List<Traite> LRSommees = new ArrayList<Traite>();//LR sommée
	public List<Erreur> SommationLREnErrors = new ArrayList<Erreur>();//sommation LR KO
	public boolean interrompu;
	
	public EnvoiSommationLRsResults(RegDate dateTrait) {
		this.dateTraitement = dateTrait;
	}
	
	public void addAll(EnvoiSommationLRsResults right) {
		this.nbLRsTotal += right.nbLRsTotal;
		this.LRSommees.addAll(right.LRSommees);
		this.SommationLREnErrors.addAll(right.SommationLREnErrors);
	}

	public void addErrorException(Long element, Exception e) {
		addOnCommitException(element, e);
	}

	public void addLRSommee(DebiteurPrestationImposable dpi, DeclarationImpotSource lr) {
		LRSommees.add(new Traite(dpi.getNumero(), lr.getDateDebut()));
	}
	
	public void addError(DebiteurPrestationImposable dpi, DeclarationImpotSource lr, Exception e) {
		SommationLREnErrors.add(new Erreur(dpi.getNumero(), lr.getDateDebut(), ErreurType.EXCEPTION, e.getMessage()));
	}
	
	public void addOnCommitException(Long dpiId, Exception e) {
		SommationLREnErrors.add(new Erreur(dpiId, null, ErreurType.ROLLBACK, e.getMessage()));
	}
	
}
