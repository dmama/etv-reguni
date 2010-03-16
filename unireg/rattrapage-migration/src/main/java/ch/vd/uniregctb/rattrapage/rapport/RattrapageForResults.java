package ch.vd.uniregctb.rattrapage.rapport;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public class RattrapageForResults extends JobResults {

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
	public int nbCtbFors=0;
	public int nbSuissePermisC=0;
	public int nbErrors=0;
	public int nbDecede=0;
	public int nbHorsSuisse=0;
	public int nbSansAdresse=0;




	public final List<String> listeResultats = new ArrayList<String>();
	public final List<String> listeFor = new ArrayList<String>();
	public final List<String> listeSuissePermisC = new ArrayList<String>();
	public final List<String> listeError = new ArrayList<String>();
	public final List<String> listeDecede = new ArrayList<String>();
	public final List<String> listeHorsSuisse = new ArrayList<String>();
	public final List<String> listeSansAdresse = new ArrayList<String>();

	public boolean interrompu;

	public RattrapageForResults() {
		// TODO Auto-generated constructor stub
	}

	public RattrapageForResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}
	public RattrapageForResults(int nbCtbCharges) {
		this.nombreCtbCharges = nbCtbCharges;
	}




	public void add(RattrapageForResults rapport) {

		nbCtbsTotal += rapport.nbCtbsTotal;
		nbCtbFors += rapport.nbCtbFors;
		nbSuissePermisC +=rapport.nbSuissePermisC;
		nbDecede += rapport.nbDecede;
		nbHorsSuisse +=rapport.nbHorsSuisse;
		nbSansAdresse +=rapport.nbSansAdresse;

		listeError.addAll(rapport.listeError);
		listeResultats.addAll(rapport.listeResultats);
		listeFor.addAll(rapport.listeFor);
		listeSuissePermisC.addAll(rapport.listeSuissePermisC);
		listeDecede.addAll(rapport.listeDecede);
		listeHorsSuisse.addAll(rapport.listeHorsSuisse);
		listeSansAdresse.addAll(rapport.listeSansAdresse);

	}

	public void addResultat(String message){
		++nbCtbsTotal;
		listeResultats.add(message);
	}
	public void addFor(String message){
		++nbCtbFors;
		listeFor.add(message);
	}

	public void addSuissePermisC(String message){
		++nbSuissePermisC;
		listeSuissePermisC.add(message);

	}

	public void addDecede(String message) {
		++nbDecede;
		listeDecede.add(message);

	}

	public void addHorsSuisse(String message){
		++nbHorsSuisse;
		listeHorsSuisse.add(message);

	}

	public void addSansAdresse(String message) {
		++nbSansAdresse;
		listeSansAdresse.add(message);

	}


	public void addError(String message) {
		++nbErrors;
		listeError.add(message);

	}



}
