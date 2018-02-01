package ch.vd.unireg.evenement.reqdes.engine;

/**
 * Exception checkée qui permet d'arrêter le traitement d'une unité de traitement
 * d'événement ReqDes
 */
public class EvenementReqDesException extends Exception {

	public EvenementReqDesException(String message) {
		super(message);
	}

	public EvenementReqDesException(String message, Throwable cause) {
		super(message, cause);
	}

	public EvenementReqDesException(Throwable cause) {
		super(cause);
	}
}
