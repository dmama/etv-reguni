package ch.vd.unireg.acces.copie.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.acces.copie.view.ConfirmCopieView;
import ch.vd.unireg.acces.copie.view.ConfirmedDataView;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.security.DroitAccesConflitAvecDonneesContribuable;

public interface CopieDroitAccesManager {

	/**
	 * Alimente le frombacking du controller
	 */
	@Transactional(readOnly = true)
	ConfirmCopieView get(String visaOperateurReference, String visaOperateurDestination, ParamPagination pagination) throws AdresseException;

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
