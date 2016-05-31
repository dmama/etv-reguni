package ch.vd.uniregctb.common;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

/**
 * Interface implémentable par les entités qui possèdent une notion d'âge
 */
public interface Dated {

	/**
	 * @param unit unité dans laquelle l'âge doit être exprimé
	 * @return âge de l'entité, dans l'unité demandée
	 */
	long getAge(@NotNull TimeUnit unit);
}
