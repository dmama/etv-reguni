package ch.vd.uniregctb.acls;

import org.acegisecurity.Authentication;
import org.acegisecurity.acls.Acl;
import org.acegisecurity.acls.AclService;
import org.acegisecurity.acls.NotFoundException;
import org.acegisecurity.acls.Permission;
import org.acegisecurity.acls.domain.BasePermission;
import org.acegisecurity.acls.objectidentity.ObjectIdentity;
import org.acegisecurity.acls.objectidentity.ObjectIdentityRetrievalStrategy;
import org.acegisecurity.acls.objectidentity.ObjectIdentityRetrievalStrategyImpl;
import org.acegisecurity.acls.sid.Sid;
import org.acegisecurity.acls.sid.SidRetrievalStrategy;
import org.acegisecurity.acls.sid.SidRetrievalStrategyImpl;
import org.acegisecurity.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider;

/**
 * Filtre la collection en enlevant les objets pour lesquels une des permissions a été bannie. 
 *
 * @author Ludovic Bertin
 */
public class AclEntryAfterInvocationCollectionFilteringVetoProvider extends AclEntryAfterInvocationCollectionFilteringProvider {


    /**
     * Le service permettant de récupérer les ACLs.
     */
    private AclService aclService;

    /**
     * Les permissions dont on veut tester les vétos.
     */
    private Permission[] requirePermission = {BasePermission.READ};
    
	/**
	 * Le ObjectIdentityRetrievalStrategy permettant de construire un OID a partir d'un objet.
	 */
    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();

	/**
	 * Le SidRetrievalStrategy permettant de construire un SID a partir d'un objet Principal.
	 */
    private SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();
    
	/**
	 * Constructeur.
	 * @param aclService
	 * @param requirePermission
	 */
	public AclEntryAfterInvocationCollectionFilteringVetoProvider(AclService aclService, Permission[] requirePermission) {
		super(aclService, requirePermission);
		this.aclService = aclService;
		this.requirePermission = requirePermission;
	}

	/* (non-Javadoc)
	 * @see org.acegisecurity.afterinvocation.AbstractAclProvider#hasPermission(org.acegisecurity.Authentication, java.lang.Object)
	 */
	@Override
    protected boolean hasPermission(Authentication authentication, Object domainObject) {
        // Recupere l'OID
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity(domainObject);

        // Récupère les SIDs aplpicable au Principal
        Sid[] sids = sidRetrievalStrategy.getSids(authentication);

        Acl acl = null;

        try {
            // Recherche des ACLs pour les SIDS concernés
            acl = aclService.readAclById(objectIdentity, sids);

            return acl.isGranted(requirePermission, sids, false);
        } catch (NotFoundException ignore) {
        	// Dans le cas ou il n'y a pas d'ACL Entry, pas de veto => hasPermission renvoie true
            return true;
        }
    }

	/* (non-Javadoc)
	 * @see org.acegisecurity.afterinvocation.AbstractAclProvider#setObjectIdentityRetrievalStrategy(org.acegisecurity.acls.objectidentity.ObjectIdentityRetrievalStrategy)
	 */
	@Override
	public void setObjectIdentityRetrievalStrategy(ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy) {
		super.setObjectIdentityRetrievalStrategy(objectIdentityRetrievalStrategy);
		this.objectIdentityRetrievalStrategy = objectIdentityRetrievalStrategy;
	}

	/* (non-Javadoc)
	 * @see org.acegisecurity.afterinvocation.AbstractAclProvider#setSidRetrievalStrategy(org.acegisecurity.acls.sid.SidRetrievalStrategy)
	 */
	@Override
	public void setSidRetrievalStrategy(SidRetrievalStrategy sidRetrievalStrategy) {
		super.setSidRetrievalStrategy(sidRetrievalStrategy);
		this.sidRetrievalStrategy = sidRetrievalStrategy;
	}

	
}
