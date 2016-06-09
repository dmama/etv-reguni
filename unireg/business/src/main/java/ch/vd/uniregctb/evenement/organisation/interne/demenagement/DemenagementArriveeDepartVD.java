package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2015-10-13
 */
public class DemenagementArriveeDepartVD extends Demenagement {

	public DemenagementArriveeDepartVD(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                   EvenementOrganisationContext context,
	                                   EvenementOrganisationOptions options,
	                                   Domicile siegeAvant,
	                                   Domicile siegeApres) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, siegeAvant, siegeApres);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		MotifFor motifFor;
		RegDate dateDebutNouveauSiege;
		//
		if (getSiegeAvant().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				getSiegeApres().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
			motifFor = MotifFor.DEPART_HC;
			if (getSitePrincipalApres().isInscritAuRC(getDateApres())) {
				final RegDate dateRadiationRCVd = getSitePrincipalApres().getDateRadiationRCVd(getDateApres());
				if (dateRadiationRCVd == null) {
					throw new EvenementOrganisationException("Date de radiation au registre vaudois du commerce introuvable pour l'établissement principal en partance.");
				}
				dateDebutNouveauSiege = dateRadiationRCVd;
			} else {
				dateDebutNouveauSiege = getDateApres();
			}
		} else {
		if (getSiegeAvant().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC &&
				getSiegeApres().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			motifFor = MotifFor.ARRIVEE_HC;
			if (getSitePrincipalApres().isInscritAuRC(getDateApres())) {
				final RegDate dateInscriptionRCVd = getSitePrincipalApres().getDateInscriptionRCVd(getDateApres());
				if (dateInscriptionRCVd == null) {
					throw new EvenementOrganisationException("Date d'inscription au registre vaudois du commerce introuvable pour l'établissement principal en arrivée.");
				}
				dateDebutNouveauSiege = dateInscriptionRCVd;
			} else {
				dateDebutNouveauSiege = getDateApres();
			}
			if (getEntreprise().getDernierForFiscalPrincipal() == null) {
				regleDateDebutPremierExerciceCommercial(getEntreprise(), getDateApres(), suivis);
			}
		} else
			throw new EvenementOrganisationException(String.format("Une combinaison non supportée de déplacement de siège est survenue. type avant: %s, type après: %s", getSiegeAvant().getTypeAutoriteFiscale(), getSiegeApres().getTypeAutoriteFiscale()));
		}

		// Création & vérification de la surcharge corrective s'il y a lieu
		if (dateDebutNouveauSiege.isBefore(getDateEvt())) {
			SurchargeCorrectiveRange surchargeCorrectiveRange = new SurchargeCorrectiveRange(dateDebutNouveauSiege, getDateEvt().getOneDayBefore());
			verifieSurchargeAcceptable(dateDebutNouveauSiege, surchargeCorrectiveRange);
			appliqueDonneesCivilesSurPeriode(getEntreprise(), surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
		}

		effectueChangementSiege(motifFor, dateDebutNouveauSiege, warnings, suivis);
	}

	private void verifieSurchargeAcceptable(RegDate dateDebutNouveauSiege, SurchargeCorrectiveRange surchargeCorrectiveRange) throws EvenementOrganisationException {
		if (!surchargeCorrectiveRange.isAcceptable()) {
			throw new EvenementOrganisationException(
					String.format("Refus de créer dans Unireg une entreprise dont le déménagement semble remonter à %s, %d jours avant la date de l'événement. La tolérance étant de %d jours. " +
							              "Il y a probablement une erreur d'identification ou un problème de date.",
					              RegDateHelper.dateToDisplayString(dateDebutNouveauSiege),
					              surchargeCorrectiveRange.getEtendue(),
					              OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC)
			);
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings, suivis);
	}
}
