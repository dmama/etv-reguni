package ch.vd.unireg.common.linkedentity;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.hibernate.HibernateTemplate;

/**
 * Les informations du context dans lequel on demande la liste des entités liées.
 */
public class LinkedEntityContext {

	@NotNull
	private final LinkedEntityPhase phase;

	@NotNull
	private final HibernateTemplate hibernateTemplate;

	public LinkedEntityContext(@NotNull LinkedEntityPhase phase, @NotNull HibernateTemplate hibernateTemplate) {
		this.phase = phase;
		this.hibernateTemplate = hibernateTemplate;
	}

	@NotNull
	public LinkedEntityPhase getPhase() {
		return phase;
	}

	@NotNull
	public HibernateTemplate getHibernateTemplate() {
		return hibernateTemplate;
	}
}
