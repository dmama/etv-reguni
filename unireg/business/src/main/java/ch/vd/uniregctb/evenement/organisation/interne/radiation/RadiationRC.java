package ch.vd.uniregctb.evenement.organisation.interne.radiation;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class RadiationRC extends EvenementOrganisationInterne {

	protected RadiationRC(EvenementOrganisation evenement, Organisation organisation,
	                      Entreprise entreprise, EvenementOrganisationContext context,
	                      EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		warnings.addWarning(String.format("Une vérification est requise pour contrôler la radiation de l'entreprise %s (civil: %s).",
		                                  getEntreprise().getNumero(), getEntreprise().getNumeroEntreprise()));
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		final RegDate dateApres = getDateEvt();

		final SiteOrganisation sitePrincipalApres = getOrganisation().getSitePrincipal(dateApres).getPayload();

		Assert.notNull(sitePrincipalApres.getDonneesRC().getStatus(dateApres));
	}
}
