package ch.vd.unireg.evenement.organisation.interne.demenagement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

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
			if (getSitePrincipalApres().isConnuInscritAuRC(getDateApres())) {
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
			if (getSitePrincipalApres().isConnuInscritAuRC(getDateApres())) {
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

		final TiersService tiersService = getContext().getTiersService();
		final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(getEntreprise(), getDateApres());

		// Création & vérification de la surcharge corrective s'il y a lieu
		if (dateDebutNouveauSiege.isBefore(getDateEvt())) {
			SurchargeCorrectiveRange surchargeCorrectiveRange = new SurchargeCorrectiveRange(dateDebutNouveauSiege, getDateEvt().getOneDayBefore());
			verifieSurchargeAcceptable(dateDebutNouveauSiege, surchargeCorrectiveRange);
			tiersService.fermeSurchargesCiviles(getEntreprise(), surchargeCorrectiveRange.getDateDebut().getOneDayBefore());
			tiersService.fermeSurchargesCiviles(etablissementPrincipal, surchargeCorrectiveRange.getDateDebut().getOneDayBefore());
			// TODO: Fermer les adresses en surcharge!
			appliqueDonneesCivilesSurPeriode(getEntreprise(), surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
		} else {
			tiersService.fermeSurchargesCiviles(getEntreprise(), getDateEvt().getOneDayBefore());
			tiersService.fermeSurchargesCiviles(etablissementPrincipal, getDateEvt().getOneDayBefore());
			// TODO: Fermer les adresses en surcharge!
		}

		effectueChangementSiege(motifFor, dateDebutNouveauSiege, warnings, suivis);
	}

}
