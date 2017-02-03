package ch.vd.uniregctb.foncier.migration;

import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.ObjectNotFoundException;

/**
 * Exception lancée quand l'immeuble n'est pas trouvé
 */
public class ImmeubleNotFoundException extends ObjectNotFoundException {

	public ImmeubleNotFoundException(Commune commune, MigrationParcelle parcelle) {
		super(buildMessage(commune, parcelle));
	}

	private static String buildMessage(Commune commune, MigrationParcelle parcelle) {
		return String.format("L'immeuble avec la parcelle [%s] n'existe pas sur la commune de %s (%d).", parcelle, commune.getNomOfficiel(), commune.getNoOFS());
	}
}
