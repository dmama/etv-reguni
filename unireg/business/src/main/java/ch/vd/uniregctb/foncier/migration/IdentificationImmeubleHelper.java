package ch.vd.uniregctb.foncier.migration;

import org.hibernate.FlushMode;
import org.hibernate.NonUniqueResultException;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;

/**
 * Classe utilitaire pour l'identification des immeubles à partir de la commune et du numéro de parcelle
 */
public abstract class IdentificationImmeubleHelper {

	/**
	 * Retrouve l'immeuble de la commune données avec l'identification de parcelle donnée.
	 * @param dao DAO des immeubles RF
	 * @param commune commune sur laquelle réside l'immeuble recherché
	 * @param parcelle identification de parcelle de l'immeuble
	 * @return l'immeuble trouvé
	 * @throws IllegalArgumentException si aucun immeuble ne correspond à la recherche
	 * @throws NonUniqueResultException si plusieurs immeubles correspondent à la recherche
	 */
	@NotNull
	public static ImmeubleRF findImmeuble(ImmeubleRFDAO dao, Commune commune, MigrationParcelle parcelle) {
		final ImmeubleRF immeuble = dao.findImmeubleActif(commune.getNoOFS(), parcelle.getNoParcelle(), parcelle.getIndex1(), parcelle.getIndex2(), parcelle.getIndex3(), FlushMode.MANUAL);
		if (immeuble == null) {
			throw new IllegalArgumentException("L'immeuble avec la parcelle [" + parcelle + "] n'existe pas sur la commune de " + commune.getNomOfficiel() + " (" + commune.getNoOFS() + ").");
		}
		return immeuble;
	}

}
