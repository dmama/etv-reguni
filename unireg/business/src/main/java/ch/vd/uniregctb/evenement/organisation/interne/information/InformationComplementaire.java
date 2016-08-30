package ch.vd.uniregctb.evenement.organisation.interne.information;

import org.springframework.util.Assert;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneInformationPure;
import ch.vd.uniregctb.tiers.Entreprise;

import static ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class InformationComplementaire extends EvenementOrganisationInterneInformationPure {

	private final TypeInformationComplementaire typeInfo;

	protected InformationComplementaire(EvenementOrganisation evenement, Organisation organisation,
	                                    Entreprise entreprise, EvenementOrganisationContext context,
	                                    EvenementOrganisationOptions options, TypeInformationComplementaire typeInfo) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		this.typeInfo = typeInfo;
	}

	public TypeInformationComplementaire getTypeInfo() {
		return typeInfo;
	}

	@Override
	public String describe() {
		return "Information complémentaire";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		String message = String.format("Envoi d'un événement d'information: %s. Entreprise n°%s (civil: %d).",
		                               typeInfo.toString(), FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero()), getNoOrganisation());
		emetEvtFiscalInformation(getDateEvt(), getEntreprise(), typeInfo, message, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		Assert.notNull(typeInfo);
	}
}
