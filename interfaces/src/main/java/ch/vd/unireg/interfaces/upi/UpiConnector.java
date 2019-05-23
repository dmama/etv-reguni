package ch.vd.unireg.interfaces.upi;

import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;

public interface UpiConnector {

	String SERVICE_NAME = "UpiConnector";

	/**
	 * Appelle le service UPI avec le numéro AVS13 donné et récupère les informations liées à cet individu
	 * @param noAvs13 numéro AVS13
	 * @return les informations collectées, ou <code>null</code> si aucune n'est collectable (par exemple AVS13 assigné par le passé à deux personnes distinctes)
	 * @throws UpiConnectorException en cas d'erreur du service
	 */
	UpiPersonInfo getPersonInfo(String noAvs13) throws UpiConnectorException;

}
