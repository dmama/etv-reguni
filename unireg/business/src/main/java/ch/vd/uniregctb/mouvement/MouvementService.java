package ch.vd.uniregctb.mouvement;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;

public interface MouvementService {

	/**
	 * Détermine les mouvements de dossiers pour une année (2010 pour les mouvements début 2010)
	 */
	DeterminerMouvementsDossiersEnMasseResults traiteDeterminationMouvements(RegDate dateTraitement, StatusManager statusManager);

	/**
	 * Fait la demande d'impression d'un bordereau de mouvement de dossiers en masse avec les mouvements indiqués
	 * @param mvts les mouvements constituant le bordereau
	 * @return l'identifiant à utiliser (dans une autre transaction, afin que le message soit bien envoyé) dans un appel à {@link #recevoirImpressionBordereau(String)}
	 */
	EditiqueResultat envoyerImpressionBordereau(List<MouvementDossier> mvts) throws EditiqueException;
}
