package ch.vd.unireg.type;

import ch.vd.unireg.common.ObjectNotFoundException;

/**
 * Exception spécialisée pour signaler proprement les cas où l'information du permis d el'individu n'a pu être interpretée.
 *
 * @author Baba NGOM <baba-issa.ngom@vd.ch>
 */
public class TypePermisInvalideException extends ObjectNotFoundException {

	private static final long serialVersionUID = -736783238010398724L;

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible d'interpréter le permis de l'individu rattaché à un habitant.
	 */
	public TypePermisInvalideException(String codePermis) {
		super(buildMessage(codePermis));
	}

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible d'interpréter le permis de l'individu à partir de son numéro.
	 */
	public TypePermisInvalideException(long noIndividu, String detail) {
		super(buildMessage(noIndividu,detail));
	}

	private static String buildMessage(long noInvidu, String detail) {
		return String.format("Impossible d'interpréter le type de  permis pour l'individu n°%d, détails: %s", noInvidu,detail);
	}

	private static String buildMessage(String code) {
		return String.format("Erreur détectée: °%s", code);
	}
}
