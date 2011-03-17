package ch.vd.uniregctb.evenement.tutelle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
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
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.type.TypeTutelle;

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
		final Tutelle tutelle = (Tutelle) evenement;

		/*
		 * Un pupille ne peut être à la fois sous tutelle d'un tuteur et de l'office du tuteur général.
		 * Cas qui peut arriver suite à un bug coriace dans ILF1
		 */
		if  (tutelle.getTuteur() != null && tutelle.getTuteurGeneral() != null) {
			throw new EvenementCivilHandlerException("Un pupille ne peut être à la fois sous tutelle d'un tuteur et de l'office du tuteur général");
		}
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		final Tutelle tutelle = (Tutelle) evenement;

		// Récupération de la pupille
		final PersonnePhysique pupille = getPersonnePhysiqueOrThrowException(tutelle.getNoIndividu());
		Assert.notNull(pupille);

		// Récupération du tuteur. Dans certains cas, le tuteur n'est pas renseigné et c'est l'office du tuteur général qui le remplace.
		final PersonnePhysique tuteurPersonnePhysique;
		if (tutelle.getTuteur() != null) {
			tuteurPersonnePhysique = getPersonnePhysiqueOrThrowException(tutelle.getTuteur().getNoTechnique());
			Assert.notNull(tuteurPersonnePhysique);
		}
		else {
			tuteurPersonnePhysique = null;
		}

		// Récupération éventuelle de l'autorité tutelaire, c'est-à-dire l'office du tuteur général
		final CollectiviteAdministrative officeTuteurGeneral;
		if (tutelle.getTuteurGeneral() != null ){
			officeTuteurGeneral = getService().getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noTuteurGeneral);
			Assert.notNull(officeTuteurGeneral);
		}
		else {
			officeTuteurGeneral = null;
		}

		// La tutelle n'est rattachée ni à un tuteur personne physique, ni à l'Office du Tuteur Général ==> ERREUR
		if (tuteurPersonnePhysique == null && officeTuteurGeneral == null) {
			throw new EvenementCivilHandlerException("La tutelle n'est rattachée ni à un tuteur ordinaire, ni à l'Office du Tuteur Général");
		}

		final TypeRapportEntreTiers mesureExistante = getTutelleExistante(pupille, tuteurPersonnePhysique, officeTuteurGeneral, tutelle.getDate());
		if (mesureExistante != null) {
			Audit.warn(evenement.getNumeroEvenement(), String.format("Une mesure de tutelle existe déjà (type %s). Aucune action enteprise.", mesureExistante.name()));
		}
		else {
			final Tiers tuteurEffectif;
			if (tuteurPersonnePhysique != null) {
				Audit.info(evenement.getNumeroEvenement(), "Le pupille est rattaché à un tuteur ordinaire");
				tuteurEffectif = tuteurPersonnePhysique;
			}
			else {
				Audit.info(evenement.getNumeroEvenement(), "Le pupille est rattaché à l'OTG");
				tuteurEffectif = officeTuteurGeneral;
			}
			Assert.notNull(tuteurEffectif);

			// autorité tutélaire ?
			final CollectiviteAdministrative autoriteTutelaire;
			if (tutelle.getAutoriteTutelaire() != null) {
				autoriteTutelaire = getService().getOrCreateCollectiviteAdministrative(tutelle.getAutoriteTutelaire().getNoColAdm());
				Assert.notNull(autoriteTutelaire);
			}
			else {
				autoriteTutelaire = null;
			}

			/*
			 * Création d'un rapport entre tiers
			 */
			RapportEntreTiers rapport = creeRepresentationLegale(tutelle.getDate(), pupille, tuteurEffectif, autoriteTutelaire, tutelle.getTypeTutelle());
			rapport = getTiersDAO().save(rapport);

			final StringBuilder b = new StringBuilder();
			b.append(String.format("Création d'un rapport de type %s entre le pupille et son tuteur physique", rapport.getType().name()));
			if (autoriteTutelaire != null) {
				b.append(String.format(" (autorité tutélaire : %s (%d))", tutelle.getAutoriteTutelaire().getNomCourt(), autoriteTutelaire.getNumeroCollectiviteAdministrative()));
			}
			else {
				b.append(" (autorité tutélaire inconnue)");
			}
			final String msg = b.toString();
			Audit.info(evenement.getNumeroEvenement(), msg);
		}

		return null;
	}

	/**
	 * @return un type de tutelle ({@link ch.vd.uniregctb.type.TypeRapportEntreTiers#TUTELLE TUTELLE}, {@link ch.vd.uniregctb.type.TypeRapportEntreTiers#CURATELLE CURATELLE} ou
	 *          {@link ch.vd.uniregctb.type.TypeRapportEntreTiers#CONSEIL_LEGAL CONSEIL_LEGAL}) correspondant à la mesure de tutelle déjà connue entre le pupille et le
	 *          tuteur (ou office du tuteur général) à la date donnée, ou <code>null</code> s'il n'existe aucune mesure de tutelle sur ce pupille à cette date
	 * @see ch.vd.uniregctb.type.TypeRapportEntreTiers
	 */
	private static TypeRapportEntreTiers getTutelleExistante(PersonnePhysique pupille, PersonnePhysique tuteur, CollectiviteAdministrative officeTuteurGeneral, RegDate date) {
		final Set<RapportEntreTiers> rapports = pupille.getRapportsSujet();
		final Long numeroTuteur = (tuteur == null ? null : tuteur.getNumero());
		final Long numeroOfficeTG = (officeTuteurGeneral == null ? null : officeTuteurGeneral.getNumero());

		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (!rapport.isValidAt(date)) {
					continue;
				}
				if (TypeRapportEntreTiers.TUTELLE == rapport.getType() || TypeRapportEntreTiers.CURATELLE == rapport.getType() || TypeRapportEntreTiers.CONSEIL_LEGAL == rapport.getType()) {
					if (numeroTuteur != null && rapport.getObjetId().equals(numeroTuteur)) {
						return rapport.getType();
					}
					if (numeroOfficeTG != null && rapport.getObjetId().equals(numeroOfficeTG)) {
						return rapport.getType();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Relie le tiers et le pupille avec un objet RapportEntreTiers de type tutelle, curatelle ou conseil légal.
	 *
	 * @param dateEvenement la date de début validité de la représentation légale
	 * @param pupille le pupille
	 * @param representant le tuteur/curateur/conseiller (peut être une personne physique ou l'office du tuteur général)
	 * @param autoriteTutelaire justice de paix qui a ordonné la représentation légale (optionelle)
	 * @param typeTutelle le type de représentation légale
	 */
	private RepresentationLegale creeRepresentationLegale(RegDate dateEvenement, PersonnePhysique pupille, Tiers representant, CollectiviteAdministrative autoriteTutelaire, TypeTutelle typeTutelle)  throws EvenementCivilHandlerException {

		Assert.notNull(pupille);
		Assert.notNull(representant);

		final RepresentationLegale rapport;

		switch (typeTutelle) {
		case TUTELLE :
			rapport = new ch.vd.uniregctb.tiers.Tutelle(dateEvenement, null, pupille, representant, autoriteTutelaire);
			break;

		case CURATELLE :
			rapport = new Curatelle(dateEvenement, null, pupille, representant, autoriteTutelaire);
			break;

		case CONSEIL_LEGAL:
			rapport = new ConseilLegal(dateEvenement, null, pupille, representant, autoriteTutelaire);
			break;

		default :
			throw new EvenementCivilHandlerException("Ce type de tutelle n'est pas pris en charge : " + typeTutelle);
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
