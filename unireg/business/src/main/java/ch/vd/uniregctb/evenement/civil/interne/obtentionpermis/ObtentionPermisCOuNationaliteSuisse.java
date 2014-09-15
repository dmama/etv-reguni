package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneAvecAdresses;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.metier.common.DecalageDateHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règles métiers permettant de traiter les événements suivants :
 * - obtention de permis C
 * - obtention de la nationalité suisse
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public abstract class ObtentionPermisCOuNationaliteSuisse extends EvenementCivilInterneAvecAdresses {

	protected ObtentionPermisCOuNationaliteSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected ObtentionPermisCOuNationaliteSuisse(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ObtentionPermisCOuNationaliteSuisse(Individu individu, Individu conjoint, TypeEvenementCivil typeEvenementCivil, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce,
	                                              Adresse adressePrincipale, Adresse adresseSecondaire, Adresse adresseCourrier, EvenementCivilContext context) {
		super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, adressePrincipale, adresseSecondaire, adresseCourrier, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector errors, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * L'evenenement est mis en erreur dans les cas suivants
		 */

		// [SIFISC-9211] si on ne veut pas avoir à ouvrir des fors dans le futur, on ne peut pas traiter les obtentions de permis/nationalité
		// dès le jour de leur réalisation (voir SIFISC-35)
		// [SIFISC-10518] cette limitation n'est valable qu'avant 2014...
		if (DecalageDateHelper.getDateOuvertureForOrdinaireApresPermisCNationaliteSuisse(getDate()).isAfter(RegDate.get())) {
			errors.addErreur("Une obtention de permis ou de nationalité ne peut être traitée qu'à partir du lendemain de sa date d'effet");
		}

		/*
		 * Il n'existe pas de tiers contribuable correspondant à l'individu,
		 * assujetti ou non (mineur, conjoint) correspondant à l'individu.
		 */
		Individu individu = getIndividu();
		PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), errors);

		if (habitant == null) {
			return;
		}

		EnsembleTiersCouple etc = context.getTiersService().getEnsembleTiersCouple(habitant, getDate().getOneDayBefore());
		PersonnePhysique conjoint =null;
		MenageCommun couple = null;
		if (etc != null) {
			conjoint = etc.getConjoint(habitant);
			couple = etc.getMenage();
		}
		final DecisionAci decisionAci = habitant.getDecisionAciValideAt(getDate());
		if (decisionAci != null) {
			errors.addErreur(String.format("Le contribuable trouvé (%s) fait l'objet d'une décision ACI (%s)",
					FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero()),decisionAci));
		}

		if (conjoint != null) {
			final ch.vd.uniregctb.tiers.DecisionAci decisionAciConjoint = conjoint.getDecisionAciValideAt(getDate());
			if (decisionAciConjoint != null) {
				errors.addErreur(String.format("Le contribuable trouvé (%s) a un conjoint (%s) qui fait l'objet d'une décision ACI (%s)",
						FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero()),FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()),decisionAciConjoint));
			}

		}

		if (couple != null) {
			final DecisionAci decisionSurCouple = couple.getDecisionAciValideAt(getDate());
			if (decisionSurCouple != null) {
				errors.addErreur(String.format("Le contribuable trouvé (%s) appartient à un ménage  (%s) qui fait l'objet d'une décision ACI (%s)",
						FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero()),FormatNumeroHelper.numeroCTBToDisplay(couple.getNumero()),decisionSurCouple));
			}
		}

	}

	/**
	 * Méthode utilitaire pour continuer/remplacer un for fiscal principal existant
	 * en changeant juste le mode d'imposition, la date et le motif d'ouverture
	 */
	private void openForFiscalPrincipalChangementModeImposition(Contribuable contribuable,
	                                                            ForFiscalPrincipal reference,
	                                                            RegDate dateOuverture,
	                                                            MotifFor motifOuverture,
	                                                            ModeImposition nouveauModeImposition) {
		// [UNIREG-1979] On schedule un réindexation pour le début du mois suivant (les changements d'assujettissement source->ordinaire sont décalés en fin de mois)
		final RegDate debutMoisProchain = DecalageDateHelper.getDateDebutAssujettissementOrdinaireApresPermisCNationaliteSuisse(dateOuverture);
		contribuable.scheduleReindexationOn(debutMoisProchain);

		openForFiscalPrincipal(contribuable, dateOuverture, reference.getTypeAutoriteFiscale(), reference.getNumeroOfsAutoriteFiscale(), reference.getMotifRattachement(), motifOuverture, nouveauModeImposition);
	}

	/**
	 * Ouvre un nouveau for fiscal principal (vaudois + imposition ordinaire) sur un contribuable  non-assujetti (= aucun for fiscal) suite à l'obtention d'un permis C. Ce contribuable est considéré
	 * comme sourcier <b>implicite</b> par l'algorithme de calcul de l'assujettissement puisqu'il devient ordinaire suite à l'obtention d'un permis C (voir [SIFISC-1199]).
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @return le nouveau for fiscal principal
	 */
	private ForFiscalPrincipal openForFiscalPrincipalChangementModeImpositionImplicite(Contribuable contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale) {
		// [UNIREG-1979][SIFISC-1199] On schedule un réindexation pour le début du mois suivant (les changements d'assujettissement source->ordinaire sont décalés en fin de mois)
		final RegDate debutMoisProchain = DecalageDateHelper.getDateDebutAssujettissementOrdinaireApresPermisCNationaliteSuisse(dateOuverture);
		contribuable.scheduleReindexationOn(debutMoisProchain);

		return openForFiscalPrincipal(contribuable, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsAutoriteFiscale, MotifRattachement.DOMICILE, MotifFor.PERMIS_C_SUISSE,
				ModeImposition.ORDINAIRE);
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 */
	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// Recupere le tiers correspondant a l'individu
		final Individu individu = getIndividu();
		final PersonnePhysique habitant = getPersonnePhysiqueOrThrowException(individu.getNoTechnique());

		// [SIFISC-9211] : le for ordinaire ne doit être ouvert qu'au lendemain de l'obtention du permis/de la nationalité
		// [SIFISC-10518] : le SIFISC-9211 ne s'applique qu'à avant 2014
		final RegDate datePriseEnCompte = DecalageDateHelper.getDateOuvertureForOrdinaireApresPermisCNationaliteSuisse(getDate());
		final ForFiscalPrincipal forPrincipalHabitant = habitant.getForFiscalPrincipalAt(null);
		final EnsembleTiersCouple ensembleTiersCouple = getService().getEnsembleTiersCouple(habitant, datePriseEnCompte);
		MenageCommun menage = null;
		if (ensembleTiersCouple != null) {
			menage = ensembleTiersCouple.getMenage();
		}

		if (forPrincipalHabitant != null) { //individu seul assujetti
			final EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
			if (etatCivilIndividu == null) {
				throw new EvenementCivilException("Impossible de récupérer l'état civil courant de l'individu");
			}

			if (EtatCivilHelper.estMarieOuPacse(etatCivilIndividu)){
				//le for devrait être sur le menage commun
				throw new EvenementCivilException("Un individu avec conjoint non séparé possède un for principal individuel actif");
			}

			final ModeImposition modeImposition = forPrincipalHabitant.getModeImposition();
			if (modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 || modeImposition == ModeImposition.MIXTE_137_1) {
				// obtention permis ou nationalité le jour de l'arrivée
				if (forPrincipalHabitant.getDateDebut().isAfterOrEqual(datePriseEnCompte) &&
						(MotifFor.ARRIVEE_HC == forPrincipalHabitant.getMotifOuverture() || MotifFor.ARRIVEE_HS == forPrincipalHabitant.getMotifOuverture())) {
					getService().annuleForFiscal(forPrincipalHabitant);
					openForFiscalPrincipalChangementModeImposition(habitant, forPrincipalHabitant, forPrincipalHabitant.getDateDebut(), forPrincipalHabitant.getMotifOuverture(), ModeImposition.ORDINAIRE);
					Audit.info(getNumeroEvenement(), "Mise au rôle ordinaire de l'individu");
				} else {
					closeForFiscalPrincipal(habitant, datePriseEnCompte.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE);
					openForFiscalPrincipalChangementModeImposition(habitant, forPrincipalHabitant, datePriseEnCompte, MotifFor.PERMIS_C_SUISSE, ModeImposition.ORDINAIRE);
					Audit.info(getNumeroEvenement(), "Mise à jour du for principal de l'individu au rôle ordinaire");
				}
			}

			// [UNIREG-725] si déjà ordinaire, indigent ou à la dépense : rien à faire!
		}
		else if (menage != null && menage.getForFiscalPrincipalAt(null) != null) { //couple assujetti
			final ForFiscalPrincipal forPrincipalMenage = menage.getForFiscalPrincipalAt(null);
			final ModeImposition modeImposition = forPrincipalMenage.getModeImposition();
			if(modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 || modeImposition == ModeImposition.MIXTE_137_1) {
				//obtention permis ou nationalité le jour de l'arrivée
				if (forPrincipalMenage.getDateDebut().isAfterOrEqual(datePriseEnCompte) &&
						(MotifFor.ARRIVEE_HC == forPrincipalMenage.getMotifOuverture() || MotifFor.ARRIVEE_HS == forPrincipalMenage.getMotifOuverture())) {
					getService().annuleForFiscal(forPrincipalMenage);
					openForFiscalPrincipalChangementModeImposition(menage, forPrincipalMenage, forPrincipalMenage.getDateDebut(), forPrincipalMenage.getMotifOuverture(), ModeImposition.ORDINAIRE);
					Audit.info(getNumeroEvenement(), "Mise au role ordinaire du ménage");
				} else {
					closeForFiscalPrincipal(menage, datePriseEnCompte.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE);
					openForFiscalPrincipalChangementModeImposition(menage, forPrincipalMenage, datePriseEnCompte, MotifFor.PERMIS_C_SUISSE, ModeImposition.ORDINAIRE);
					Audit.info(getNumeroEvenement(), "Mise à jour du for principal du ménage au rôle ordinaire");
				}
			}
			//else pas de changement du mode d'imposition
		}
		else if (!FiscalDateHelper.isMajeurAt(individu, datePriseEnCompte)) { //individu mineur non assujetti
			Audit.info(getNumeroEvenement(), "Individu mineur non assujetti, il reste non assujetti");
		}
		else { //individu majeur non assujetti
			final EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
			if (etatCivilIndividu == null) {
				throw new EvenementCivilException("Impossible de récupérer l'état civil courant de l'individu");
			}

			int noOfs = 0;
			if(this instanceof ObtentionNationalite){
				noOfs = ((ObtentionNationalite) this).getNumeroOfsCommunePrincipale();
			}
			else if(this instanceof ObtentionPermis){
				noOfs = ((ObtentionPermis) this).getNumeroOfsCommunePrincipale();
			}

			if (noOfs == 0) {
				// récupération du numero OFS de la commune à partir de l'adresse du tiers
				try {
					final AdresseGenerique adresse = context.getAdresseService().getAdresseFiscale(habitant, TypeAdresseFiscale.DOMICILE, datePriseEnCompte, false);
					if (adresse != null) {
						final Commune commune = context.getServiceInfra().getCommuneByAdresse(adresse, datePriseEnCompte);
						// uniquement si la commune de domicile est vaudoise
						if (commune != null && commune.isVaudoise()) {
							noOfs = commune.getNoOFS();
						}
					}
				}
				catch (AdresseException e) {
					throw new EvenementCivilException("Impossible de récupérer l'adresse", e);
				} catch (ServiceInfrastructureException e) {
					throw new EvenementCivilException("Impossible de récupérer la commune", e);
				}
			}
			else {
				// on vérifie que la commune principale est bien vaudoise...
				try {
					final Commune commune = context.getServiceInfra().getCommuneByNumeroOfs(noOfs, datePriseEnCompte);
					if (commune == null || !commune.isVaudoise()) {
						noOfs = 0;
					}
				}
				catch (ServiceInfrastructureException e) {
					throw new EvenementCivilException("Impossible de récupérer la commune", e);
				}
			}

			// ouverture d'un for si la commune est vaudoise
			if (noOfs != 0) {
				if (noOfs == NO_OFS_FRACTION_SENTIER) {
					warnings.addWarning("Ouverture d'un for dans la fraction de commune du Sentier: veuillez vérifier la fraction de commune du for principal");
				}
				if (EtatCivilHelper.estMarieOuPacse(etatCivilIndividu)) { // le for est ouvert sur le ménage commun
					if (menage != null) {
						openForFiscalPrincipalChangementModeImpositionImplicite(menage, datePriseEnCompte, noOfs);
						Audit.info(getNumeroEvenement(), "Ouverture du for principal du ménage au rôle ordinaire");
					}
					else {
						throw new EvenementCivilException("L'individu est marié ou en partenariat enregistré mais ne possède pas de ménage commun");
					}
				}
				else { // le for est ouvert sur l'individu
					openForFiscalPrincipalChangementModeImpositionImplicite(habitant, datePriseEnCompte, noOfs);
					Audit.info(getNumeroEvenement(), "Ouverture du for principal de l'individu au rôle ordinaire");
				}
			}
			else {
				Audit.info(getNumeroEvenement(), "Domicile hors du territoire cantonal : pas d'ouverture de for");
			}
		}
		return HandleStatus.TRAITE;
	}
}
