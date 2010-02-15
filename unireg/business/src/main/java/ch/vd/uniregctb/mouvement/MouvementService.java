package ch.vd.uniregctb.mouvement;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.editique.EditiqueException;

public interface MouvementService {

	/**
	 * Détermine les mouvements de dossiers pour une année (2010 pour les mouvements début 2010)
	 */
	DeterminerMouvementsDossiersEnMasseResults traiteDeterminationMouvements(RegDate dateTraitement, StatusManager statusManager);

	/**
	 * Imprime un bordereau de mouvement de dossiers en masse avec les mouvements indiqués
	 * @param mvts les mouvements constituant le bordereau
	 * @return le document imprimé (PCL)
	 */
	byte[] creerEtImprimerBordereau(List<MouvementDossier> mvts) throws EditiqueException;
}
