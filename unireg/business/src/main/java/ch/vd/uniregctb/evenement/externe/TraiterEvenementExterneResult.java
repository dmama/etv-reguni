package ch.vd.uniregctb.evenement.externe;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public class TraiterEvenementExterneResult  extends JobResults<Long, TraiterEvenementExterneResult> {

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public boolean isInterrompu() {
		return interrompu;  
	}


	public static class InfoEvenement {
		public final Long id;
		public final Long numeroTiers;



		public InfoEvenement(Long id, Long numeroTiers) {
			this.id = id;

			this.numeroTiers = numeroTiers;
		}


	}


	public static class Erreur  {

		public Long evenementId;
		public final String raison;

		public Erreur(Long evenementId, String raison) {

			this.raison = raison;
			this.evenementId = evenementId;
		}


	}

	public static class Traite extends InfoEvenement {
		public final String action;


		public Traite(Long id, Long numeroTiers, String s) {
			super(id, numeroTiers);
			this.action = s;
		}


	}

	public static class Ignores extends InfoEvenement {
		public final String raison = "Cible de l'evenement externe déjà traitée ";

		public Ignores(Long id, Long numeroTiers) {
			super(id, numeroTiers);
		}
	}

	public final RegDate dateTraitement;
	public int nbEvenementTotal;
	public List<Traite> traites = new ArrayList<Traite>();
	public List<Ignores> ignores = new ArrayList<Ignores>();
	public List<Erreur> erreurs = new ArrayList<Erreur>();
	public boolean interrompu;

	public TraiterEvenementExterneResult(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public void addErrorException(Long element, Exception e) {
			erreurs.add(new Erreur( element, e.getMessage()));
	}

	public void addAll(TraiterEvenementExterneResult right) {
	this.nbEvenementTotal += right.nbEvenementTotal;
		this.traites.addAll(right.traites);
		this.ignores.addAll(right.ignores);
		this.erreurs.addAll(right.erreurs);
	}

	public void addTraite(EvenementExterne event){
		traites.add(new Traite(event.getId(),event.getTiers().getNumero(),event.getMessage()));
	}
	public void addIgnores(EvenementExterne event){
		ignores.add(new Ignores(event.getId(),event.getTiers().getNumero()));
	}
}
