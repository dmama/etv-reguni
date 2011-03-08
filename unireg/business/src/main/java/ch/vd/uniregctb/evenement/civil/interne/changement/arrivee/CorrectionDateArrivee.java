package ch.vd.uniregctb.evenement.civil.interne.changement.arrivee;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class CorrectionDateArrivee extends EvenementCivilInterne {

	protected CorrectionDateArrivee(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected CorrectionDateArrivee(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CORREC_DATE_ARRIVEE, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	private ForFiscalPrincipal getForFiscalPrincipalDeterminant(PersonnePhysique pp) {
		final EnsembleTiersCouple ensemble = context.getTiersService().getEnsembleTiersCouple(pp, null);
		final Contribuable ctbDeterminant;
		if (ensemble != null && ensemble.getMenage() != null) {
			ctbDeterminant = ensemble.getMenage();
		}
		else {
			ctbDeterminant = pp;
		}
		return ctbDeterminant != null ? ctbDeterminant.getDernierForFiscalPrincipal() : null;
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		// il se peut encore ici qu'aucun tiers ne soit trouvé avec ce numéro d'individu... si c'est le cas, l'erreur a déjà été logguée dans
		// le validateCommon, donc pas besoin de la logguer une nouvelle fois, si ?
		final PersonnePhysique pp = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());
		if (pp == null) {
			if (erreurs.isEmpty()) {
				erreurs.add(new EvenementCivilExterneErreur(String.format("Aucun tiers contribuable ne correspond au numero d'individu %d", getNoIndividu())));
			}
		}
		else {

			// on vérifie que le dernier for principal est bien un for ouvert pour motif "arrivée" ou "déménagement"
			final ForFiscalPrincipal ffp = getForFiscalPrincipalDeterminant(pp);
			if (ffp == null) {
				// si le tiers est mineur à la date d'arrivée (la nouvelle), on passe tout droit
				if (context.getTiersService().isMineur(pp, getDate())) {
					Audit.info(getNumeroEvenement(), "Individu mineur au moment de l'arrivée, événement ignoré.");
				}
				else {
					erreurs.add(new EvenementCivilExterneErreur("L'individu n'a pas de for fiscal principal connu."));
				}
			}
			else {

				// la commune d'annonce doit correspondre à la commune du for
				final Integer ofsCommuneAnnonce = getNumeroOfsCommuneAnnonce();
				if (ofsCommuneAnnonce == null) {
					erreurs.add(new EvenementCivilExterneErreur("L'identifiant de la commune d'annonce est vide."));
				}
				else {
					if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
						final String msg = String.format("Le dernier for principal du contribuable %s est hors-Suisse.", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
						erreurs.add(new EvenementCivilExterneErreur(msg));
					}
					else if (!ofsCommuneAnnonce.equals(ffp.getNumeroOfsAutoriteFiscale())) {
						final String msg = String.format("Le dernier for principal du contribuable %s n'est pas sur la commune d'annonce de l'événement.", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
						erreurs.add(new EvenementCivilExterneErreur(msg));
					}
					else {
						final MotifFor motifOuverture = ffp.getMotifOuverture();
						if (motifOuverture != MotifFor.ARRIVEE_HS && motifOuverture != MotifFor.ARRIVEE_HC && motifOuverture != MotifFor.DEMENAGEMENT_VD) {
							final String msg = String.format("Le dernier for principal sur le contribuable %s n'a pas été ouvert pour un motif d'arrivée (trouvé : %s).",
															FormatNumeroHelper.numeroCTBToDisplay(ffp.getTiers().getNumero()), motifOuverture.getDescription(true));
							erreurs.add(new EvenementCivilExterneErreur(msg));
						}
						else if (getDate().year() != ffp.getDateDebut().year()) {
							erreurs.add(new EvenementCivilExterneErreur(
									"La date d'ouverture du for principal ne peut pas changer d'année avec le traitement automatique. Veuillez traiter ce cas manuellement."));
						}
					}
				}
			}
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		// le dernier for principal doit voir sa date d'ouverture modifiée à la date de l'événement

		final PersonnePhysique pp = (PersonnePhysique) context.getTiersService().getTiers(getPrincipalPPId());
		final ForFiscalPrincipal ffp = getForFiscalPrincipalDeterminant(pp);
		if (ffp != null) {
			final Tiers tiersDeterminant = ffp.getTiers();
			final RegDate ancienneDateOuverture = ffp.getDateDebut();
			if (RegDateHelper.equals(ancienneDateOuverture, getDate())) {
				final String msg = String.format("La date d'ouverture du dernier for fiscal principal du contribuable %s est déjà au %s",
												FormatNumeroHelper.numeroCTBToDisplay(tiersDeterminant.getNumero()), RegDateHelper.dateToDisplayString(ancienneDateOuverture));
				Audit.info(getNumeroEvenement(), msg);
			}
			else {
				context.getTiersService().annuleForFiscal(ffp, false);
				context.getTiersService().addForPrincipal((Contribuable) tiersDeterminant, getDate(), ffp.getMotifOuverture(), ffp.getDateFin(), ffp.getMotifFermeture(), ffp.getMotifRattachement(),
						ffp.getNumeroOfsAutoriteFiscale(), ffp.getTypeAutoriteFiscale(), ffp.getModeImposition());
			}
		}
		return null;
	}
}
