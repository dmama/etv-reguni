package ch.vd.unireg.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.tiers.view.AdresseView;
import ch.vd.unireg.tiers.view.TiersEditView;

/**
 * Service offrant les methodes permettant de gerer le controller TiersAdresseController
 *
 * @author xcifde
 *
 */
public interface AdresseManager {

	/**
	 * Alimente la vue AdresseView pour une adresse existante
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	AdresseView getAdresseView(Long id);

	AdresseView getAdresseView(TiersEditView tiers,Long numero);

	/**
	 * Cree une nouvelle vue AdresseView pour une nouvelle adresse
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	AdresseView create(Long numeroCtb);

	/**
	 * Sauvegarde de l'adresse en base de donnees
	 *
	 * @param adresseView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(AdresseView adresseView) throws AccessDeniedException;

	/**
	 * Annule une adresse
	 *
	 * @param idAdresse
	 */
	@Transactional(rollbackFor = Throwable.class)
	void annulerAdresse(Long idAdresse);

	/**
	 * Ferme une adresse à la date demandée
	 */
	@Transactional(rollbackFor = Throwable.class)
	void fermerAdresse(Long idAdresse, RegDate dateFin);

	/**
	 * Sauvegarde d'une reprise d'adresse
	 *
	 * @param adresseView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void saveReprise(AdresseView adresseView);

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	TiersEditView getView(Long numero) throws InfrastructureException, AdresseException;

}
