package ch.vd.uniregctb.registrefoncier;

import java.util.function.Function;

/**
 * Function qui permet de récupérer l'URL vers Capitastra à partir de l'id d'un immeuble.
 */
public interface CapitastraURLProvider extends Function<Long, String> {
}
