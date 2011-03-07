package ch.vd.uniregctb.evenement.civil.interne.tutelle;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
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
 * Modélise un événement de mise ou levee de tutelle, curatelle ou conseil
 * légal.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class TutelleAdapter extends EvenementCivilInterneBase {

	protected static Logger LOGGER = Logger.getLogger(TutelleAdapter.class);

	/**
	 * L'Office du Tuteur General.
	 */
	private TuteurGeneral tuteurGeneral;

	/**
	 * Le tuteur de l'individu.
	 */
	private Individu tuteur = null;

	/**
	 * Le type de tutelle
	 */
	private TypeTutelle typeTutelle = null;

	/**
	 * L'autorité tutélaire
	 */
	private CollectiviteAdministrative autoriteTutelaire = null;

	protected TutelleAdapter(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);

		/*
		 * Récupération de l'année de l'événement
		 */
		final int anneeEvenement = evenement.getDateEvenement().year();

		/*
		 * Récupération de la tutelle.
		 */
		ch.vd.uniregctb.interfaces.model.Tutelle tutelle = context.getServiceCivil().getTutelle(getNoIndividu(), anneeEvenement);

		/*
		 * Initialisation du type de tutelle.
		 */
		this.typeTutelle = tutelle.getTypeTutelle();

		/*
		 * Récupération du tuteur ou/et autorité tutellaire
		 */
		this.tuteurGeneral = tutelle.getTuteurGeneral();
		this.tuteur = tutelle.getTuteur();

		if (tutelle.getNumeroCollectiviteAutoriteTutelaire() != null) {
			try {
				this.autoriteTutelaire = context.getServiceInfra().getCollectivite(tutelle.getNumeroCollectiviteAutoriteTutelaire().intValue());
			}
			catch (InfrastructureException e) {
				throw new EvenementCivilInterneException(String.format("Autorité tutélaire avec numéro %d introuvable", tutelle.getNumeroCollectiviteAutoriteTutelaire()), e);
			}
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected TutelleAdapter(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, Individu tuteur, TuteurGeneral tuteurGeneral,
	                         TypeTutelle typeTutelle, CollectiviteAdministrative autoriteTutelaire, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.MESURE_TUTELLE, date, numeroOfsCommuneAnnonce, context);
		this.tuteur = tuteur;
		this.tuteurGeneral = tuteurGeneral;
		this.typeTutelle = typeTutelle;
		this.autoriteTutelaire = autoriteTutelaire;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		/* rien à faire ici, un seul événement unitaire pour le pupille */
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		/*
		 * Un pupille ne peut être à la fois sous tutelle d'un tuteur et de l'office du tuteur général.
		 * Cas qui peut arriver suite à un bug coriace dans ILF1
		 */
		if  (getTuteur() != null && getTuteurGeneral() != null) {
			throw new EvenementCivilHandlerException("Un pupille ne peut être à la fois sous tutelle d'un tuteur et de l'office du tuteur général");
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		// Récupération de la pupille
		final PersonnePhysique pupille = getPersonnePhysiqueOrThrowException(getNoIndividu());
		Assert.notNull(pupille);

		// Récupération du tuteur. Dans certains cas, le tuteur n'est pas renseigné et c'est l'office du tuteur général qui le remplace.
		final PersonnePhysique tuteurPersonnePhysique;
		if (getTuteur() != null) {
			tuteurPersonnePhysique = getPersonnePhysiqueOrThrowException(getTuteur().getNoTechnique());
			Assert.notNull(tuteurPersonnePhysique);
		}
		else {
			tuteurPersonnePhysique = null;
		}

		// Récupération éventuelle de l'autorité tutelaire, c'est-à-dire l'office du tuteur général
		final ch.vd.uniregctb.tiers.CollectiviteAdministrative officeTuteurGeneral;
		if (getTuteurGeneral() != null ){
			officeTuteurGeneral = context.getTiersService().getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noTuteurGeneral);
			Assert.notNull(officeTuteurGeneral);
		}
		else {
			officeTuteurGeneral = null;
		}

		// La tutelle n'est rattachée ni à un tuteur personne physique, ni à l'Office du Tuteur Général ==> ERREUR
		if (tuteurPersonnePhysique == null && officeTuteurGeneral == null) {
			throw new EvenementCivilHandlerException("La tutelle n'est rattachée ni à un tuteur ordinaire, ni à l'Office du Tuteur Général");
		}

		final TypeRapportEntreTiers mesureExistante = getTutelleExistante(pupille, tuteurPersonnePhysique, officeTuteurGeneral, getDate());
		if (mesureExistante != null) {
			Audit.warn(getNumeroEvenement(), String.format("Une mesure de tutelle existe déjà (type %s). Aucune action enteprise.", mesureExistante.name()));
		}
		else {
			final Tiers tuteurEffectif;
			if (tuteurPersonnePhysique != null) {
				Audit.info(getNumeroEvenement(), "Le pupille est rattaché à un tuteur ordinaire");
				tuteurEffectif = tuteurPersonnePhysique;
			}
			else {
				Audit.info(getNumeroEvenement(), "Le pupille est rattaché à l'OTG");
				tuteurEffectif = officeTuteurGeneral;
			}
			Assert.notNull(tuteurEffectif);

			// autorité tutélaire ?
			final ch.vd.uniregctb.tiers.CollectiviteAdministrative autoriteTutelaire1;
			if (getAutoriteTutelaire() != null) {
				autoriteTutelaire1 = context.getTiersService().getOrCreateCollectiviteAdministrative(getAutoriteTutelaire().getNoColAdm());
				Assert.notNull(autoriteTutelaire1);
			}
			else {
				autoriteTutelaire1 = null;
			}

			/*
			 * Création d'un rapport entre tiers
			 */
			RapportEntreTiers rapport = creeRepresentationLegale(getDate(), pupille, tuteurEffectif, autoriteTutelaire1, getTypeTutelle());
			rapport = context.getTiersDAO().save(rapport);

			final StringBuilder b = new StringBuilder();
			b.append(String.format("Création d'un rapport de type %s entre le pupille et son tuteur physique", rapport.getType().name()));
			if (autoriteTutelaire1 != null) {
				b.append(String.format(" (autorité tutélaire : %s (%d))", getAutoriteTutelaire().getNomCourt(), autoriteTutelaire1.getNumeroCollectiviteAdministrative()));
			}
			else {
				b.append(" (autorité tutélaire inconnue)");
			}
			final String msg = b.toString();
			Audit.info(getNumeroEvenement(), msg);
		}

		return null;
	}

	/**
	 * @return un type de tutelle ({@link ch.vd.uniregctb.type.TypeRapportEntreTiers#TUTELLE TUTELLE}, {@link ch.vd.uniregctb.type.TypeRapportEntreTiers#CURATELLE CURATELLE} ou
	 *          {@link ch.vd.uniregctb.type.TypeRapportEntreTiers#CONSEIL_LEGAL CONSEIL_LEGAL}) correspondant à la mesure de tutelle déjà connue entre le pupille et le
	 *          tuteur (ou office du tuteur général) à la date donnée, ou <code>null</code> s'il n'existe aucune mesure de tutelle sur ce pupille à cette date
	 * @see ch.vd.uniregctb.type.TypeRapportEntreTiers
	 */
	private static TypeRapportEntreTiers getTutelleExistante(PersonnePhysique pupille, PersonnePhysique tuteur, ch.vd.uniregctb.tiers.CollectiviteAdministrative officeTuteurGeneral, RegDate date) {
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
	private RepresentationLegale creeRepresentationLegale(RegDate dateEvenement, PersonnePhysique pupille, Tiers representant, ch.vd.uniregctb.tiers.CollectiviteAdministrative autoriteTutelaire, TypeTutelle typeTutelle)  throws EvenementCivilHandlerException {

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

	public static RapportEntreTiers getRapportTutelleOuvert(PersonnePhysique pupille, RegDate date) throws EvenementCivilHandlerException {

		RapportEntreTiers tutelle = null;
		int nombreRapportTutelleOuverts = 0;
		for (RapportEntreTiers rapportEntreTiers : pupille.getRapportsSujet()) {
			if ((TypeRapportEntreTiers.TUTELLE == rapportEntreTiers.getType() ||
					TypeRapportEntreTiers.CURATELLE == rapportEntreTiers.getType() ||
					TypeRapportEntreTiers.CONSEIL_LEGAL == rapportEntreTiers.getType()) &&
					RegDateHelper.isBetween(date, rapportEntreTiers.getDateDebut(), rapportEntreTiers.getDateFin(), null)) {
				tutelle = rapportEntreTiers;
				nombreRapportTutelleOuverts++;
			}
		}
		if (nombreRapportTutelleOuverts > 1)
			throw new EvenementCivilHandlerException("Plus d'un rapport tutelle, curatelle ou conseil légal actif a été trouvé");
		return tutelle;
	}

	/**
	 * @return Returns the tuteurGeneral.
	 */
	public final TuteurGeneral getTuteurGeneral() {
		return tuteurGeneral;
	}

	/**
	 * @return Returns the tuteur.
	 */
	public final Individu getTuteur() {
		return tuteur;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.evenement.tutelle.Tutelle#getTypeTutelle()
	 */
	public TypeTutelle getTypeTutelle() {
		return typeTutelle;
	}

	/**
	 * @return l'autorité tutélaire qui a ordonné la tutelle
	 */
	public CollectiviteAdministrative getAutoriteTutelaire() {
		return autoriteTutelaire;
	}
}
