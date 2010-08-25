package ch.vd.uniregctb.validation;

import java.util.List;

import ch.vd.registre.base.validation.Validateable;
import ch.vd.uniregctb.common.EntityKey;

/**
 * Interface qui définit un objet validable et qui pointe vers zéro ou plusieurs autres entités devant être validées.
 */
public interface JoinValidateable extends Validateable {

	/**
	 * @return une collection contenant les clés de zéro ou plusieurs entités devant être validées.
	 */
	List<EntityKey> getJoinedEntities();
}
