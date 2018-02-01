package ch.vd.unireg.declaration.snc;

import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TacheAnnulationQuestionnaireSNC;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.unireg.tiers.TiersService;

public class DeterminationQuestionnairesSNCResults extends JobResults<Long, DeterminationQuestionnairesSNCResults> {

	private static final int OIPM = ServiceInfrastructureService.noOIPM;

	public final int periodeFiscale;
	public final RegDate dateTraitement;
	public final int nbThreads;

	private boolean interrompu = false;
	private int nbContribuablesInspectes = 0;
	private final List<Ignore> ignores = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();
	private final List<Traite> traites = new LinkedList<>();

	public enum IgnoreType {
		QUESTIONNAIRE_DEJA_PRESENT("Questionnaire déjà présent"),
		TACHE_ENVOI_DEJA_PRESENTE("Tâche d'émission déjà présente"),
		AUCUN_QUESTIONNAIRE_REQUIS("Aucun questionnaire requis"),
		TACHE_ANNULATION_DEJA_PRESENTE("Tâche d'annulation déjà présente");

		public final String description;

		IgnoreType(String description) {
			this.description = description;
		}
	}

	public static class Ignore extends Info {

		final IgnoreType type;

		public Ignore(long noCtb, IgnoreType type, String nomCtb) {
			super(noCtb, OIPM, null, nomCtb);
			this.type = type;
		}

		@Override
		public String getDescriptionRaison() {
			return type.description;
		}
	}

	public enum ErreurType {
		CTB_INVALIDE("Le contribuable n'est pas valide"),
		EXCEPTION(EXCEPTION_DESCRIPTION);

		public final String description;

		ErreurType(String description) {
			this.description = description;
		}
	}

	public static class Erreur extends Info {

		final ErreurType type;

		public Erreur(long noCtb, String nomCtb, ErreurType type, String details) {
			super(noCtb, OIPM, details, nomCtb);
			this.type = type;
		}

		@Override
		public String getDescriptionRaison() {
			return type.description;
		}
	}

	public enum TraiteType {
		EMISSION_CREE("Tâche d'envoi de questionnaire créée"),
		EMISSION_ANNULEE("Tâche d'envoi de questionnaire annulée"),
		ANNULATION_CREE("Tâche d'annulation de questionnaire créée"),
		ANNULATION_ANNULEE("Tâche d'annulation de questionnaire annulée");

		public final String description;

		TraiteType(String description) {
			this.description = description;
		}
	}

	public static class Traite extends Info {
		final TraiteType type;

		public Traite(long noCtb, TraiteType type, String nomCtb, String details) {
			super(noCtb, OIPM, details, nomCtb);
			this.type = type;
		}

		@Override
		public String getDescriptionRaison() {
			return type.description;
		}
	}

	public DeterminationQuestionnairesSNCResults(int periodeFiscale, RegDate dateTraitement, int nbThreads, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.periodeFiscale = periodeFiscale;
		this.dateTraitement = dateTraitement;
		this.nbThreads = nbThreads;
	}

	public void setInterrupted() {
		interrompu = true;
	}

	public boolean wasInterrupted() {
		return interrompu;
	}

	public int getNbContribuablesInspectes() {
		return nbContribuablesInspectes;
	}

	public List<Ignore> getIgnores() {
		return ignores;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public List<Traite> getTraites() {
		return traites;
	}

	@Override
	public void end() {
		final CtbComparator<Info> comparator = new CtbComparator<>();
		ignores.sort(comparator);
		erreurs.sort(comparator);
		traites.sort(comparator);
		super.end();
	}

	@Override
	public void addAll(DeterminationQuestionnairesSNCResults right) {
		nbContribuablesInspectes += right.nbContribuablesInspectes;
		ignores.addAll(right.ignores);
		erreurs.addAll(right.erreurs);
		traites.addAll(right.traites);
	}

	private static final StringRenderer<TacheEnvoiQuestionnaireSNC> ENVOI_RENDERER = tache -> String.format("du %s au %s (%s)",
	                                                                                                        RegDateHelper.dateToDisplayString(tache.getDateDebut()),
	                                                                                                        RegDateHelper.dateToDisplayString(tache.getDateFin()),
	                                                                                                        tache.getCategorieEntreprise());

	@Override
	public void addErrorException(Long element, Exception e) {
		incrementNbOfInspectedEntreprises();
		erreurs.add(new Erreur(element == null ? 0 : element, getNom(element), ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorCtbInvalide(Entreprise entreprise) {
		erreurs.add(new Erreur(entreprise.getNumero(), getNom(entreprise), ErreurType.CTB_INVALIDE, null));
	}

	public void addTraiteAnnulationTacheAnnulation(Entreprise entreprise, TacheAnnulationQuestionnaireSNC tacheAnnulee) {
		traites.add(new Traite(entreprise.getNumero(), TraiteType.ANNULATION_ANNULEE, getNom(entreprise), null));
	}

	public void addTraiteAnnulationTacheEnvoi(Entreprise entreprise, TacheEnvoiQuestionnaireSNC tacheAnnulee) {
		traites.add(new Traite(entreprise.getNumero(), TraiteType.EMISSION_ANNULEE, getNom(entreprise), ENVOI_RENDERER.toString(tacheAnnulee)));
	}

	public void addTraiteNouvelleTacheEnvoi(Entreprise entreprise, TacheEnvoiQuestionnaireSNC tache) {
		traites.add(new Traite(entreprise.getNumero(), TraiteType.EMISSION_CREE, getNom(entreprise), ENVOI_RENDERER.toString(tache)));
	}

	public void addTraiteNouvelleTacheAnnulation(Entreprise entreprise, TacheAnnulationQuestionnaireSNC tache) {
		traites.add(new Traite(entreprise.getNumero(), TraiteType.ANNULATION_CREE, getNom(entreprise), DateRangeHelper.toDisplayString(tache.getDeclaration())));
	}

	public void addIgnoreQuestionnaireDejaPresent(Entreprise entreprise) {
		ignores.add(new Ignore(entreprise.getNumero(), IgnoreType.QUESTIONNAIRE_DEJA_PRESENT, getNom(entreprise)));
	}

	public void addIgnoreAucunQuestionnaireRequis(Entreprise entreprise) {
		ignores.add(new Ignore(entreprise.getNumero(), IgnoreType.AUCUN_QUESTIONNAIRE_REQUIS, getNom(entreprise)));
	}

	public void addIgnoreTacheEnvoiDejaPresente(Entreprise entreprise) {
		ignores.add(new Ignore(entreprise.getNumero(), IgnoreType.TACHE_ENVOI_DEJA_PRESENTE, getNom(entreprise)));
	}

	public void addIgnoreTacheAnnulationDejaPresente(Entreprise entreprise) {
		ignores.add(new Ignore(entreprise.getNumero(), IgnoreType.TACHE_ANNULATION_DEJA_PRESENTE, getNom(entreprise)));
	}

	public void incrementNbOfInspectedEntreprises() {
		++nbContribuablesInspectes;
	}
}
