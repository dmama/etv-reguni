package ch.vd.unireg.annulation.couple.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.MetierServiceException;

public interface AnnulationCoupleRecapManager {

	/**
	 * @param idMenage identifiant d'un ménage commun
	 * @return la date de début de la dernière période d'activité du ménage
	 */
	@Transactional(readOnly = true)
	RegDate getDateDebutDernierMenage(long idMenage);


	/**
	 * @throws MetierServiceException
	 */
	@Transactional(rollbackFor = Throwable.class)
	void annuleCouple(long idMenage, RegDate dateReference) throws MetierServiceException;

	/**
	 *
	 */
	@Transactional(readOnly = true)
	boolean isMenageCommunAvecPrincipal(long noCtb, RegDate date);

}
