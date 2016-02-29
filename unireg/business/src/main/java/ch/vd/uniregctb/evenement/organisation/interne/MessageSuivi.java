package ch.vd.uniregctb.evenement.organisation.interne;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Evénement interne sans effet servant publier un message en tant que suivi. Cet événement interne a pour unique conséquence
 * l'ajout d'une ligne dans les messages affichés à l'utilisateur.
 * <p>
 *     Cette classe d'événement est utile pour communiquer avec l'utilisateur des informations importantes en lien avec
 *     la détermination des événements internes par les stratégies. Notamment pour communiquer des décisions prises qui
 *     ont un impact sur le résultat final mais qui ne sont pas perceptibles depuis les événements eux-mêmes.
 * </p>
 */
public class MessageSuivi extends EvenementOrganisationInterneDeTraitement {

	private String suivi = null;

	public MessageSuivi(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
	                    EvenementOrganisationOptions options, String suivi) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		this.suivi = suivi;
	}

	@Override
	public String describe() {
		return null; // On ne veut pas de message descriptif sur cet événement qui n'en est pas un.
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		if (StringUtils.isNotBlank(suivi)) {
			suivis.addSuivi(suivi);
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
	}
}
