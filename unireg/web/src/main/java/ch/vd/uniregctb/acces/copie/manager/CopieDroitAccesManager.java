package ch.vd.uniregctb.acces.copie.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.acces.copie.view.ConfirmCopieView;
import ch.vd.uniregctb.acces.copie.view.ConfirmedDataView;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.security.DroitAccesException;

public interface CopieDroitAccesManager {

	/**
	 * Alimente le frombacking du controller
	 * @param noOperateurReference
	 * @param noOperateurDestination
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	ConfirmCopieView get(long noOperateurReference, long noOperateurDestination) throws AdresseException;

	/**
	 * Copie les droits d'un utilisateur vers un autre
	 */
	@Transactional(rollbackFor = Throwable.class)
	void copie(ConfirmedDataView view) throws DroitAccesException ;

	/**
	 * Transfert les droits d'un utilisateur vers un autre
	 */
	@Transactional(rollbackFor = Throwable.class)
	void transfert(ConfirmedDataView view) throws DroitAccesException ;

}
