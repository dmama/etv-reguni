package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import ch.vd.infrastructure.service.InfrastructureException;
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
import ch.vd.uniregctb.evenement.civil.interne.arrivee.Arrivee;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.Commune;

/**
 * Règles métiers permettant de traiter les événements de déménagement intra communal.
 * <p/>
 * [UNIREG-3379] ajouté un mécanisme de translation d'événements dans le cas des fusions de communes
 */
public class DemenagementTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		if (isArriveeDansCommuneNonEncoreFusionnee(event, context)) {
			// [UNIREG-3379] il s'agit d'un déménagement dans des communes fusionnées au niveau civil, mais pas encore fusionnées au niveau fiscal => on converti l'événement en arrivée.
			return new Arrivee(event, context, options);
		}
		else {
			// ok, il s'agit d'un déménagement conventionnel
			return new Demenagement(event, context, options);
		}
	}

	/**
	 * @param event   un événement d'arrivée externe
	 * @param context le context d'exécution
	 * @return <b>true</b> si l'événement est un déménagement dans des communes fusionnées au niveau civil, mais pas encore fusionnées au niveau fiscal; <b>faux</b> dans tous les autres cas.
	 * @throws EvenementCivilException en cas d'erreur
	 */
	private boolean isArriveeDansCommuneNonEncoreFusionnee(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {

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

			if (egidAvant == null || egidApres == null) {
				Audit.warn(event.getId(), "Le numéro de bâtiment (egid) n'est pas disponible avant et/ou après le déménagement de l'individu n°" + event.getNumeroIndividuPrincipal() +
						" : impossible de détecter si le déménagement correspond en fait à une arrivée dans une commune non-encore fusionnée fiscalement.");
				return false; // on considère qu'il s'agit d'un déménagement
			}

			final Commune communeAvant = context.getServiceInfra().getCommuneByEgid(egidAvant, veilleDemenagement, event.getNumeroOfsCommuneAnnonce());
			final Commune communeApres = context.getServiceInfra().getCommuneByEgid(egidApres, jourDemenagement, event.getNumeroOfsCommuneAnnonce());

			if (communeAvant == null) {
				throw new EvenementCivilException(
						"Impossible de récupérer la commune fiscale de l'individu n°" + principal + " la veille [" + RegDateHelper.dateToDisplayString(veilleDemenagement) + "] de son déménagement.");
			}
			if (communeApres == null) {
				throw new EvenementCivilException(
						"Impossible de récupérer la commune fiscale de l'individu n°" + principal + " le jour [" + RegDateHelper.dateToDisplayString(jourDemenagement) + "] de son déménagement.");
			}

			// si les communes avant et après déménagement sont différentes, c'est qu'il s'agit bien en fait d'une arrivée
			return communeAvant.getNoOFSEtendu() != communeApres.getNoOFSEtendu();
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilException(e);
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

}
