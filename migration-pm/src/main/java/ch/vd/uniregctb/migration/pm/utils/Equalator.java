package ch.vd.uniregctb.migration.pm.utils;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Equalator<T> {

	/**
	 * @param data1 donnée 1
	 * @param data2 donnée 2
	 * @return <code>true</code> si les deux entités doivent être considérées comme égales
	 */
	boolean areEquals(@Nullable T data1, @Nullable T data2);
}
