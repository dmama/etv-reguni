package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchContext;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.ArriveePrincipale;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

/**
 * Règles métiers permettant de traiter les événements de déménagement intra communal.
 * <p/>
 * [UNIREG-3379] ajouté un mécanisme de translation d'événements dans le cas des fusions de communes
 */
public class DemenagementTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		if (isDemenagementPrincipal(event, context)) {
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
		else if (isDemenagementSecondaire(event, context)) {
			// Démemnagement secondaire
			return new DemenagementSecondaire(event, context, options);
		}
		else {
			throw new EvenementCivilException(
					"L'individu n°" + event.getNumeroIndividuPrincipal() + " ne possède pas d'adresse principale ou secondaire qui commence le jour [" +
							RegDateHelper.dateToDisplayString(event.getDateEvenement()) + "] de son déménagement.");
		}
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilEchContext context, EvenementCivilOptions options) throws EvenementCivilException {

		if (isDemenagementPrincipal(event, context)) {
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
		else if (isDemenagementSecondaire(event, context)) {
			// Démemnagement secondaire
			return new DemenagementSecondaire(event, context, options);
		}
		else {
			throw new EvenementCivilException(
					"L'individu n°" + event.getNumeroIndividu() + " ne possède pas d'adresse principale ou secondaire qui commence le jour [" +
							RegDateHelper.dateToDisplayString(event.getDateEvenement()) + "] de son déménagement.");
		}
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilEchContext context) throws EvenementCivilException {
		final Communes communes = determineCommunesAvantEtApres(event, context);
		return (communes.avant == null || communes.apres == null || communes.avant.getNoOFSEtendu() == communes.apres.getNoOFSEtendu());
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
	 * @param event   l'événement de déménagement regPP
	 * @param context le context d'exécution
	 * @return les communes trouvées (qui peuvent être nulles)
	 * @throws EvenementCivilException un cas de problème
	 */
	private Communes determineCommunesAvantEtApres(EvenementCivilRegPP event, EvenementCivilContext context) throws EvenementCivilException {
		final Long principal = event.getNumeroIndividuPrincipal();
		final RegDate jourDemenagement = event.getDateEvenement();
		return determineCommmunesAvantApres(context, principal, jourDemenagement);
	}

	/**
	 * Détermine les communes de domicile d'un individu juste avant et juste après son déménagement.
	 *
	 * @param event   l'événement de déménagement eCH
	 * @param context le context d'exécution
	 * @return les communes trouvées (qui peuvent être nulles)
	 * @throws EvenementCivilException un cas de problème
	 */
	private Communes determineCommunesAvantEtApres(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		final Long principal = event.getNumeroIndividu();
		final RegDate jourDemenagement = event.getDateEvenement();
		return determineCommmunesAvantApres(context, principal, jourDemenagement);
	}

	/**
	 * Détermine les communes de domicile d'un individu juste avant et juste après son déménagement.
	 *
	 * @param context            le context d'execution
	 * @param principal          l'individu concerné par l'évenement
	 * @param jourDemenagement   jour de l'évenement
	 * @return les communes trouvées (qui peuvent être nulles)
	 * @throws EvenementCivilException
	 */
	private Communes determineCommmunesAvantApres(EvenementCivilContext context, Long principal, RegDate jourDemenagement) throws
			EvenementCivilException {

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

	/**
	 * Permet de determiner si l'évènement reçu est un déménagement PRINCIPAL.
	 *
	 * @param context le context d'execution
	 * @param principal le numéro de l'individu principal
	 * @return true si l'adresse principale a une date de début qui correspond au jour du déménagement
	 * @throws EvenementCivilException
	 */
	private boolean isDemenagementPrincipal(EvenementCivilContext context, Long principal, RegDate jourDemenagement) throws
			EvenementCivilException {
		try {
			final AdressesCivilesActives nouvelleAdresse = context.getServiceCivil().getAdresses(principal, jourDemenagement, false);

			final Adresse principale = nouvelleAdresse.principale;
			if (principale != null && principale.getDateDebut().equals(jourDemenagement)) {
				return true;
			}
			return false;
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilException(e);
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

	/**
	 * Permet de determiner si l'évènement reçu est un déménagement PRINCIPAL.
	 * @param event evenement civil eCH
	 * @param context le context d'execution
	 * @return true si l'adresse principale a une date de début qui correspond au jour du déménagement
	 * @throws EvenementCivilException
	 */
	private boolean isDemenagementPrincipal(EvenementCivilEch event, EvenementCivilContext context) throws
			EvenementCivilException {
		final RegDate jourDemenagement = event.getDateEvenement();
		final long principal = event.getNumeroIndividu();
		return isDemenagementPrincipal(context, principal, jourDemenagement);
	}


	/**
	 * Permet de determiner si l'évènement reçu est un déménagement PRINCIPAL.
	 * @param event evenement civil regPP
	 * @param context le context d'execution
	 * @return true si l'adresse principale a une date de début qui correspond au jour du déménagement
	 * @throws EvenementCivilException
	 */
	private boolean isDemenagementPrincipal(EvenementCivilRegPP event, EvenementCivilContext context) throws
			EvenementCivilException {
		final RegDate jourDemenagement = event.getDateEvenement();
		final long principal = event.getNumeroIndividuPrincipal();
		return isDemenagementPrincipal(context, principal, jourDemenagement);
	}


	/**
	 * Permet de determiner si l'évènement reçu est un déménagement secondaire.
	 * @param event evenement civil eCH
	 * @param context le context d'execution
	 * @return true si au moins une adresse secondaire a une date de début qui correspond au jour du déménagement
	 * @throws EvenementCivilException
	 */
	private boolean isDemenagementSecondaire(EvenementCivilEch event, EvenementCivilContext context) throws
			EvenementCivilException {
		final RegDate jourDemenagement = event.getDateEvenement();
		final long principal = event.getNumeroIndividu();
		return isDemenagementSecondaire(context, principal, jourDemenagement);
	}

	/**
	 * Permet de determiner si l'évènement reçu est un déménagement secondaire.
	 * @param event evenement civil regPP
	 * @param context le context d'execution
	 * @return true si au moins une adresse secondaire a une date de début qui correspond au jour du déménagement
	 * @throws EvenementCivilException
	 */
	private boolean isDemenagementSecondaire(EvenementCivilRegPP event, EvenementCivilContext context) throws
			EvenementCivilException {
		final RegDate jourDemenagement = event.getDateEvenement();
		final long principal = event.getNumeroIndividuPrincipal();
		return isDemenagementSecondaire(context, principal, jourDemenagement);
	}


	/**
	 * Permet de determiner si l'évènement reçu est un déménagement secondaire.
	 *
	 * @param context le context d'execution
	 * @return true si au moins une adresse secondaire a une date de début qui correspond au jour du déménagement
	 * @throws EvenementCivilException
	 */
	private boolean isDemenagementSecondaire(EvenementCivilContext context, Long principal, RegDate jourDemenagement) throws
			EvenementCivilException {
		try {
			final AdressesCivilesActives nouvelleAdresse = context.getServiceCivil().getAdresses(principal, jourDemenagement, false);

			List<Adresse> adressesSecondaires = nouvelleAdresse.secondaires;
			if (estVide(adressesSecondaires)) {
				return false;
			}

			for (Adresse adressesSecondaire : adressesSecondaires) {
				if (adressesSecondaire.getDateDebut().equals(jourDemenagement)) {
					return true;
				}
			}
			return false;
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilException(e);
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}


	private boolean estVide(List<Adresse> list) {
		return list == null || list.isEmpty();
	}

}
