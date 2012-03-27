package ch.vd.uniregctb.evenement.civil.interne.separation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public abstract class SeparationOuDivorce extends EvenementCivilInterne {

	/**
	 * L'ancien conjoint de l'individu concerné par la séparation.
	 */
	private PersonnePhysique ancienConjoint;

	protected SeparationOuDivorce(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final PersonnePhysique ppPrincipale = getPrincipalPP();
		EnsembleTiersCouple etc = context.getTiersService().getEnsembleTiersCouple(ppPrincipale, getDate().getOneDayBefore());
		if (etc != null) {
			ancienConjoint = etc.getConjoint(ppPrincipale);
		}

	}

	protected SeparationOuDivorce(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);

		final PersonnePhysique ppPrincipale = getPrincipalPP();
		EnsembleTiersCouple etc = context.getTiersService().getEnsembleTiersCouple(ppPrincipale, getDate().getOneDayBefore());
		if (etc != null) {
			ancienConjoint = etc.getConjoint(ppPrincipale);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	public SeparationOuDivorce(Individu individu, Individu conjoint, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce,
	                           Individu ancienConjoint, EvenementCivilContext context) {
		super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, context);
		if (ancienConjoint != null) {
			this.ancienConjoint = context.getTiersDAO().getPPByNumeroIndividu(ancienConjoint.getNoTechnique());
		}
	}

	public PersonnePhysique getAncienConjoint() {
		return ancienConjoint;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		Individu individu = getIndividu();

		RegDate date = getDate();
		// obtention du tiers correspondant a l'individu.
		PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), erreurs);
		if (habitant == null) {
			return;
		}

		/*
		 * Vérifie que les tiers sont séparés ou divorcés dans le civil.
		 */
		long noIndividuPrincipal = individu.getNoTechnique();

		final ServiceCivilService serviceCivil = context.getTiersService().getServiceCivilService();
		EtatCivil etatCivilTiersPrincipal =  serviceCivil.getEtatCivilActif(noIndividuPrincipal, date);
		if (etatCivilTiersPrincipal == null) {
			erreurs.addErreur("L'individu " + noIndividuPrincipal + " ne possède pas d'état civil à la date de l'événement");
		}
		else if (!(EtatCivilHelper.estSepare(etatCivilTiersPrincipal) || EtatCivilHelper.estDivorce(etatCivilTiersPrincipal))) {
			erreurs.addErreur("L'individu " + noIndividuPrincipal + " n'est ni séparé ni divorcé dans le civil");
		}

		if (ancienConjoint != null) {
			long noIndividuConjoint = ancienConjoint.getNumeroIndividu();
			EtatCivil etatCivilTiersConjoint = serviceCivil.getEtatCivilActif(noIndividuConjoint, date);
			if (etatCivilTiersConjoint == null) {
				erreurs.addErreur("L'individu " + noIndividuConjoint + " ne possède pas d'état civil à la date de l'événement");
			}
			else if (!(EtatCivilHelper.estSepare(etatCivilTiersConjoint) || EtatCivilHelper.estDivorce(etatCivilTiersConjoint))) {
				erreurs.addErreur("L'individu " + noIndividuConjoint + " n'est ni séparé ni divorcé dans le civil");
			}
		}

		/*
		 * Vérifie que l'individu ne soit pas déjà séparé
		 */
		if (isSeparesFiscalement(date, habitant, ancienConjoint)) {
			// si les tiers sont séparés ne pas continuer les vérifications
			return;
		}

		EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(habitant, date);
		if (menageComplet == null) {
			erreurs.addErreur("Aucun ensemble tiers-couple a été trouvé pour l'habitant n°" + habitant.getNumero());
			return;
		}
		if (!menageComplet.estComposeDe(habitant, ancienConjoint)) {
			/*
			 * Vérifie que les deux habitants appartiennent au même ménage
			 */
			if (ancienConjoint != null) {
				erreurs.addErreur(String.format("Les deux habitant (%d et %d) ne font pas partie du même ménage.", habitant.getNumero(), ancienConjoint.getNumero()));
			}
			else {
				erreurs.addErreur(String.format("L'habitant (%d) ne fait pas partie du ménage.", habitant.getNumero()));
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
				erreurs.addErreur(String.format("Le ménage commun de l'habitant n°%d n'existe pas", habitant.getNumero()));
			}
		}
	}

	/**
	 * Retourne true si les tiers appartenant au ménage sont séparés fiscalement.
	 *
	 * @param date     date pour laquelle la vérification s'effectue
	 * @param habitant l'habitant
	 * @param conjoint son conjoint
	 * @return <b>true</b> si les tiers appartenant au ménage sont séparés fiscalement; <b>false</b> autrement.
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException
	 *          en cas de données incohérentes
	 */
	protected boolean isSeparesFiscalement(RegDate date, PersonnePhysique habitant, @Nullable PersonnePhysique conjoint) throws EvenementCivilException {

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

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		long numeroIndividu = getNoIndividu();
		RegDate dateEvenement = getDate();

		// Récupération de l'état civil de l'individu
		final ServiceCivilService serviceCivil = context.getTiersService().getServiceCivilService();

		// état civil au moment de l'événement
		final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(numeroIndividu, dateEvenement);
		Assert.notNull(etatCivil);

		if (!EtatCivilHelper.estSepare(etatCivil) && !EtatCivilHelper.estDivorce(etatCivil)) {
			throw new EvenementCivilException("L'individu " + numeroIndividu + " n'est ni séparé ni divorcé dans le civil");
		}

		return handleSeparation();
	}

	private HandleStatus handleSeparation() throws EvenementCivilException {

		// Obtention du premier tiers.
		final PersonnePhysique principal = getPrincipalPP();

		final RegDate dateEvt = getDate();

		if (isSeparesFiscalement(dateEvt, principal, null)) {
			return HandleStatus.REDONDANT;
		}

		// récupération de l'ensemble tiers-couple
		final EnsembleTiersCouple menageComplet = getService().getEnsembleTiersCouple(principal, dateEvt);
		// Récupération du ménage du tiers
		final MenageCommun menageCommun = menageComplet.getMenage();
		// état civil pour traitement
		final EtatCivil etatCivil = getService().getServiceCivilService().getEtatCivilActif(menageComplet.getPrincipal().getNumeroIndividu(), dateEvt);
		final ch.vd.uniregctb.type.EtatCivil etatCivilUnireg = etatCivil.getTypeEtatCivil().asCore();
		// traitement de la séparation
		try {
			context.getMetierService().separe(menageCommun, dateEvt, null, etatCivilUnireg, false, getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}
}
