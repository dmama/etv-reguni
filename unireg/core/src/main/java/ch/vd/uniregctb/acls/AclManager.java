package ch.vd.uniregctb.acls;

import org.acegisecurity.acls.AccessControlEntry;
import org.acegisecurity.acls.MutableAcl;
import org.acegisecurity.acls.MutableAclService;
import org.acegisecurity.acls.NotFoundException;
import org.acegisecurity.acls.Permission;
import org.acegisecurity.acls.objectidentity.ObjectIdentityImpl;
import org.acegisecurity.acls.sid.PrincipalSid;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.tiers.Tiers;

/**
 * Gestionnaire des ACL.
 *
 * @author Ludovic Bertin
 *
 */
public class AclManager {

	/**
	 * Un logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(AclManager.class);

	/**
	 * L'objet MutableAclService utilisé pour géré les ACLs.
	 */
	private MutableAclService mutableAclService;

	/**
	 * Ajoute la permission donnée sur le tiers pour l'utilisateur donné.
	 *
	 * @param tiers
	 *            le tiers concerné
	 * @param userName
	 *            l'utilisateur bénéficiant de la permission
	 * @param permission
	 *            la permission a ajouter
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void addPermission(Tiers tiers, String userName, Permission permission, boolean granted) {
		// Construction de l'OID
		org.acegisecurity.acls.objectidentity.ObjectIdentity oid = new ObjectIdentityImpl(Tiers.class, tiers.getId());

		// On recherche un ACL déjà existant : le cas échéant, on en crée un.
		MutableAcl acl;
		try {
			acl = (MutableAcl) mutableAclService.readAclById(oid);
		} catch (NotFoundException nfe) {
			acl = mutableAclService.createAcl(oid);
		}

		// Construction du SID
		PrincipalSid sid = new PrincipalSid(userName);

		// insertion de l'ACL Entry
		acl.insertAce(null, permission, sid, granted);
		mutableAclService.updateAcl(acl);

		// Un ptit log, un !
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Permission ajoutée :" + permission + " pour Sid " + userName + "( tiers : " + tiers.getId() + " )");
		}
	}

	/**
	 * Supprime la permission donnée sur le tiers pour l'utilisateur donné.
	 *
	 * @param tiers
	 *            le tiers concerné
	 * @param userName
	 *            l'utilisateur bénéficiant de la permission
	 * @param permission
	 *            la permission a ajouter
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void deletePermission(Tiers tiers, String userName, Permission permission) {
		// Construction de l'OID
		org.acegisecurity.acls.objectidentity.ObjectIdentity oid = new ObjectIdentityImpl(tiers.getClass(), tiers.getId());

		// Construction du SID
		PrincipalSid sid = new PrincipalSid(userName);

		// On recherche l' ACL
		MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);

		// On parcourt les ACL entries pour supprimer la bonne
		AccessControlEntry entries[] = acl.getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getSid().equals(sid) && entries[i].getPermission().equals(permission)) {
				acl.deleteAce(entries[i].getId());
			}
		}

		// On met a jour en base de données
		mutableAclService.updateAcl(acl);

		// Un ptit log, un !
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Permission supprimée :" + permission + " pour Sid " + userName + "( tiers : " + tiers.getId() + " )");
		}
	}

	/**
	 * Supprime toutes les permissions donnée sur le tiers.
	 *
	 * @param tiers
	 *            le tiers concerné
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void deleteAcl(Tiers tiers) {
		// Construction de l'OID
		org.acegisecurity.acls.objectidentity.ObjectIdentity oid = new ObjectIdentityImpl(tiers.getClass(), tiers.getId());

		// Suppression de l'ACL
		mutableAclService.deleteAcl(oid, true);

		// Un ptit log, un !
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("ACL supprimée ( tiers : " + tiers.getId() + " )");
		}
	}

	/**
	 * @param mutableAclService
	 *            the mutableAclService to set
	 */
	public void setMutableAclService(MutableAclService mutableAclService) {
		this.mutableAclService = mutableAclService;
	}

}
