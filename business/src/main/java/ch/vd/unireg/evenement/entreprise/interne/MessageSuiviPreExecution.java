package ch.vd.unireg.evenement.entreprise.interne;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * <p>
 *     Evénement interne sans effet servant publier un message en tant que suivi <strong>avant</strong> le début de l'exécution proprement dite.
 *     Cet événement interne a pour unique conséquence l'ajout d'une ligne dans les messages affichés à l'utilisateur.
 * </p>
 * <p>
 *     Cette classe d'événement est utile pour communiquer avec l'utilisateur des informations importantes en lien avec
 *     la détermination des événements internes par les stratégies. Notamment pour communiquer des décisions prises qui
 *     ont un impact sur le résultat final mais qui ne sont pas raportées par ailleurs.
 * </p>
 * <p>
 *     Les messages sont ajoutés au cours de l'étape de validation préliminaire et non en cours d'execution [handle()].
 *     De cette manière, les messages sont toujours affichés même lorsque l'exécution des traitements n'est pas possible.
 * </p>
 */
public class MessageSuiviPreExecution extends EvenementEntrepriseInterneDeTraitement {

	private String suivi = null;

	public MessageSuiviPreExecution(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise, EvenementEntrepriseContext context,
	                                EvenementEntrepriseOptions options, String suivi) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
		this.suivi = suivi;
	}

	@Override
	public String describe() {
		return null; // On ne veut pas de message descriptif sur cet événement qui n'en est pas un.
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		if (StringUtils.isNotBlank(suivi)) {
			suivis.addSuivi(suivi);
		}
	}
}
