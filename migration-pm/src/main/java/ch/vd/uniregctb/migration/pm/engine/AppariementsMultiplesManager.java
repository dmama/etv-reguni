package ch.vd.uniregctb.migration.pm.engine;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Interface du bean qui permet au job de migration de savoir si un numéro cantonal a été
 * utilisé pour plusieurs entreprises distinctes (et, le cas échéant, lesquelles)
 */
public interface AppariementsMultiplesManager {

	/**
	 * @param noCantonal un numéro cantonal d'organisation
	 * @return la liste (vide si le numéro est inconnu, ou s'il n'est utilisé que pour une seule entreprise...) des numéros d'entreprises de RegPM associées à ce numéro cantonal
	 */
	@NotNull
	Set<Long> getIdentifiantsEntreprisesAvecMemeAppariement(long noCantonal);
}
