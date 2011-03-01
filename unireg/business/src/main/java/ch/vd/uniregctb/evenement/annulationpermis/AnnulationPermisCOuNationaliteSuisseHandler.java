package ch.vd.uniregctb.evenement.annulationpermis;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Règles métiers permettant de traiter les événements suivants :
 * - annulation de permis C
 * - annulation de la nationalité suisse
 *
 * @author Pavel BLANCO
 *
 */
public abstract class AnnulationPermisCOuNationaliteSuisseHandler extends EvenementCivilHandlerBase {

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase#handle(ch.vd.uniregctb.evenement.EvenementCivil)
	 */
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		Individu individu = evenement.getIndividu();

		// si l'habitant a un permis C (événement annulation permis C)
		// ou a la nationalité suisse (événement annulation de l'obtention de la nationalité suisse)
		RegDate dateObtention = evenement.getDate();
		PersonnePhysique habitant = getService().getPersonnePhysiqueByNumeroIndividu(individu.getNoTechnique());
		// récupération du For fiscal principal actuel
		ForFiscalPrincipal ffp = getForPrincipalActif(habitant, null);
		if (ffp == null) { // passer en erreur
			throw new EvenementCivilHandlerException("L'habitant " + habitant.getNumero() + " n'a pas de For principal actif");
		}
		if (ffp.getDateDebut().equals(dateObtention)) {
			// cherche le For precedent
			ForFiscalPrincipal ffpPrecedent = getForPrincipalActif(habitant, dateObtention.getOneDayBefore());
			if (ffpPrecedent == null) { // passer en erreur si le For précédent n'existe pas
				throw new EvenementCivilHandlerException("L'habitant " + habitant.getNumero() + " n'a pas de For principal précédant le For courrant");
			}
			//annule le for, réouvre le rpécédent et envoi evt fiscal
			getService().annuleForFiscal(ffp, true);
		}
		else if (ffp.getDateDebut().compareTo(dateObtention) > 0) {
			// il y a eu d'autres opérations aprés l'obtention, passer en erreur
			throw new EvenementCivilHandlerException("Il y a eu d'autres opérations après l'obtention du permis C/nationalité");
		}
		return null;
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
	private ForFiscalPrincipal getForPrincipalActif(PersonnePhysique habitant, RegDate date) {
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
				throw new EvenementCivilHandlerException("Impossible de récupérer l'état civil courant de l'individu");
			}
			if(EtatCivilHelper.estMarieOuPacse(etatCivilIndividu)) {
				// le for devrait être sur le menage commun
				throw new EvenementCivilHandlerException("Un individu avec conjoint non séparé possède un for principal individuel actif");
			}

			result = forPrincipalHabitant;
		}
		else if(menage != null && menage.getForFiscalPrincipalAt(date) != null) { //couple assujetti
			result = menage.getForFiscalPrincipalAt(date);
		}
		return result;
	}

}
