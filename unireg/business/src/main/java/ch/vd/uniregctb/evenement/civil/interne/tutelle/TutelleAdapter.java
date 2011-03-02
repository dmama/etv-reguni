package ch.vd.uniregctb.evenement.civil.interne.tutelle;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.type.TypeTutelle;

/**
 * Modélise un événement de mise ou levee de tutelle, curatelle ou conseil
 * légal.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class TutelleAdapter extends EvenementCivilInterneBase implements Tutelle {

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

	private TutelleHandler handler;

	protected TutelleAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, TutelleHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
		this.handler = handler;

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

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.validateSpecific(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
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
}
