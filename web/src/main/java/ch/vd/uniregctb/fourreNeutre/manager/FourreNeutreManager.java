package ch.vd.uniregctb.fourreNeutre.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.fourreNeutre.FourreNeutreException;

public interface FourreNeutreManager {
	/**
	 * Retourne la liste des périodes fiscales autorisées pour l'impression d'une fourre neutre selon le contribuable
	 * @param tiersId le tiers pour qui les périodes doivent être retournées
	 * @return la liste des périodes fiscales autorisées
	 */
	List<Integer> getPeriodesAutoriseesPourImpression(long tiersId);

	/**
	 *Imprime une fourre neutre pour un ctb, envoie un evenement fiscal  et logg l'opération si besoin
	 *@param ctbId identifiant du tiers concerné par l'impressions de la fn
	 *@param pf période fiscale choisi pour l'impression de  la fn
	 */

	@Transactional(rollbackFor = Throwable.class)
	EditiqueResultat envoieImpressionLocaleFourreNeutre(Long ctbId, int pf) throws FourreNeutreException;

	/**
	 * Teste l'autorisation d'imprimer une fourre neutre pour un tiers
	 * @param tiersId identifiant du tiers à tester
	 * @return vrai si ok pour impression false sinon
	 */
	boolean isAutorisePourFourreNeutre(long tiersId);
}
