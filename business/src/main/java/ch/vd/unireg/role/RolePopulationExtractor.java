package ch.vd.unireg.role;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;

/**
 * Interface de définition d'un extracteur de population pour les rôles
 * @param <T> type de contribuable pour la composition des rôles
 */
public interface RolePopulationExtractor<T extends Contribuable> {

	@Nullable
	Integer getCommunePourRoles(int annee, T contribuable);

	@Nullable ForFiscalRevenuFortune getForPourRoles(int annee, T contribuable);
}
