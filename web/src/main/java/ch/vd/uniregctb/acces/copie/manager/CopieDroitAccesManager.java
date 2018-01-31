package ch.vd.uniregctb.acces.copie.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.acces.copie.view.ConfirmCopieView;
import ch.vd.uniregctb.acces.copie.view.ConfirmedDataView;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.security.DroitAccesConflitAvecDonneesContribuable;

public interface CopieDroitAccesManager {

	/**
	 * Alimente le frombacking du controller
	 * @param noOperateurReference
	 * @param noOperateurDestination
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	ConfirmCopieView get(long noOperateurReference, long noOperateurDestination, ParamPagination pagination) throws AdresseException;

	/**
	 * Copie les droits d'un utilisateur vers un autre
	 * @return les éventuels conflits rencontrés lors de la copie
	 */
	@Transactional(rollbackFor = Throwable.class)
	List<DroitAccesConflitAvecDonneesContribuable> copie(ConfirmedDataView view) throws AdresseException;

	/**
	 * Transfert les droits d'un utilisateur vers un autre
	 * @return les éventuels conflits rencontrés lors du transfert
	 */
	@Transactional(rollbackFor = Throwable.class)
	List<DroitAccesConflitAvecDonneesContribuable> transfert(ConfirmedDataView view) throws AdresseException;

}
