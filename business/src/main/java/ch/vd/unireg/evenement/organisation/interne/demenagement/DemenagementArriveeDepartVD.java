package ch.vd.unireg.evenement.organisation.interne.demenagement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2015-10-13
 */
public class DemenagementArriveeDepartVD extends Demenagement {

	public DemenagementArriveeDepartVD(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                                   EvenementEntrepriseContext context,
	                                   EvenementEntrepriseOptions options,
	                                   Domicile siegeAvant,
	                                   Domicile siegeApres) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options, siegeAvant, siegeApres);
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

		MotifFor motifFor;
		RegDate dateDebutNouveauSiege;
		//
		if (getSiegeAvant().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				getSiegeApres().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
			motifFor = MotifFor.DEPART_HC;
			if (getEtablissementCivilPrincipalApres().isConnuInscritAuRC(getDateApres())) {
				final RegDate dateRadiationRCVd = getEtablissementCivilPrincipalApres().getDateRadiationRCVd(getDateApres());
				if (dateRadiationRCVd == null) {
					throw new EvenementEntrepriseException("Date de radiation au registre vaudois du commerce introuvable pour l'établissement principal en partance.");
				}
				dateDebutNouveauSiege = dateRadiationRCVd;
			} else {
				dateDebutNouveauSiege = getDateApres();
			}
		} else {
		if (getSiegeAvant().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC &&
				getSiegeApres().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			motifFor = MotifFor.ARRIVEE_HC;
			if (getEtablissementCivilPrincipalApres().isConnuInscritAuRC(getDateApres())) {
				final RegDate dateInscriptionRCVd = getEtablissementCivilPrincipalApres().getDateInscriptionRCVd(getDateApres());
				if (dateInscriptionRCVd == null) {
					throw new EvenementEntrepriseException("Date d'inscription au registre vaudois du commerce introuvable pour l'établissement principal en arrivée.");
				}
				dateDebutNouveauSiege = dateInscriptionRCVd;
			} else {
				dateDebutNouveauSiege = getDateApres();
			}
			if (getEntreprise().getDernierForFiscalPrincipal() == null) {
				regleDateDebutPremierExerciceCommercial(getEntreprise(), getDateApres(), suivis);
			}
		} else
			throw new EvenementEntrepriseException(String.format("Une combinaison non supportée de déplacement de siège est survenue. type avant: %s, type après: %s", getSiegeAvant().getTypeAutoriteFiscale(), getSiegeApres().getTypeAutoriteFiscale()));
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
