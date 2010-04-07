package ch.vd.uniregctb.evenement.mariage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Règles métiers permettant de traiter les événements mariage ou de partenariat enregistré.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class MariageHandler extends EvenementCivilHandlerBase {

	/** Un logger. */
	private static final Logger LOGGER = Logger.getLogger(MariageHandler.class);

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Rien a vérifier, un seul événement est envoyé pour l'un des 2
		// individus : donc pas de regroupement
	}

	/**
	 * @throws EvenementCivilHandlerException
	 * @see ch.vd.uniregctb.evenement.common.EvenementCivilHandler#validate(java.lang.Object, java.util.List)
	 */
	@Override
	public void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> errors, List<EvenementCivilErreur> warnings) {
		/* L’événement est mis en erreur dans les cas suivants */
		final Mariage mariage = (Mariage) target;
		final Individu individu = mariage.getIndividu();

		/*
		 * Le tiers correspondant doit exister
		 */
		PersonnePhysique habitant = getHabitantOrFillErrors(individu.getNoTechnique(), errors);
		if (habitant == null) {
			return;
		}

		final ServiceCivilService serviceCivil = getService().getServiceCivilService();
		final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(individu.getNoTechnique(), mariage.getDate());
		if (etatCivil == null) {
			errors.add(new EvenementCivilErreur("L'individu principal ne possède pas d'état civil à la date de l'événement"));
		}

		if (!EtatCivilHelper.estMarieOuPacse(etatCivil)) {
			errors.add(new EvenementCivilErreur("L'individu principal n'est ni marié ni pacsé dans le civil"));
		}

		/*
		 * Dans le cas où le conjoint réside dans le canton, il faut que le tiers contribuable existe.
		 */
		PersonnePhysique habitantConjoint = null;
		Individu conjoint = mariage.getNouveauConjoint();
		if (conjoint != null) {

			/*
			 * Le tiers correspondant doit exister
			 */
			habitantConjoint = getHabitantOrFillErrors(conjoint.getNoTechnique(), errors);
			if (habitantConjoint == null) {
				return;
			}

			final EtatCivil etatCivilConjoint = serviceCivil.getEtatCivilActif(conjoint.getNoTechnique(), mariage.getDate());
			if (etatCivilConjoint == null) {
				errors.add(new EvenementCivilErreur("Le conjoint ne possède pas d'état civil à la date de l'événement"));
			}

			if (!EtatCivilHelper.estMarieOuPacse(etatCivilConjoint)) {
				errors.add(new EvenementCivilErreur("Le conjoint n'est ni marié ni pacsé dans le civil"));
			}
		}

		final ValidationResults resultat = getMetier().validateMariage(mariage.getDate(), habitant, habitantConjoint);
		addValidationResults(errors, warnings, resultat);
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		Mariage mariage = (Mariage) evenement;

		try {
			final PersonnePhysique contribuable = getHabitantOrThrowException(mariage.getIndividu().getNoTechnique());
			final PersonnePhysique conjointContribuable = (mariage.getNouveauConjoint() == null) ? null : getHabitantOrThrowException(mariage.getNouveauConjoint().getNoTechnique());

			// état civil pour traitement
			final EtatCivil etatCivil = getService().getServiceCivilService().getEtatCivilActif(contribuable.getNumeroIndividu(), mariage.getDate());
			final ch.vd.uniregctb.type.EtatCivil etatCivilUnireg = ch.vd.uniregctb.type.EtatCivil.from(etatCivil.getTypeEtatCivil());
			
			// [UNIREG-780] : détection et reprise d'un ancien ménage auquel ont appartenu les deux contribuables
			boolean remariage = false;
			MenageCommun ancienMenage = null;
			for (RapportEntreTiers rapport : contribuable.getRapportsSujet()) {
				if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapport.getType())) {
					final MenageCommun menage = (MenageCommun) getTiersDAO().get(rapport.getObjetId());
					final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(menage, rapport.getDateDebut());
					if (couple != null && couple.estComposeDe(contribuable, conjointContribuable)) {
						// les contribuables se sont remariés
						remariage = true;
						ancienMenage = menage;
					}
				}
			}
			
			if (remariage) {
				// [UNIREG-780]
				getMetier().rattachToMenage(ancienMenage, contribuable, conjointContribuable, mariage.getDate(), null, etatCivilUnireg, false, mariage.getNumeroEvenement());
			}
			else {
				getMetier().marie(mariage.getDate(), contribuable, conjointContribuable, null, etatCivilUnireg, false, mariage.getNumeroEvenement());
			}

			return null;
		}
		catch (Exception e) {
			LOGGER.error("Erreur lors du traitement de mariage", e);
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.MARIAGE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new MariageAdapter();
	}

}
