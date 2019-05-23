package ch.vd.unireg.evenement.civil.interne.obtentionpermis;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.FiscalDateHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterneAvecAdresses;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.metier.common.DecalageDateHelper;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;

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
		}
		verifierPresenceDecisionEnCours(habitant,getDate());
		verifierPresenceDecisionsEnCoursSurCouple(habitant);

		if (conjoint != null) {
			verifierPresenceDecisionEnCours(conjoint, habitant,getDate());

		}

	}

	/**
	 * Méthode utilitaire pour continuer/remplacer un for fiscal principal existant
	 * en changeant juste le mode d'imposition, la date et le motif d'ouverture
	 */
	private void openForFiscalPrincipalChangementModeImposition(ContribuableImpositionPersonnesPhysiques contribuable,
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
	private ForFiscalPrincipalPP openForFiscalPrincipalChangementModeImpositionImplicite(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale) {
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
		final ForFiscalPrincipalPP forPrincipalHabitant = habitant.getForFiscalPrincipalAt(null);
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
					context.audit.info(getNumeroEvenement(), "Mise au rôle ordinaire de l'individu");
				} else {
					closeForFiscalPrincipal(habitant, datePriseEnCompte.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE);
					openForFiscalPrincipalChangementModeImposition(habitant, forPrincipalHabitant, datePriseEnCompte, MotifFor.PERMIS_C_SUISSE, ModeImposition.ORDINAIRE);
					context.audit.info(getNumeroEvenement(), "Mise à jour du for principal de l'individu au rôle ordinaire");
				}
			}

			// [UNIREG-725] si déjà ordinaire, indigent ou à la dépense : rien à faire!
		}
		else if (menage != null && menage.getForFiscalPrincipalAt(null) != null) { //couple assujetti
			final ForFiscalPrincipalPP forPrincipalMenage = menage.getForFiscalPrincipalAt(null);
			final ModeImposition modeImposition = forPrincipalMenage.getModeImposition();
			if(modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 || modeImposition == ModeImposition.MIXTE_137_1) {
				//obtention permis ou nationalité le jour de l'arrivée
				if (forPrincipalMenage.getDateDebut().isAfterOrEqual(datePriseEnCompte) &&
						(MotifFor.ARRIVEE_HC == forPrincipalMenage.getMotifOuverture() || MotifFor.ARRIVEE_HS == forPrincipalMenage.getMotifOuverture())) {
					getService().annuleForFiscal(forPrincipalMenage);
					openForFiscalPrincipalChangementModeImposition(menage, forPrincipalMenage, forPrincipalMenage.getDateDebut(), forPrincipalMenage.getMotifOuverture(), ModeImposition.ORDINAIRE);
					context.audit.info(getNumeroEvenement(), "Mise au role ordinaire du ménage");
				} else {
					closeForFiscalPrincipal(menage, datePriseEnCompte.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE);
					openForFiscalPrincipalChangementModeImposition(menage, forPrincipalMenage, datePriseEnCompte, MotifFor.PERMIS_C_SUISSE, ModeImposition.ORDINAIRE);
					context.audit.info(getNumeroEvenement(), "Mise à jour du for principal du ménage au rôle ordinaire");
				}
			}
			//else pas de changement du mode d'imposition
		}
		else if (!FiscalDateHelper.isMajeurAt(individu, datePriseEnCompte)) { //individu mineur non assujetti
			context.audit.info(getNumeroEvenement(), "Individu mineur non assujetti, il reste non assujetti");
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

					// [SIFISC-24702] Seules les véritables adresses (= non-défaut) doivent être prises en compte...
					if (adresse != null && !adresse.isDefault()) {
						final Commune commune = context.getServiceInfra().getCommuneByAdresse(adresse, datePriseEnCompte);
						// uniquement si la commune de domicile est vaudoise
						if (commune != null && commune.isVaudoise()) {
							noOfs = commune.getNoOFS();
						}
					}
				}
				catch (AdresseException e) {
					throw new EvenementCivilException("Impossible de récupérer l'adresse", e);
				} catch (InfrastructureException e) {
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
				catch (InfrastructureException e) {
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
						context.audit.info(getNumeroEvenement(), "Ouverture du for principal du ménage au rôle ordinaire");
					}
					else {
						throw new EvenementCivilException("L'individu est marié ou en partenariat enregistré mais ne possède pas de ménage commun");
					}
				}
				else { // le for est ouvert sur l'individu
					openForFiscalPrincipalChangementModeImpositionImplicite(habitant, datePriseEnCompte, noOfs);
					context.audit.info(getNumeroEvenement(), "Ouverture du for principal de l'individu au rôle ordinaire");
				}
			}
			else {
				context.audit.info(getNumeroEvenement(), "Domicile hors du territoire cantonal : pas d'ouverture de for");
			}
		}
		return HandleStatus.TRAITE;
	}
}
