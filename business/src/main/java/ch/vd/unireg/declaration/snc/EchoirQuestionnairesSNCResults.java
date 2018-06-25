package ch.vd.unireg.declaration.snc;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

/**
 * Résultats bruts du processeur d'échéances des questionnaires SNC.
 */
public class EchoirQuestionnairesSNCResults extends JobResults<IdentifiantDeclaration, EchoirQuestionnairesSNCResults> {

	protected EchoirQuestionnairesSNCResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	// Paramètres d'entrée
	public final RegDate dateTraitement;

	// Données de processing
	private int total;
	private final List<Traite> traites = new ArrayList<>();
	private final List<Erreur> erreurs = new ArrayList<>();
	private boolean interrompu;

	@Override
	public void addErrorException(IdentifiantDeclaration ident, Exception e) {
		++total;
		erreurs.add(new Erreur(ident.getNumeroTiers(), ident.getIdDeclaration(), ErreurType.EXCEPTION, e.getMessage(), getNom(ident.getNumeroTiers())));
	}

	public void addErrorEtatIncoherent(QuestionnaireSNC qsnc, String message) {
		++total;
		final Tiers tiers = qsnc.getTiers();
		erreurs.add(new Erreur(tiers.getNumero(), qsnc.getId(), ErreurType.ETAT_DECLARATION_INCOHERENT, message, getNom(tiers.getNumero())));
	}

	public void addErrorRappelNonEchu(QuestionnaireSNC qsnc, String message) {
		++total;
		final Tiers tiers = qsnc.getTiers();
		erreurs.add(new Erreur(tiers.getNumero(), qsnc.getId(), ErreurType.RAPPEL_NON_ECHU, message, getNom(tiers.getNumero())));
	}

	public void addQuestionnaireTraite(QuestionnaireSNC qsnc) {
		++total;
		final Tiers tiers = qsnc.getTiers();
		traites.add(new Traite(tiers.getNumero(), qsnc.getId(), qsnc.getDateDebut(), qsnc.getDateFin()));
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	/**
	 * @return le nombre de questionnaires SNC considérés
	 */
	public int getTotal() {
		return total;
	}

	public List<Traite> getTraites() {
		return traites;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	@Override
	public void addAll(EchoirQuestionnairesSNCResults right) {
		total += right.total;
		traites.addAll(right.traites);
		erreurs.addAll(right.erreurs);
	}

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), 
		ETAT_DECLARATION_INCOHERENT("L'état de la déclaration est incohérent"),
		RAPPEL_NON_ECHU("L'échéance du rappel n'est pas échue");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;
		public final long sncId;

		public Erreur(long noCtb, long sncId, ErreurType raison, String details, String nomCtb) {
			super(noCtb, null, details, nomCtb);
			this.raison = raison;
			this.sncId = sncId;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Traite {
		public final long sncId;
		public final long ctbId;
		public final RegDate dateDebut;
		public final RegDate dateFin;

		public Traite(long ctbId, long sncId, RegDate dateDebut, RegDate dateFin) {
			this.sncId = sncId;
			this.ctbId = ctbId;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}
	}

}
