package ch.vd.uniregctb.mouvement;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;

public interface MouvementService {

	/**
	 * Détermine les mouvements de dossiers pour une année (2010 pour les mouvements début 2010)
	 * @param dateTraitement date de traitement (= date d'exécution du job)
	 * @param archivesSeulement <code>true</code> si seuls les mouvements vers les archives doivent être générés, <code>false</code> s'il faut également générer les mouvements inter-offices
	 * @param statusManager status manager à utiliser lors de l'avancement du job
	 */
	DeterminerMouvementsDossiersEnMasseResults traiteDeterminationMouvements(RegDate dateTraitement, boolean archivesSeulement, StatusManager statusManager);

	/**
	 * Fait la demande d'impression d'un bordereau de mouvement de dossiers en masse avec les mouvements indiqués
	 * @param mvts les mouvements constituant le bordereau
	 * @return l'identifiant à utiliser (dans une autre transaction, afin que le message soit bien envoyé) dans un appel à {@link #recevoirImpressionBordereau(String)}
	 */
	EditiqueResultat envoyerImpressionBordereau(List<MouvementDossier> mvts) throws EditiqueException;
}
