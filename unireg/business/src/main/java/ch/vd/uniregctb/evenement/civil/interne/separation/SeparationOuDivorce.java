package ch.vd.uniregctb.evenement.civil.interne.separation;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public abstract class SeparationOuDivorce extends EvenementCivilInterne {

	/**
	 * L'ancien conjoint de l'individu concerné par la séparation.
	 */
	private Individu ancienConjoint;

	protected SeparationOuDivorce(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		/*
		 * Récupération des informations sur le conjoint de l'individu depuis le host.
		 */
		final long noIndividu = getNoIndividu();
		Individu individuPrincipal = context.getServiceCivil().getIndividu(noIndividu, getDate());
		this.ancienConjoint = context.getServiceCivil().getConjoint(individuPrincipal.getNoTechnique(), getDate().getOneDayBefore());
		
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	public SeparationOuDivorce(Individu individu, Individu conjoint, TypeEvenementCivil typeEvenementCivil, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce,
	                           Individu ancienConjoint, EvenementCivilContext context) {
		super(individu, conjoint, typeEvenementCivil, dateEvenement, numeroOfsCommuneAnnonce, context);
		this.ancienConjoint = ancienConjoint;
	}

	public Individu getAncienConjoint() {
		return ancienConjoint;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		Individu individu = getIndividu();

		RegDate date = getDate();
		// obtention du tiers correspondant a l'individu.
		PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), erreurs);
		if (habitant == null) {
			return;
		}

		/*
		 * Le conjoint de l'individu est inconnu a ce moment, car un seul
		 * événement de séparation/divorce est envoyé.
		 * Pour récuperer l'ex conjoint la méthode getAncienConjoint a été créé.
		 */

		// récupération du conjoint
		PersonnePhysique conjoint = null;
		if (getAncienConjoint() != null) {
			// obtention du tiers correspondant au conjoint.
			conjoint = getPersonnePhysiqueOrFillErrors(getAncienConjoint().getNoTechnique(), erreurs);
			if (conjoint == null) {
				return;
			}
		}

		/*
		 * Vérifie que les tiers sont séparés ou divorcés dans le civil.
		 */
		long noIndividuPrincipal = individu.getNoTechnique();

		final ServiceCivilService serviceCivil = context.getTiersService().getServiceCivilService();
		EtatCivil etatCivilTiersPrincipal =  serviceCivil.getEtatCivilActif(noIndividuPrincipal, date);
		if (etatCivilTiersPrincipal == null) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu " + noIndividuPrincipal + " ne possède pas d'état civil à la date de l'événement"));
		}
		else if (!(EtatCivilHelper.estSepare(etatCivilTiersPrincipal) || EtatCivilHelper.estDivorce(etatCivilTiersPrincipal))) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu " + noIndividuPrincipal + " n'est ni séparé ni divorcé dans le civil"));
		}

		if (conjoint != null) {
			long noIndividuConjoint = (conjoint).getNumeroIndividu();
			EtatCivil etatCivilTiersConjoint = serviceCivil.getEtatCivilActif(noIndividuConjoint, date);
			if (etatCivilTiersConjoint == null) {
				erreurs.add(new EvenementCivilExterneErreur("L'individu " + noIndividuConjoint + " ne possède pas d'état civil à la date de l'événement"));
			}
			else if (!(EtatCivilHelper.estSepare(etatCivilTiersConjoint) || EtatCivilHelper.estDivorce(etatCivilTiersConjoint))) {
				erreurs.add(new EvenementCivilExterneErreur("L'individu " + noIndividuConjoint + " n'est ni séparé ni divorcé dans le civil"));
			}
		}

		/*
		 * Vérifie que l'individu ne soit pas déjà séparé
		 */
		if (isSeparesFiscalement(date, habitant, conjoint)) {
			// si les tiers sont séparés ne pas continuer les vérifications
			return;
		}

		EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(habitant, date);
		if (menageComplet == null) {
			erreurs.add(new EvenementCivilExterneErreur("Aucun ensemble tiers-couple a été trouvé pour l'habitant n°" + habitant.getNumero()));
			return;
		}
		if (!menageComplet.estComposeDe(habitant, conjoint)) {
			/*
			 * Vérifie que les deux habitants appartiennent au même ménage
			 */
			if (conjoint != null) {
				erreurs.add(new EvenementCivilExterneErreur("Les deux habitant (" + habitant.getNumero() + " et " +
						conjoint.getNumero() + ") ne font pas partie du même ménage."));
			}
			else {
				erreurs.add(new EvenementCivilExterneErreur("L'habitant (" + habitant.getNumero() + ") ne fait pas partie du ménage."));
			}
		}
		else {
			try {
				/*
				 * validation d'après le MetierService
				 */
				ValidationResults validationResults = context.getMetierService().validateSeparation(menageComplet.getMenage(), date);
				addValidationResults(erreurs, warnings, validationResults);
			}
			catch (NullPointerException npe) {
				erreurs.add(new EvenementCivilExterneErreur("Le ménage commun de l'habitant n°" + habitant.getNumero() + " n'existe pas"));
			}
		}
	}

	/**
	 * Retourne true si les tiers appartenant au ménage sont séparés fiscalement.
	 * @param date date pour laquelle la vérification s'effectue
	 * @param habitant l'habitant
	 * @param conjoint son conjoint
	 * @return
	 */
	protected boolean isSeparesFiscalement(RegDate date, PersonnePhysique habitant, PersonnePhysique conjoint) throws EvenementCivilException {

		MenageCommun menage = getService().findMenageCommun(habitant, date);
		if (menage != null) {
			// check validité du for principal du ménage
			ForFiscalPrincipal forFiscalPremier = habitant.getForFiscalPrincipalAt(date);

			ForFiscalPrincipal forFiscalDeuxieme = null;
			if (conjoint != null) {
				forFiscalDeuxieme = conjoint.getForFiscalPrincipalAt(date);
			}

			ForFiscalPrincipal forFiscalMenage = menage.getForFiscalPrincipalAt(date);
			if (forFiscalMenage != null && forFiscalMenage.isValidAt(date)) {
				// le for du ménage est ouvert, vérification que ceux des tiers sont fermés
				if (forFiscalPremier != null && forFiscalPremier.getDateFin() == null) {
					throw new EvenementCivilException("Le for du ménage [" + menage + "] est ouvert, celui de l'habitant [" +
							habitant.getNumero() + "] est aussi ouvert");
				}
				if (forFiscalDeuxieme != null && forFiscalDeuxieme.getDateFin() == null) {
					throw new EvenementCivilException("Le for du ménage [" + menage + "] est ouvert, celui de l'habitant [" +
							conjoint.getNumero() + "] est aussi ouvert");
				}
			}
			else {
				throw new EvenementCivilException(String.format("Le ménage commun [%s] ne possède pas de for principal le %s", menage, RegDateHelper.dateToDisplayString(date)));
			}

			return false;
		}

		return true;
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		long numeroIndividu = getNoIndividu();
		RegDate dateEvenement = getDate();

		// Récupération de l'état civil de l'individu
		final ServiceCivilService serviceCivil = context.getTiersService().getServiceCivilService();

		// état civil au moment de l'événement
		final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(numeroIndividu, dateEvenement);
		Assert.notNull(etatCivil);

		if (EtatCivilHelper.estSepare(etatCivil) || EtatCivilHelper.estDivorce(etatCivil)) { // si l'individu est séparé ou divorcé
			handleSeparation(this, warnings);
		}
		return null;
	}

	private void handleSeparation(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		// Obtention du premier tiers.
		final PersonnePhysique principal = getService().getPersonnePhysiqueByNumeroIndividu(evenement.getNoIndividu());

		final RegDate dateEvt = evenement.getDate();

		if (!isSeparesFiscalement(dateEvt, principal, null)) {
			// récupération de l'ensemble tiers-couple
			final EnsembleTiersCouple menageComplet = getService().getEnsembleTiersCouple(principal, dateEvt);
			// Récupération du ménage du tiers
			final MenageCommun menageCommun = menageComplet.getMenage();
			// état civil pour traitement
			final EtatCivil etatCivil = getService().getServiceCivilService().getEtatCivilActif(menageComplet.getPrincipal().getNumeroIndividu(), dateEvt);
			final ch.vd.uniregctb.type.EtatCivil etatCivilUnireg = etatCivil.getTypeEtatCivil().asCore();
			// traitement de la séparation
			try {
				context.getMetierService().separe(menageCommun, dateEvt, null, etatCivilUnireg, false, evenement.getNumeroEvenement());
			}
			catch (MetierServiceException e) {
				throw new EvenementCivilException(e.getMessage(), e);
			}
		}
	}
}
