package ch.vd.uniregctb.acces.copie.manager;

import ch.vd.uniregctb.adresse.AdresseException;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.acces.copie.view.ConfirmCopieView;
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
	public ConfirmCopieView get(long noOperateurReference, long noOperateurDestination) throws AdresseException;

	/**
	 * Copie les droits d'un utilisateur vers un autre
	 *
	 * @param confirmCopieView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void copie(ConfirmCopieView confirmCopieView) throws DroitAccesException ;

	/**
	 * Transfert les droits d'un utilisateur vers un autre
	 *
	 * @param confirmCopieView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void transfert(ConfirmCopieView confirmCopieView) throws DroitAccesException ;

}
