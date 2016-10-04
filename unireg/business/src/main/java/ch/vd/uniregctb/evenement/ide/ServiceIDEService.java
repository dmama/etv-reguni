package ch.vd.uniregctb.evenement.ide;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2016-09-02, <raphael.marmier@vd.ch>
 */
public interface ServiceIDEService {

	/**
	 * Détermine si Unireg est service IDE à responsabilité étendue pour l'entreprise mentionnée
	 * pour la date donnée. Un service a responsabilité étendue est chargé d'annoncer à l'IDE les
	 * changements survenant dans les caractéristiques des entreprises dont il est responsable.
	 *
	 * @param entreprise l'entreprise
	 * @param date la date
	 * @return true si Unireg est service IDE à responsabilité étendue à cette date pour l'établissement.
	 */
	boolean isServiceIDEObligEtendues(Entreprise entreprise, RegDate date);

	/**
	 * <p>
	 *     Evalue s'il faut envoyer une annonce à l'IDE pour une entreprise, et la valide et l'expédie le cas échéant.
	 * </p>
	 * <p>
	 *     Unireg a un rôle de service IDE à obligations étendues pour certains types d'entreprises. Auquel cas,
	 *     annonce à l'IDE la création et les changements survenus dans ses caractéristiques civiles.
	 * </p>
	 * <p>
	 *     Dans l'état actuel des choses, seul l'établissement principal est annoncé. Les établissements secondaires ne sont pas
	 *     pris en charge).
	 * </p>
	 * @param entreprise l'entreprise ciblé
	 * @return l'annonce IDE envoyée, le modèle d'annonce en cas de validation seule, ou null si aucune annonce n'a besoin d'etre émise.
	 * @throws ServiceIDEException en cas d'erreur non récupérable.
	 * @throws AnnonceIDEValidationException en cas d'echec de la validation effectuée avant tout envoi. L'exception contient la liste des erreurs rencontrées.
	 */
	BaseAnnonceIDE synchroniseIDE(Entreprise entreprise) throws ServiceIDEException;

	/**
	 * <p>
	 *     Vérifier s'il faut envoyer une annonce à l'IDE pour une entreprise, mais seulement à fin de validation, sans expédier d'annonce.
	 * </p>
	 * @param entreprise l'entreprise ciblé
	 * @return le modèle de l'annonce IDE qui serait envoyée, ou null si aucune annonce n'a besoin d'etre émise.
	 * @throws ServiceIDEException en cas d'erreur non récupérable.
	 * @throws AnnonceIDEValidationException en cas d'echec de la validation effectuée avant tout envoi. L'exception contient la liste des erreurs rencontrées.
	 */
	BaseAnnonceIDE simuleSynchronisationIDE(Entreprise entreprise) throws ServiceIDEException;

	/**
	 * Valider le modèle d'annonce. Le service civil organisation est utilisé pour cela.
	 *
	 * @param modele le modèle à faire valider.
	 * @throws ServiceIDEException en cas d'erreur non récupérable, notamment lors de l'accès au service de validation du régistre civil.
	 * @throws AnnonceIDEValidationException signale l'échec de la validation. Voir la liste des erreurs jointes à l'exception.
	 */
	void validerAnnonceIDE(BaseAnnonceIDE modele) throws ServiceIDEException;


}
