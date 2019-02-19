package ch.vd.unireg.evenement.civil.interne.veuvage;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;

/**
 * Adapter de l'événement de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public class Veuvage extends EvenementCivilInterne {

	protected Veuvage(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Veuvage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	protected Veuvage(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	public RegDate getDateVeuvage() {
		return getDate();
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final Individu individu = getIndividu();

		// [UNIREG-2241] au traitement d'un événement civil de veuvage, on doit contrôler l'état civil de l'individu
		final EtatCivil etatCivil = individu.getEtatCivil(getDate());
		if (etatCivil == null || etatCivil.getTypeEtatCivil() != TypeEtatCivil.VEUF) {
			erreurs.addErreur(String.format("L'individu %d n'est pas veuf dans le civil au %s", individu.getNoTechnique(), RegDateHelper.dateToDisplayString(getDate())));
		}
		else {
			final PersonnePhysique veuf = getPrincipalPP();

			/*
			 * Validations métier
			 */
			final ValidationResults validationResults = context.getMetierService().validateVeuvage(veuf, getDate());
			addValidationResults(erreurs, warnings, validationResults);
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/*
		 * Obtention du tiers correspondant au veuf.
		 */
		PersonnePhysique veuf = getPrincipalPP();
		RegDate dateEvenement = getDateVeuvage();
		if (isVeuvageRedondant(veuf, dateEvenement)) {
			return HandleStatus.REDONDANT;
		}

		/*
		 * Traitement de l'événement
		 */
		try {
			context.getMetierService().veuvage(veuf, getDateVeuvage(), null, getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}

	private boolean isVeuvageRedondant(PersonnePhysique veuf,RegDate dateEvenement) {
		final EnsembleTiersCouple ensembleTiersCouple = context.getTiersService().getEnsembleTiersCouple(veuf, dateEvenement);
		if (ensembleTiersCouple != null) {
			final MenageCommun menage = ensembleTiersCouple.getMenage();
			final ForFiscalPrincipal forMenage = menage.getForFiscalPrincipalAt(null);
			if (forMenage == null) {
				final ForFiscalPrincipal dernierForMenage = menage.getDernierForFiscalPrincipal();
				final ForFiscalPrincipal forCourantVeuf = veuf.getForFiscalPrincipalAt(dateEvenement.getOneDayAfter());

				if (dernierForMenage != null && dateEvenement.equals(dernierForMenage.getDateFin()) && MotifFor.VEUVAGE_DECES == dernierForMenage.getMotifFermeture()
						&& forCourantVeuf !=null && dateEvenement.getOneDayAfter().equals(forCourantVeuf.getDateDebut()) &&
						MotifFor.VEUVAGE_DECES == forCourantVeuf.getMotifOuverture()) {
					return true;
				}
			}
		}
		return false;
	}
}
