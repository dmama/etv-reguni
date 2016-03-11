package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
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
	                                   EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		MotifFor motifFor;
		RegDate dateDebutNouveauSiege;
		//
		if (getSiegeAvant().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				getSiegeApres().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
			motifFor = MotifFor.DEPART_HC;
			dateDebutNouveauSiege = getDateApres();
		} else {
		if (getSiegeAvant().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC &&
				getSiegeApres().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			motifFor = MotifFor.ARRIVEE_HC;
			dateDebutNouveauSiege = getDateApres();
			if (getEntreprise().getDernierForFiscalPrincipal() == null) {
				regleDateDebutPremierExerciceCommercial(getEntreprise(), getDateApres(), suivis);
			}
		} else
			throw new EvenementOrganisationException(String.format("Une combinaison non supportée de déplacement de siège est survenue. type avant: %s, type après: %s", getSiegeAvant().getTypeAutoriteFiscale(), getSiegeApres().getTypeAutoriteFiscale()));
		}

		effectueChangementSiege(motifFor, dateDebutNouveauSiege, warnings, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		/*
		 Erreurs techniques fatale
		  */

		// On doit avoir deux sites
		Assert.isTrue(
				getSitePrincipalAvant() != null && getEtablissementPrincipalApres() != null
		);

		// On doit avoir deux autorités fiscales
		Assert.isTrue(
				(getSiegeAvant() != null && getSiegeApres() != null)
		);

		// Quelque conditions non valides
		Assert.isTrue(getSiegeAvant() != getSiegeApres(), "Pas un déménagement de siège, la commune n'a pas changé!");
	}
}
