package ch.vd.uniregctb.evenement.civil.interne.mouvement;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneAvecAdressesBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.ArriveeAdapter;
import ch.vd.uniregctb.evenement.civil.interne.depart.DepartAdapter;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public abstract class MouvementAdapter extends EvenementCivilInterneAvecAdressesBase {

	protected MouvementAdapter(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected MouvementAdapter(Individu individu, Individu conjoint, TypeEvenementCivil typeEvenementCivil, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce,
	                           Adresse adressePrincipale, Adresse adresseSecondaire, Adresse adresseCourrier, EvenementCivilContext context) {
		super(individu, conjoint, typeEvenementCivil, dateEvenement, numeroOfsCommuneAnnonce, adressePrincipale, adresseSecondaire, adresseCourrier, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected MouvementAdapter(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, TypeEvenementCivil typeEvenementCivil, RegDate dateEvenement,
	                           Integer numeroOfsCommuneAnnonce, Adresse adressePrincipale, Adresse adresseSecondaire, Adresse adresseCourrier, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, typeEvenementCivil, dateEvenement, numeroOfsCommuneAnnonce, adressePrincipale, adresseSecondaire, adresseCourrier, context);
	}

	/**
	 * @return l'adresse principale de l'individu après le départ ou l'arrivée.
	 */
	protected abstract Adresse getNouvelleAdressePrincipale();

	/**
	 * Permet de faire les verifications standards sur les adresses et les
	 * individus en cas de départ ou d'arrivée
	 *
	 * @param target
	 * @param regroupementObligatoire en cas où le regroupement de deux membres d'un couple est obligatoire pour le movement
	 * @param erreurs
	 * @param warnings
	 */
	protected void verifierMouvementIndividu(MouvementAdapter mouvement, boolean regroupementObligatoire, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		String message = null;

		/*
		 * Vérifie les adresses
		 */
		if (mouvement.getNouvelleAdressePrincipale() == null) {
			if (mouvement instanceof DepartAdapter) {
				warnings.add(new EvenementCivilExterneErreur("La nouvelle adresse principale de l'individu est vide", TypeEvenementErreur.WARNING));
			}
			else if (mouvement instanceof ArriveeAdapter) {
				erreurs.add(new EvenementCivilExterneErreur("La nouvelle adresse principale de l'individu est vide"));
			}

		}

		if (mouvement.getNumeroOfsCommuneAnnonce() == null) {
			erreurs.add(new EvenementCivilExterneErreur("La commune d'annonce est vide"));
		}
		/*
		 * Vérifie les individus
		 */
		final ServiceCivilService serviceCivil = getService().getServiceCivilService();
		final Individu individuPrincipal = mouvement.getIndividu();

		if (individuPrincipal == null) {
			if (mouvement instanceof DepartAdapter) {
				message="Impossible de récupérer l'individu concerné par le départ";
			}else if (mouvement instanceof ArriveeAdapter) {
				message="Impossible de récupérer l'individu concerné par l'arrivé";
			}

			erreurs.add(new EvenementCivilExterneErreur(message));
		}
		else {
			final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(mouvement.getNoIndividu(), mouvement.getDate());
			if (etatCivil == null) {
				erreurs.add(new EvenementCivilExterneErreur("L'individu principal ne possède pas d'état civil à la date de l'événement"));
			}

			if (EtatCivilHelper.estMarieOuPacse(etatCivil)) {
				/*
				 * si l'individu est marié ou pacsé, on vérifie que le conjoint est spécifié de manière cohérente
				 */
				final Individu conjointDeIndividu =serviceCivil.getConjoint(mouvement.getNoIndividu(),mouvement.getDate());
				final Individu conjointDeMouvement = mouvement.getConjoint();

				if (conjointDeIndividu == null && conjointDeMouvement == null) {
					/*
					 * nous avons un marie seul (= dont le conjoint n'habite pas dans le canton) -> rien à faire
					 */
				}
				else if (conjointDeIndividu != null && conjointDeMouvement == null) {
					if (regroupementObligatoire && !EtatCivilHelper.estSepare(individuPrincipal.getEtatCivil(mouvement.getDate()))) {
						if (mouvement instanceof DepartAdapter) {
							message="L'évenement de départ du conjoint n'a pas été reçu";

						}
						else if (mouvement instanceof ArriveeAdapter) {
							message="L'évenement d'arrivée du conjoint n'a pas été reçu";

						}
						erreurs.add(new EvenementCivilExterneErreur(message));
					}
				}
				else if (conjointDeIndividu == null && conjointDeMouvement != null) {
					EvenementCivilExterneErreur erreur = new EvenementCivilExterneErreur(
							"Un conjoint est spécifié dans l'événement alors que l'individu principal n'en possède pas");
					erreurs.add(erreur);
				}
				else {
					/*
					 * erreur si l'id du conjoint reçu ne correspond pas à celui de l'état civil
					 */
					if (conjointDeIndividu.getNoTechnique() != conjointDeMouvement.getNoTechnique()) {
						EvenementCivilExterneErreur erreur = new EvenementCivilExterneErreur(
								"le conjoint déclaré dans l'événement et celui dans le registre civil diffèrent");
						erreurs.add(erreur);
					}
				}
			}
		}
	}

}
