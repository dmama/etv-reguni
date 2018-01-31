package ch.vd.uniregctb.role.before2016;

import java.util.Collection;

/**
 * Données des contribuables pour les rôles d'une commune
 * @param <CTB> type de contribuable
 * @param <COM> classe dérivée des informations des communes
 */
public abstract class InfoCommune<CTB extends InfoContribuable<CTB>, COM extends InfoCommune<CTB, COM>> {

	private final int noOfs;

	public InfoCommune(int noOfs) {
		this.noOfs = noOfs;
	}

	public int getNoOfs() {
		return noOfs;
	}

	public abstract Collection<CTB> getInfosContribuables();

	public abstract void addAll(COM value);
}
