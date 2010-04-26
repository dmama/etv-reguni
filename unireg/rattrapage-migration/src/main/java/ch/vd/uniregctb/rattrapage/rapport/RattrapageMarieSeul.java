package ch.vd.uniregctb.rattrapage.rapport;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public class RattrapageMarieSeul extends JobResults {

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
	public int nombreCtbCharges=0;

	// Données de processing
	public int nbCtbsTotal=0;
	public int nbErrors=0;
	public int nbConjoint=0;
	public int nbCtbForsSupprimes =0;
	public int nbCtbForsCrees =0;



	public final List<String> listeResultats = new ArrayList<String>();
	public final List<String> listeForSupprimes = new ArrayList<String>();
	public final List<String> listeForCrees = new ArrayList<String>();
	public final List<String> listeError = new ArrayList<String>();
	public final List<String> listeConjoint = new ArrayList<String>();

	public boolean interrompu;

	public RattrapageMarieSeul() {
		// TODO Auto-generated constructor stub
	}

	public RattrapageMarieSeul(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}
	public RattrapageMarieSeul(int nbCtbCharges) {
		this.nombreCtbCharges = nbCtbCharges;
	}




	public void add(RattrapageMarieSeul rapport) {

		nbCtbsTotal += rapport.nbCtbsTotal;
		nbConjoint += rapport.nbConjoint;
		nbCtbForsSupprimes += rapport.nbCtbForsSupprimes;
		nbCtbForsCrees += rapport.nbCtbForsCrees;
		nbErrors+= rapport.nbErrors;

		listeError.addAll(rapport.listeError);
		listeResultats.addAll(rapport.listeResultats);
		listeConjoint.addAll(rapport.listeConjoint);
		listeForSupprimes.addAll(rapport.listeForSupprimes);
		listeForCrees.addAll(rapport.listeForCrees);

	}

	public void addResultat(String message){
		++nbCtbsTotal;
		listeResultats.add(message);
	}

	public void addError(String message) {
		++nbErrors;
		listeError.add(message);

	}

	public void addConjoint(String message) {
		++nbConjoint;
		listeConjoint.add(message);

	}
	public void addForSupprimes(String message){
		++nbCtbForsSupprimes;
		listeForSupprimes.add(message);
	}

		public void addForCrees(String message){
		++nbCtbForsCrees;
		listeForCrees.add(message);
	}


}