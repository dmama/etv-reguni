package ch.vd.uniregctb.evenement.organisation.interne.passeplat;

import org.springframework.util.Assert;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.tiers.Entreprise;

import static ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class PassePlat extends EvenementOrganisationInterne {

	private final TypeInformationComplementaire typeInfo;

	protected PassePlat(EvenementOrganisation evenement, Organisation organisation,
	                    Entreprise entreprise, EvenementOrganisationContext context,
	                    EvenementOrganisationOptions options, TypeInformationComplementaire typeInfo) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		this.typeInfo = typeInfo;
	}

	public TypeInformationComplementaire getTypeInfo() {
		return typeInfo;
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		emetEvenementInformation();
	}

	private void emetEvenementInformation() {
		Audit.info(String.format("Envoi d'un événement d'information: %s. Entreprise %s (civil: %s).", typeInfo.name(), getEntreprise().getNumero(), getNoOrganisation()));
		context.getEvenementFiscalService().publierEvenementFiscalInformationComplementaire(getEntreprise(), typeInfo, getDateEvt());
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		Assert.notNull(typeInfo);
	}
}
