package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public class RapprocherCtbResults extends JobResults {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), // ------------------------------------------
		CTB_INCONNU("Le contribuable est inconnu");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	//Données en entrée
	public RegDate dateTraitement;

	// Données de processing
	public int nbCtbsTotal=0;
	public int nbIndividuTrouvesExact=0;
	public int nbIndividuTrouvesSaufDateNaissance=0;
	public int nbIndividuTrouvesSansCorrespondance=0;
	public int nbCtbInconnu=0;
	public int nbIndviduInconnu=0;
	public int nbPlusDeDeuxIndividu=0;

	public List<Erreur> ctbsEnErrors = new ArrayList<Erreur>();

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(long noCtb, Integer officeImpotID, ErreurType raison, String details) {
			super(noCtb, officeImpotID, details);

			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public final List<ProprietaireRapproche> listeRapproche = new ArrayList<ProprietaireRapproche>();

	public boolean interrompu;

	public RapprocherCtbResults() {
		// TODO Auto-generated constructor stub
	}

	public RapprocherCtbResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public void addErrorException(Long idCtb, Exception e) {
		++nbCtbsTotal;
		ctbsEnErrors.add(new Erreur(idCtb, null, ErreurType.EXCEPTION, e.getMessage()));

	}


	public void add(RapprocherCtbResults rapport) {

		nbCtbsTotal += rapport.nbCtbsTotal;
		nbIndividuTrouvesExact += rapport.nbIndividuTrouvesExact;
		nbIndividuTrouvesSaufDateNaissance +=rapport.nbIndividuTrouvesSaufDateNaissance;
		nbIndividuTrouvesSansCorrespondance += rapport.nbIndividuTrouvesSansCorrespondance;
		nbCtbInconnu += rapport.nbCtbInconnu;
		nbIndviduInconnu += rapport.nbIndviduInconnu;
		nbPlusDeDeuxIndividu += rapport.nbPlusDeDeuxIndividu;

		ctbsEnErrors.addAll(rapport.ctbsEnErrors);
		listeRapproche.addAll(rapport.listeRapproche);

	}

	public void addProprietaireRapproche(ProprietaireRapproche proprioRapproche){
		++nbCtbsTotal;
		listeRapproche.add(proprioRapproche);
	}

	public void incrementNbIndividuTrouvesExact(){
		++nbIndividuTrouvesExact;
	}

	public void incrementNbIndividuTrouvesSaufDateNaissance(){
		++nbIndividuTrouvesSaufDateNaissance;
	}
	public void incrementNbIndividuTrouvesSansCorrespondance(){
		++nbIndividuTrouvesSansCorrespondance;
	}

	public void incrementNbCtbInconnu(){
		++nbCtbInconnu;
	}

	public void incrementNbIndviduInconnu(){
		++nbIndviduInconnu;
	}

	public void incrementNbPlusDeDeuxIndividu(){
		++nbPlusDeDeuxIndividu;

	}

}
