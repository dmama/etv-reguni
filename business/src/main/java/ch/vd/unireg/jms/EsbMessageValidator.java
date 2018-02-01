package ch.vd.unireg.jms;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.util.exception.ESBValidationException;

/**
 * Interface d'accès à la validation d'un message (<i>a priori</i> sortant...)
 */
public interface EsbMessageValidator {

	/**
	 * Validation du message
	 * @param msg message à valider
	 * @throws ESBValidationException en cas de message invalide
	 */
	void validate(EsbMessage msg) throws ESBValidationException;
}
