package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.ArriveePrincipale;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

/**
 * Règles métiers permettant de traiter les événements de déménagement intra communal.
 * <p/>
 * [UNIREG-3379] ajouté un mécanisme de translation d'événements dans le cas des fusions de communes
 */
public class DemenagementTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		final Communes communes = determineCommunesAvantEtApres(event, context);

		if (communes.avant == null || communes.apres == null) {
			final String message = "Le numéro de bâtiment (egid) n'est pas disponible avant et/ou après le déménagement de l'individu.";
			Audit.warn(event.getId(), message);
			event.setCommentaireTraitement(message);
			return new Demenagement(event, context, options);
		}
		else if (communes.avant.getNoOFSEtendu() != communes.apres.getNoOFSEtendu()) {
			// [UNIREG-3379] il s'agit d'un déménagement dans des communes fusionnées au niveau civil, mais pas encore fusionnées au niveau fiscal => on converti l'événement en arrivée.
			final String message = String.format("Traité comme une arrivée car les communes %s et %s ne sont pas encore fusionnées du point-de-vue fiscal.",
			                                     communes.avant.getNomMinuscule(), communes.apres.getNomMinuscule());
			Audit.info(event.getId(), message);
			event.setCommentaireTraitement(message);
			return new ArriveePrincipale(event, context, options);
		}
		else {
			// ok, il s'agit d'un déménagement conventionnel
			return new Demenagement(event, context, options);
		}
	}

	private static class Communes {

		final Commune avant;
		final Commune apres;

		private Communes(Commune communeAvant, Commune communeApres) {
			this.avant = communeAvant;
			this.apres = communeApres;
		}
	}

	/**
	 * Détermine les communes de domicile d'un individu juste avant et juste après son déménagement.
	 *
	 * @param event   l'événement de déménagement
	 * @param context le context d'exécution
	 * @return les communes trouvées (qui peuvent être nulles)
	 * @throws EvenementCivilException un cas de problème
	 */
	private Communes determineCommunesAvantEtApres(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {
		final Long principal = event.getNumeroIndividuPrincipal();
		final RegDate jourDemenagement = event.getDateEvenement();
		final RegDate veilleDemenagement = jourDemenagement.getOneDayBefore();
		try {
			final AdressesCivilesActives adresseAvant = context.getServiceCivil().getAdresses(principal, veilleDemenagement, false);
			final AdressesCivilesActives adresseApres = context.getServiceCivil().getAdresses(principal, jourDemenagement, false);

			if (adresseAvant.principale == null) {
				throw new EvenementCivilException(
						"L'individu n°" + principal + " ne possède pas d'adresse de domicile la veille [" + RegDateHelper.dateToDisplayString(veilleDemenagement) + "] de son déménagement.");
			}
			if (adresseApres.principale == null) {
				throw new EvenementCivilException(
						"L'individu n°" + principal + " ne possède pas d'adresse de domicile le jour [" + RegDateHelper.dateToDisplayString(jourDemenagement) + "] de son déménagement.");
			}

			final Integer egidAvant = adresseAvant.principale.getEgid();
			final Integer egidApres = adresseApres.principale.getEgid();


			final Commune communeAvant = (egidAvant == null ? null : context.getServiceInfra().getCommuneByEgid(egidAvant, veilleDemenagement));
			final Commune communeApres = (egidApres == null ? null : context.getServiceInfra().getCommuneByEgid(egidApres, jourDemenagement));

			return new Communes(communeAvant, communeApres);
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilException(e);
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

}
