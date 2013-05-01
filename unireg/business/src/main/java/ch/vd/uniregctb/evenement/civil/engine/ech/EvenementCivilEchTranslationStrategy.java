package ch.vd.uniregctb.evenement.civil.engine.ech;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

public interface EvenementCivilEchTranslationStrategy {

	/**
	 * Crée un événement civil interne à partir d'un événement civil reçu de RCPers.
	 *
	 * @param event   un événement civil reçu de RCPers
	 * @param context le context d'exécution de l'événement civil
	 * @param options des options de traitement
	 * @return un événement civil interne qui corresponds à l'événement civil externe reçu
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException en cas de problème
	 */
	EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException;

	/**
	 *
	 * @param event un événement civil reçu de RCPers
	 * @param context le contexte d'exécution de l'événement civil
	 * @return <code>true</code> si l'activité principale de la strategy correspond à une ré-indexation (ou nettoyage de cache, ou mise à jour de données sur des anciens habitants), <code>false</code> si un réel impact fiscal est à prévoir
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException en cas de problème
	 */
	boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException;
}
