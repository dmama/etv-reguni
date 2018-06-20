package ch.vd.unireg.evenement.entreprise.interne.transformation;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.entreprise.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.PublicationBusiness;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénements portant sur la liquidation.
 *
 * @author Raphaël Marmier, 2016-02-18.
 */
public class LiquidationStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public LiquidationStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {
		if (entreprise == null) {
			return null;
		}

		final RegDate dateApres = event.getDateEvenement();
		final EtablissementCivil etablissementPrincipal = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload();

		final List<PublicationBusiness> publicationBusinessesDuJour = etablissementPrincipal.getPublications(event.getDateEvenement());
		final InscriptionRC inscriptionRC = etablissementPrincipal.getDonneesRC().getInscription(dateApres);
		final StatusInscriptionRC statusInscription = inscriptionRC != null ? inscriptionRC.getStatus() : null;
		final StatusRegistreIDE statusRegistreIDE = etablissementPrincipal.getDonneesRegistreIDE().getStatus(dateApres);

		if (statusInscription == StatusInscriptionRC.EN_LIQUIDATION
				&& statusRegistreIDE == StatusRegistreIDE.RADIE
				&& publicationBusinessesDuJour != null
				&& !publicationBusinessesDuJour.isEmpty()) {

			for (PublicationBusiness publication : publicationBusinessesDuJour) {
				if (publication.getTypeDeLiquidation() != null) { // Partant du principe qu'un seul type de liquidation ne peut avoir lieu sur un même jour, on renvoie le premier trouvé.
					switch (publication.getTypeDeLiquidation()) {
					case SOCIETE_ANONYME:
					case SOCIETE_RESPONSABILITE_LIMITE:
					case SOCIETE_COOPERATIVE:
					case ASSOCIATION:
					case FONDATION:
					case SOCIETE_NOM_COLLECTIF:
					case SOCIETE_COMMANDITE:
					case SOCIETE_COMMANDITE_PAR_ACTION:
						Audit.info(event.getId(), "Liquidation de l'entreprise détectée");
						return new Liquidation(event, entrepriseCivile, entreprise, context, options);
					default:
						final String message = String.format("Type de liquidation inconnu: %s", publication.getTypeDeLiquidation());
						Audit.info(event.getId(), message);
						return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
					}
				}
			}
		}
		return null;
	}
}
