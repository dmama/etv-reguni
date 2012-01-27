package ch.vd.uniregctb.evenement.civil.interne.mouvement;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneAvecAdresses;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.Arrivee;
import ch.vd.uniregctb.evenement.civil.interne.depart.Depart;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

public abstract class Mouvement extends EvenementCivilInterneAvecAdresses {

	protected Mouvement(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected Mouvement(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Mouvement(Individu individu, Individu conjoint, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce,
	                    @Nullable Adresse adressePrincipale, @Nullable Adresse adresseSecondaire, @Nullable Adresse adresseCourrier, EvenementCivilContext context) {
		super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, adressePrincipale, adresseSecondaire, adresseCourrier, context);
	}

	protected final AdressesCiviles getAdresses(EvenementCivilContext context, RegDate date) throws EvenementCivilException {
		try {
			return new AdressesCiviles(context.getServiceCivil().getAdresses(getNoIndividu(), date, false));
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilException(e);
		}
	}

	protected final Commune getCommuneByAdresse(EvenementCivilContext context, Adresse adresse, RegDate date) throws EvenementCivilException {
		try {
			return context.getServiceInfra().getCommuneByAdresse(adresse, date);
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

	/**
	 * Permet de faire les verifications standards sur les adresses et les
	 * individus en cas de départ ou d'arrivée
	 *
	 * @param target
	 * @param regroupementObligatoire en cas où le regroupement de deux membres d'un couple est obligatoire pour le movement
	 * @param erreurs
	 * @param warnings
	 */
	protected void verifierMouvementIndividu(Mouvement mouvement, boolean regroupementObligatoire, EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) {

		String message = null;

		/*
		 * Vérifie les individus
		 */
		final ServiceCivilService serviceCivil = getService().getServiceCivilService();
		final Individu individuPrincipal = mouvement.getIndividu();

		if (individuPrincipal == null) {
			if (mouvement instanceof Depart) {
				message="Impossible de récupérer l'individu concerné par le départ";
			}else if (mouvement instanceof Arrivee) {
				message="Impossible de récupérer l'individu concerné par l'arrivé";
			}

			erreurs.addErreur(message);
		}
		else {
			final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(mouvement.getNoIndividu(), mouvement.getDate());
			if (etatCivil == null) {
				erreurs.addErreur("L'individu principal ne possède pas d'état civil à la date de l'événement");
			}

			if (EtatCivilHelper.estMarieOuPacse(etatCivil)) {
				/*
				 * si l'individu est marié ou pacsé, on vérifie que le conjoint est spécifié de manière cohérente
				 */
				final Individu conjointDeIndividu = serviceCivil.getConjoint(mouvement.getNoIndividu(),mouvement.getDate());
				final Individu conjointDeMouvement = mouvement.getConjoint();

				if (conjointDeIndividu == null && conjointDeMouvement == null) {
					/*
					 * nous avons un marie seul (= dont le conjoint n'habite pas dans le canton) -> rien à faire
					 */
				}
				else if (conjointDeIndividu != null && conjointDeMouvement == null) {
					if (regroupementObligatoire && !EtatCivilHelper.estSepare(individuPrincipal.getEtatCivil(mouvement.getDate()))) {
						if (mouvement instanceof Depart) {
							message="L'évenement de départ du conjoint n'a pas été reçu";

						}
						else if (mouvement instanceof Arrivee) {
							message="L'évenement d'arrivée du conjoint n'a pas été reçu";

						}
						erreurs.addErreur(message);
					}
				}
				else if (conjointDeIndividu == null && conjointDeMouvement != null) {
					erreurs.addErreur("Un conjoint est spécifié dans l'événement alors que l'individu principal n'en possède pas");
				}
				else {
					/*
					 * erreur si l'id du conjoint reçu ne correspond pas à celui de l'état civil
					 */
					if (conjointDeIndividu.getNoTechnique() != conjointDeMouvement.getNoTechnique()) {
						erreurs.addErreur("le conjoint déclaré dans l'événement et celui dans le registre civil diffèrent");
					}
				}
			}
		}
	}

}
