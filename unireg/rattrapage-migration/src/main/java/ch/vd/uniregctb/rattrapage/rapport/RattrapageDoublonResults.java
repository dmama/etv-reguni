package ch.vd.uniregctb.rattrapage.rapport;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public class RattrapageDoublonResults extends JobResults {

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
	public int nbHorsMigration=0;
	public int nbErrors=0;
	public int nbConjoint=0;




	public final List<String> listeResultats = new ArrayList<String>();
	public final List<String> listeFor = new ArrayList<String>();
	public final List<String> listeHorsMigrationIs = new ArrayList<String>();
	public final List<String> listeError = new ArrayList<String>();
	public final List<String> listeConjoint = new ArrayList<String>();

	public boolean interrompu;

	public RattrapageDoublonResults() {
		// TODO Auto-generated constructor stub
	}

	public RattrapageDoublonResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}
	public RattrapageDoublonResults(int nbCtbCharges) {
		this.nombreCtbCharges = nbCtbCharges;
	}




	public void add(RattrapageDoublonResults rapport) {

		nbCtbsTotal += rapport.nbCtbsTotal;
		nbCtbFors += rapport.nbCtbFors;
		nbHorsMigration +=rapport.nbHorsMigration;
		nbConjoint += rapport.nbConjoint;

		listeError.addAll(rapport.listeError);
		listeResultats.addAll(rapport.listeResultats);
		listeFor.addAll(rapport.listeFor);
		listeHorsMigrationIs.addAll(rapport.listeHorsMigrationIs);
		listeConjoint.addAll(rapport.listeConjoint);

	}

	public void addResultat(String message){
		++nbCtbsTotal;
		listeResultats.add(message);
	}
	public void addFor(String message){
		++nbCtbFors;
		listeFor.add(message);
	}

	public void addHorsMigrationIs(String message){
		++nbHorsMigration;
		listeHorsMigrationIs.add(message);
	}

	public void addError(String message) {
		++nbErrors;
		listeError.add(message);

	}

	public void addConjoint(String message) {
		++nbConjoint;
		listeConjoint.add(message);

	}


}
