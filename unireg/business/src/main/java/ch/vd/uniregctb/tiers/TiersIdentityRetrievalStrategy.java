package ch.vd.uniregctb.tiers;

import org.acegisecurity.acls.objectidentity.ObjectIdentity;
import org.acegisecurity.acls.objectidentity.ObjectIdentityImpl;
import org.acegisecurity.acls.objectidentity.ObjectIdentityRetrievalStrategy;

import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

/**
 * Classe permettant de construire un "object identity" (OID) à partir d'un objet.
 * Dans le cas d'un TiersDisplayValues, ca return l'oid correspondant au tiers.
 * 
 * @author Ludovic Bertin
 *
 */
public class TiersIdentityRetrievalStrategy implements ObjectIdentityRetrievalStrategy {

	/**
	 * Renvoie l'OID correspondant à l'objet donnée en paramètre.
	 * @param	domainObject	l'objet dont on veut l'OID
	 * @return 	l'OID du tiers si l'objet est de type TiersDisplayValues, l'OID de l'objet sinon
	 */
	public ObjectIdentity getObjectIdentity(Object domainObject) {
		
		if (domainObject instanceof TiersIndexedData) {
			return new ObjectIdentityImpl( Tiers.class.getName(), ( (TiersIndexedData)domainObject ).getNumero() );
		}
	
		else {
			return new ObjectIdentityImpl( domainObject );
		}
	}

}
