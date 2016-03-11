package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import org.springframework.util.Assert;

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

/**
 * @author Raphaël Marmier, 2015-10-13
 */
public class DemenagementSansChangementDeTypeAutoriteFiscale extends Demenagement {

	public DemenagementSansChangementDeTypeAutoriteFiscale(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                                       EvenementOrganisationContext context,
	                                                       EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		final MotifFor motifFor = MotifFor.DEMENAGEMENT_VD;
		effectueChangementSiege(motifFor, getDateApres(), warnings, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		/*
		 Erreurs techniques fatale
		  */

		// On doit avoir deux sites
		Assert.notNull(getSitePrincipalAvant());
		Assert.notNull(getSitePrincipalApres());

		// On doit avoir deux autorités fiscales
		Assert.notNull(getSiegeAvant());
		Assert.notNull(getSiegeApres());

		// Quelque conditions non valides
		Assert.isTrue(getSiegeAvant() != getSiegeApres(), "Pas un déménagement de siège, la commune n'a pas changé!");
	}
}
