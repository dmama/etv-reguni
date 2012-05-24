package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;

public class IdentifierContribuableResults extends JobResults<Long, IdentifierContribuableResults> {
	public RegDate getDateTraitement() {
		return dateTraitement;
	}


	public static class InfoMessage {
		public final String businessID;
		public final String nom;
		public final String prenom;


		public InfoMessage(String businessID, String nom, String prenom) {
			this.businessID = businessID;
			this.nom = nom;
			this.prenom = prenom;
		}


	}


	public static class Erreur  {

		public final Long messageId;
		public final String raison;

		public Erreur(Long messageId, String raison) {

			this.raison = raison;
			this.messageId = messageId;
		}


	}

	public static class Identifie extends InfoMessage {
		public final Long noCtb;
		public final Long noCtbMenage;

		public Identifie(String businessID, String nom, String prenom, Long noCtb, Long noCtbMenage) {
			super(businessID, nom, prenom);
			this.noCtb = noCtb;
			this.noCtbMenage = noCtbMenage;
		}
	}

	public static class NonIdentifie extends InfoMessage {
		public final String raison = "Identification automatique sans resultat";

		public NonIdentifie(String businessID, String nom, String prenom) {
			super(businessID, nom, prenom);
		}
	}

	public final RegDate dateTraitement;
	public int nbMessagesTotal;
	public final List<Identifie> identifies = new ArrayList<Identifie>();
	public final List<NonIdentifie> nonIdentifies = new ArrayList<NonIdentifie>();
	public final List<Erreur> erreurs = new ArrayList<Erreur>();
	public boolean interrompu;

	public IdentifierContribuableResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new Erreur( element, e.getMessage()));
	}

	@Override
	public void addAll(IdentifierContribuableResults right) {
		this.nbMessagesTotal += right.nbMessagesTotal;
		this.identifies.addAll(right.identifies);
		this.nonIdentifies.addAll(right.nonIdentifies);
		this.erreurs.addAll(right.erreurs);
	}

	public void addIdentifies(IdentificationContribuable message) {
		final String businessId = message.getHeader().getBusinessId();
		final String nom = message.getDemande().getPersonne().getNom();
		final String prenoms = message.getDemande().getPersonne().getPrenoms();
		final Long noContribuable = message.getReponse().getNoContribuable();
		final Long noMenageCommun = message.getReponse().getNoMenageCommun();
		identifies.add(new Identifie(businessId, nom, prenoms, noContribuable, noMenageCommun));

	}

	public void addNonIdentifies(IdentificationContribuable message) {

		final String businessId = message.getHeader().getBusinessId();
		final String nom = message.getDemande().getPersonne()== null? null:message.getDemande().getPersonne().getNom();
		final String prenoms = message.getDemande().getPersonne()== null? null:message.getDemande().getPersonne().getPrenoms();
		nonIdentifies.add(new NonIdentifie(businessId, nom, prenoms));
	}

public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}
	


}
