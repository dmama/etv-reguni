package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public abstract class AnnulationPermisCOuNationaliteSuisse extends EvenementCivilInterne {

	protected AnnulationPermisCOuNationaliteSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected AnnulationPermisCOuNationaliteSuisse(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationPermisCOuNationaliteSuisse(Individu individu, Individu conjoint, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		PersonnePhysique habitant = getPrincipalPP();
		final RegDate dateEvenement = getDate();
		verifierPresenceDecisionEnCours(habitant, dateEvenement);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// si l'habitant a un permis C (événement annulation permis C)
		// ou a la nationalité suisse (événement annulation de l'obtention de la nationalité suisse)
		RegDate dateObtention = getDate();
		PersonnePhysique habitant = getPrincipalPP();
		// récupération du For fiscal principal actuel
		ForFiscalPrincipal ffp = getForPrincipalActif(habitant, null);
		if (ffp == null) { // passer en erreur
			throw new EvenementCivilException("L'habitant " + habitant.getNumero() + " n'a pas de For principal actif");
		}
		if (ffp.getDateDebut().equals(dateObtention)) {
			// cherche le For precedent
			ForFiscalPrincipal ffpPrecedent = getForPrincipalActif(habitant, dateObtention.getOneDayBefore());
			if (ffpPrecedent == null) { // passer en erreur si le For précédent n'existe pas
				throw new EvenementCivilException("L'habitant " + habitant.getNumero() + " n'a pas de For principal précédant le For courrant");
			}
			//annule le for, réouvre le rpécédent et envoi evt fiscal
			getService().annuleForFiscal(ffp);
		}
		else if (ffp.getDateDebut().compareTo(dateObtention) > 0) {
			// il y a eu d'autres opérations aprés l'obtention, passer en erreur
			throw new EvenementCivilException("Il y a eu d'autres opérations après l'obtention du permis C/nationalité");
		}
		return HandleStatus.TRAITE;
	}

	/**
	 * Cherche, pour une date donnée, s'il existe un menage contenant
	 * l'habitant et retourne son for, s'il est actif; sinon, le for
	 * actif de l'habitant est retourné si trouvé.
	 *
	 * @param habitant l'habitant pour qui on cherche le for actif.
	 * @param date date pour laquelle on cherche le for; peut être null si on cherche le dernier.
	 * @return un ForFiscalPrincipal si trouvé dans le menage puis l'habitant; null, sinon.
	 */
	private ForFiscalPrincipal getForPrincipalActif(PersonnePhysique habitant, RegDate date) throws EvenementCivilException {
		Individu individu = getService().getIndividu(habitant);
		ForFiscalPrincipal forPrincipalHabitant = habitant.getForFiscalPrincipalAt(date);
		EnsembleTiersCouple ensembleTiersCouple = getService().getEnsembleTiersCouple(habitant, date);
		MenageCommun menage = null;
		if (ensembleTiersCouple != null ){
			menage = ensembleTiersCouple.getMenage();
		}

		ForFiscalPrincipal result = null;
		if (forPrincipalHabitant != null) { //individu seul assujetti
			EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
			if (etatCivilIndividu == null) {
				throw new EvenementCivilException("Impossible de récupérer l'état civil courant de l'individu");
			}
			if(EtatCivilHelper.estMarieOuPacse(etatCivilIndividu)) {
				// le for devrait être sur le menage commun
				throw new EvenementCivilException("Un individu avec conjoint non séparé possède un for principal individuel actif");
			}

			result = forPrincipalHabitant;
		}
		else if(menage != null && menage.getForFiscalPrincipalAt(date) != null) { //couple assujetti
			result = menage.getForFiscalPrincipalAt(date);
		}
		return result;
	}
}
