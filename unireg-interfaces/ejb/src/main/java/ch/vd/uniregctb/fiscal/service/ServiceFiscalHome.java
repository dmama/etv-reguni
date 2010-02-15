package ch.vd.uniregctb.fiscal.service;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * Interface home distante du service EJB Unireg-Interface.
 *
 * @author Baba NGOM xsibnm
 * @version $Revision: 1.0 $
 */
public interface ServiceFiscalHome extends EJBHome {

	/**
	 * Créé et retourne une référence distante du service.
	 *
	 * @return une référence distante du service.
	 * @throws RemoteException
	 *             si un problème technique survient durant la création du
	 *             service.
	 * @throws CreateException
	 *             si le service distant ne peut être créé.
	 *
	 *
	 */

	 /** Nom JNDI du service EJB. */
    static final String JNDI_NAME = "ejb/" + ServiceFiscal.class.getName();

	ServiceFiscal create() throws RemoteException, CreateException;

}
