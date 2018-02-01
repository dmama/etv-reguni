package ch.vd.unireg.separation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.type.EtatCivil;


public interface SeparationRecapManager {

	/**
	 * Crée une séparation sur le ménage commun indiqué, à la date donnée...
	 */
	@Transactional(rollbackFor = Throwable.class)
	void separeCouple(long idMenage, RegDate dateSeparation, EtatCivil etatCivil, String commentaire) throws MetierServiceException;

	/**
	 * @param noTiers le numéro du tiers dont on veut connaître l'activité au niveau des fors principaux
	 * @return <code>true</code> si le tiers possède un for principal actif (= non-fermé), <code>false</code> sinon
	 */
	@Transactional(readOnly = true)
	boolean isAvecForFiscalPrincipalActif(long noTiers);

}
