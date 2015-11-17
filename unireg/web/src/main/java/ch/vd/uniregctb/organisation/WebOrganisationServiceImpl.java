package ch.vd.uniregctb.organisation;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Re-organisation des informations de l'organisation
 *
 * @author Francois Dardare
 * @author Raphaël Marmier
 *
 */
public class WebOrganisationServiceImpl implements WebOrganisationService, MessageSourceAware {

	private ServiceOrganisationService serviceOrganisationService;
	private MessageSource messageSource;

	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * {@inheritDoc}
	 * @throws ObjectNotFoundException si on ne retrouve pas d'individu correspondant
	 */
	@Override
	public OrganisationView getOrganisation(Long numeroOrganisation) {
		final Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroOrganisation);
		if (organisation == null) {
			throw new ObjectNotFoundException(this.messageSource.getMessage("error.organisation.inexistant", new Object[] {Long.toString(numeroOrganisation)},  WebContextUtils.getDefaultLocale()));
		}

		// Copie les données de l'individu
		return alimenteOrganisationView(organisation);
	}

	/**
	 * Copie les propriétés d'une organisation du registre civil dans une vue OrganisationView du registre
	 * @param organisation l'organisation source
	 * @return la vue alimentée par les données actuelles de l'organisation
	 */
	private OrganisationView alimenteOrganisationView(final Organisation organisation) {
		final OrganisationView orgCible = new OrganisationView();
		orgCible.setNumeroOrganisation(organisation.getNumeroOrganisation());
		orgCible.setNom(organisation.getNom(null));
		orgCible.setAutoriteFiscale(organisation.getSiegePrincipal(null).getNoOfs());
		orgCible.setFormeJuridique(organisation.getFormeLegale(null).name());
		orgCible.setNumeroIDE(organisation.getNumeroIDE().isEmpty() ? null : organisation.getNumeroIDE().get(0).getPayload());
		final StatusRegistreIDE statusRegistreIDE = organisation.getSitePrincipal(null).getPayload().getDonneesRegistreIDE().getStatus(null);
		orgCible.setCanceled(statusRegistreIDE != null && statusRegistreIDE == StatusRegistreIDE.RADIE);
		orgCible.setNumeroOrganisationRemplacant(organisation.getRemplacePar(null));
		return orgCible;
	}
}
