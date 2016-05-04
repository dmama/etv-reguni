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
 * <p>
 *     Evénement interne sans effet servant publier un message en tant que warning <strong>avant</strong> le début de l'exécution proprement dite.
 *     Cet événement interne a deux effets: 1) l'ajout d'une ligne dans les messages affichés à l'utilisateur,
 *     2) le status "à vérifier" à la fin du traitement.
 * </p>
 * <p>
 *     Cette classe d'événement est utile pour communiquer avec l'utilisateur des informations importantes en lien avec
 *     la détermination des événements internes par les stratégies. Mais à la différence de {@link MessageSuivi},
 *     cet événement impose le status "vérifier" d'entrée de jeu. Il doit donc servir à attirer l'attention de l'utilisateur sur
 *     une situation qui requiert potentiellement son intervention.
 * </p>
 * <p>
 *     Les messages sont ajoutés au cours de l'étape de validation préliminaire et non en cours d'execution [handle()].
 *     De cette manière, les messages sont toujours affichés même lorsque l'exécution des traitements n'est pas possible.
 * </p>
 */
public class WarningPreExecution extends EvenementOrganisationInterneDeTraitement {

	private String suivi = null;

	public WarningPreExecution(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
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
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		if (StringUtils.isNotBlank(suivi)) {
			suivis.addSuivi(suivi);
		}
	}
}
