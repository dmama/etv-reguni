package ch.vd.uniregctb.evenement.tutelle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Handler gérant la mise sous tutelle d'individu.
 *
 * @author Ludovic BERTIN
 */
public class TutelleHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		/* rien à faire ici, un seul événement unitaire pour le pupille */
	}

	@Override
	protected void validateSpecific(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		/*
		 * Cast de l'événement en Tutelle.
		 */
		Tutelle tutelle = (Tutelle) evenement;

		/*
		 * Un pupille ne peut être à la fois sous tutelle d'un tuteur et de l'office du tuteur général.
		 * Cas qui peut arriver suite à un bug coriace dans ILF1
		 */
		if  ( ( tutelle.getTuteur() != null) && ( tutelle.getTuteurGeneral() != null) ) {
			throw new EvenementCivilHandlerException("Un pupille ne peut être à la fois sous tutelle d'un tuteur et de l'office du tuteur général");
		}
	}

	@Override
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		final Tutelle tutelle = (Tutelle) evenement;

		// Récupération de la pupille
		final PersonnePhysique pupille = getHabitantOrThrowException(tutelle.getIndividu().getNoTechnique());
		Assert.notNull(pupille);

		// Récupération du tuteur. Dans certains cas, le tuteur n'est pas renseigné et c'est l'autorité tutelaire que le remplace.
		final PersonnePhysique tuteur;
		if (tutelle.getTuteur() != null) {
			tuteur = getHabitantOrThrowException(tutelle.getTuteur().getNoTechnique());
			Assert.notNull(tuteur);
		}
		else {
			tuteur = null;
		}

		// Récupération éventuelle de l'autorité tutelaire, c'est-à-dire l'office du tuteur général
		final CollectiviteAdministrative autoriteTutelaire;
		if (tutelle.getTuteurGeneral() != null ){
			autoriteTutelaire = getService().getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noTuteurGeneral);
			Assert.notNull(autoriteTutelaire);
		}
		else {
			autoriteTutelaire = null;
		}

		// La tutelle n'est rattachée ni à un tuteur personne physique, ni à l'Office du Tuteur Général ==> ERREUR
		if (tuteur == null && autoriteTutelaire == null) {
			throw new EvenementCivilHandlerException("La tutelle n'est rattachée ni à un tuteur ordinaire, ni à l'Office du Tuteur Général");
		}

		if (isTutelleExistante(pupille, tuteur, autoriteTutelaire, tutelle.getDate())) {
			Audit.warn(evenement.getNumeroEvenement(), "La tutelle existe déjà. Aucune action enteprise.");
		}
		else {
			if (autoriteTutelaire == null) {
				Audit.info(evenement.getNumeroEvenement(), "Le pupille est rattaché à un tuteur ordinaire");
			}
			else if (tuteur == null) {
				Audit.info(evenement.getNumeroEvenement(), "Le pupille est rattaché à l'OTG");
			}
			else {
				Audit.info(evenement.getNumeroEvenement(), "Le pupille est rattaché à un tuteur professionnel membre de l'OTG");
			}

			/*
			 * Création d'un rapport entre tiers
			 */
			RapportEntreTiers rapport = creeRepresentationLegale(tutelle.getDate(), pupille, tuteur, autoriteTutelaire, tutelle
					.getTypeTutelle());
			rapport = getTiersDAO().save(rapport);
			Audit.info(evenement.getNumeroEvenement(), "Création d'un rapport entre le pupille et son tuteur physique");
		}
	}

	/**
	 * @return <b>vrai</b> s'il existe déja une tutelle entre la pupille et le tuteur (ou l'autorité tutelaire) à la date spécifiée;
	 *         <b>faux</b> autrement.
	 */
	private static boolean isTutelleExistante(PersonnePhysique pupille, PersonnePhysique tuteur,
			CollectiviteAdministrative autoriteTutelaire, RegDate date) {
		final Set<RapportEntreTiers> rapports = pupille.getRapportsSujet();
		final Long numeroTuteur = (tuteur == null ? null : tuteur.getNumero());
		final Long numeroAutoriteTutelaire = (autoriteTutelaire == null ? null : autoriteTutelaire.getNumero());

		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (!rapport.isValidAt(date)) {
					continue;
				}
				if (TypeRapportEntreTiers.TUTELLE.equals(rapport.getType()) || TypeRapportEntreTiers.CURATELLE.equals(rapport.getType())
						|| TypeRapportEntreTiers.CONSEIL_LEGAL.equals(rapport.getType())) {
					if (numeroTuteur != null && rapport.getObjetId().equals(numeroTuteur)) {
						return true;
					}
					if (numeroAutoriteTutelaire != null && rapport.getObjetId().equals(numeroAutoriteTutelaire)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Relie le tiers et le pupille avec un objet RapportEntreTiers de type tutelle, curatelle ou conseil légal.
	 *
	 * @param dateEvenement
	 *            la date de la mise sous tutelle
	 * @param pupille
	 *            le pupille
	 * @param tuteur
	 *            le tuteur
	 * @param typeTutelle
	 *            le type de tutelle
	 */
	private RepresentationLegale creeRepresentationLegale(RegDate dateEvenement, PersonnePhysique pupille, PersonnePhysique tuteur, CollectiviteAdministrative autoriteTutelaire, TypeTutelle typeTutelle)  throws EvenementCivilHandlerException {

		final RepresentationLegale rapport;

		if (tuteur != null) {
			// établi une tutelle entre une pupille et son tuteur. Ce tuteur peut être un tuteur professionel auquel cas l'autorité tutelaire est renseigné.
			switch (typeTutelle) {
			case TUTELLE :
				rapport = new ch.vd.uniregctb.tiers.Tutelle(dateEvenement, null, pupille, tuteur, autoriteTutelaire);
				break;

			case CURATELLE :
				rapport = new Curatelle(dateEvenement, null, pupille, tuteur, autoriteTutelaire);
				break;

			case CONSEIL_LEGAL_CODE :
				rapport = new ConseilLegal(dateEvenement, null, pupille, tuteur, autoriteTutelaire);
				break;

			default :
				throw new EvenementCivilHandlerException("Ce type de tutelle n'est pas pris en charge : " + typeTutelle);
			}
		}
		else {
			// établi une tutelle entre une pupille et un autorité tutelaire, son indication d'une personne physique ayant le role de tuteur.
			switch (typeTutelle) {
			case TUTELLE :
				rapport = new ch.vd.uniregctb.tiers.Tutelle(dateEvenement, null, pupille, autoriteTutelaire);
				break;

			case CURATELLE :
				rapport = new Curatelle(dateEvenement, null, pupille, autoriteTutelaire);
				break;

			case CONSEIL_LEGAL_CODE :
				rapport = new ConseilLegal(dateEvenement, null, pupille, autoriteTutelaire);
				break;

			default :
				throw new EvenementCivilHandlerException("Ce type de tutelle n'est pas pris en charge : " + typeTutelle);
			}
		}


		return rapport;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.MESURE_TUTELLE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new TutelleAdapter();
	}

}
