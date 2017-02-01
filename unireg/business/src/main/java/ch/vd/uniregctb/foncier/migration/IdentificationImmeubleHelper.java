package ch.vd.uniregctb.foncier.migration;

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
	 * Retrouve l'immeuble de la commune données
	 * @param dao
	 * @param commune
	 * @param parcelle
	 * @return
	 */
	@NotNull
	public static ImmeubleRF findImmeuble(ImmeubleRFDAO dao, Commune commune, MigrationParcelle parcelle) {
		final ImmeubleRF immeuble = dao.findImmeubleActif(commune.getNoOFS(), parcelle.getNoParcelle(), parcelle.getIndex1(), parcelle.getIndex2(), parcelle.getIndex3());
		if (immeuble == null) {

			if (parcelle.hasIndexes()) {
				// [SIFISC-23111] rien trouvé... cherchons sans les indexes... s'il n'y en a qu'un, alors c'est dans la poche
				final MigrationParcelle parcelleSansIndexes = parcelle.getSansIndexes();
				try {
					final ImmeubleRF immeubleSansIndexes = dao.findImmeubleActif(commune.getNoOFS(), parcelleSansIndexes.getNoParcelle(), parcelleSansIndexes.getIndex1(), parcelleSansIndexes.getIndex2(), parcelleSansIndexes.getIndex3());
					if (immeubleSansIndexes != null) {
						return immeubleSansIndexes;
					}
				}
				catch (NonUniqueResultException e) {
					throw new IllegalArgumentException("L'immeuble avec la parcelle [" + parcelle + "] n'existe pas sur la commune de " + commune.getNomOfficiel() + " (" + commune.getNoOFS() + ") mais il y a plusieurs de ces immeubles sans index sur la parcelle.");
				}
			}

			throw new IllegalArgumentException("L'immeuble avec la parcelle [" + parcelle + "] n'existe pas sur la commune de " + commune.getNomOfficiel() + " (" + commune.getNoOFS() + ").");
		}
		return immeuble;
	}

}
