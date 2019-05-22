package ch.vd.unireg.metier;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.tiers.TiersService;

public class PassageNouveauxRentiersSourciersEnMixteResults extends JobResults<Long, PassageNouveauxRentiersSourciersEnMixteResults> {

	public enum ErreurType {
		DATE_NAISSANCE_NULLE("le sourcier ne possède pas de date de naissance"),
		SEXE_NUL("le sexe du sourcier n'est pas renseigné"),
		DOMICILE_INCONNU("le domicile du contribuable est inconnu"),
		INFRA_EXCEPTION("le service infrastructure a retourné une exception"),
		ADRESSE_EXCEPTION("le service d'adresses a retourné une exception"),
		CIVIL_EXCEPTION("le service civil a retourné une exception"),
		INDIVIDU_INCONNU("l'individu associé à l'habitant n'existe pas"),
		UNKNOWN_EXCEPTION("une exception inconnue a été levée"),
		CONSTRAINT_VIOLATION_EXCEPTION("une exception de violation de contrainte base de données a été levée"),
		VALIDATION("le contribuable ne valide pas"),
		VALIDATION_APRES_OUVERTURE("le contribuable ne valide plus après l'ouverture de son for mixte"),
		INCOHERENCE_FOR_FISCAL("une incohérence avec les fors fiscaux a été détectée"),
		FOR_FISCAL_POSTERIEUR("un for a été ouvert après la date de rentier de la personne");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}

	}
	public static class Traite {

		public final Long noCtb;
		public final RegDate dateOuverture;

		public Traite(long noCtb, RegDate dateOuverture) {
			this.noCtb = noCtb;
			this.dateOuverture = dateOuverture;
		}
	}


	public static abstract class NonTraite {

		public final Long noCtb;
		public final ErreurType raison;
		public final String details;

		public NonTraite(Long noCtb, ErreurType raison, String details) {
			this.noCtb = noCtb;
			this.raison = raison;
			this.details = details;
		}
	}

	public static class Erreur extends NonTraite{


		public Erreur(Long noCtb, ErreurType raison, String details) {
			super(noCtb,raison,details);
		}
	}
	public static class Ignore extends NonTraite{


		public Ignore(Long noCtb, ErreurType raison, String details) {
			super(noCtb,raison,details);
		}
	}

	public final RegDate dateTraitement;

	private int nbSourciersTotal;
	public int nbSourciersTropJeunes;
	public int nbSourciersHorsSuisse;
	public int nbSourciersConjointsIgnores;

	public final List<Traite> sourciersConvertis = new LinkedList<>();
    public final List<NonTraite> sourciersEnErreurs = new LinkedList<>();
	public final List<NonTraite> sourciersIgnores = new LinkedList<>();

	public boolean interrompu;

	PassageNouveauxRentiersSourciersEnMixteResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	public int getNbSourciersTotal() {
		return nbSourciersTropJeunes + nbSourciersHorsSuisse + nbSourciersConjointsIgnores + sourciersConvertis.size() + sourciersEnErreurs.size() + sourciersIgnores.size();
	}

	@Override
	public void addAll(PassageNouveauxRentiersSourciersEnMixteResults right) {
		nbSourciersTotal += right.nbSourciersTotal;
		nbSourciersTropJeunes += right.nbSourciersTropJeunes;
		nbSourciersHorsSuisse += right.nbSourciersHorsSuisse;
		nbSourciersConjointsIgnores += right.nbSourciersConjointsIgnores;
		sourciersEnErreurs.addAll(right.sourciersEnErreurs);
		sourciersConvertis.addAll(right.sourciersConvertis);
		sourciersIgnores.addAll(right.sourciersIgnores);
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		if (e instanceof ValidationException) {
			sourciersEnErreurs.add(new Erreur(element, ErreurType.VALIDATION_APRES_OUVERTURE, e.getMessage()));
		}
		else if (e instanceof InfrastructureException) {
			sourciersEnErreurs.add(new Erreur(element, ErreurType.INFRA_EXCEPTION, e.getMessage()));
		}
		else if (e instanceof ConstraintViolationException) {
			sourciersEnErreurs.add(new Erreur(element, ErreurType.CONSTRAINT_VIOLATION_EXCEPTION, e.getMessage()));
		}
		else {
			sourciersEnErreurs.add(new Erreur(element, ErreurType.UNKNOWN_EXCEPTION, e.getMessage()));
		}
	}

	public void addPassageNouveauxRentiersSourciersEnMixteException(PassageNouveauxRentiersSourciersEnMixteException e) {

		switch (e.getType()){
		case DATE_NAISSANCE_NULLE:case DOMICILE_INCONNU:case FOR_FISCAL_POSTERIEUR:
			sourciersIgnores.add(new Ignore(e.getContribuable().getNumero(), e.getType(), e.getMessage()));
			break;
		default:
			sourciersEnErreurs.add(new Erreur(e.getContribuable().getNumero(), e.getType(), e.getMessage()));

		}

	}

	public void addUnknownException(Long numeroSourcier, Exception e) {
		if (e instanceof InfrastructureException) {
			sourciersEnErreurs.add(new Erreur(numeroSourcier, ErreurType.INFRA_EXCEPTION, e.getMessage()));
		}
		else if (e instanceof ConstraintViolationException) {
			sourciersEnErreurs.add(new Erreur(numeroSourcier, ErreurType.CONSTRAINT_VIOLATION_EXCEPTION, e.getMessage()));
		}
		else{
			sourciersEnErreurs.add(new Erreur(numeroSourcier, ErreurType.UNKNOWN_EXCEPTION, e.getMessage()));
		}
	}

	public void addSourcierConverti(Long numeroSourcier, RegDate dateOuvertureFor) {
		sourciersConvertis.add(new Traite(numeroSourcier, dateOuvertureFor));
	}
}
