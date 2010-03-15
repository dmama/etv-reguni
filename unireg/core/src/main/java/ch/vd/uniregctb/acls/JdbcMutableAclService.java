package ch.vd.uniregctb.acls;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.acegisecurity.Authentication;
import org.acegisecurity.acls.AccessControlEntry;
import org.acegisecurity.acls.Acl;
import org.acegisecurity.acls.AlreadyExistsException;
import org.acegisecurity.acls.ChildrenExistException;
import org.acegisecurity.acls.MutableAcl;
import org.acegisecurity.acls.MutableAclService;
import org.acegisecurity.acls.NotFoundException;
import org.acegisecurity.acls.domain.AccessControlEntryImpl;
import org.acegisecurity.acls.jdbc.AclCache;
import org.acegisecurity.acls.jdbc.JdbcAclService;
import org.acegisecurity.acls.jdbc.LookupStrategy;
import org.acegisecurity.acls.objectidentity.ObjectIdentity;
import org.acegisecurity.acls.objectidentity.ObjectIdentityImpl;
import org.acegisecurity.acls.sid.GrantedAuthoritySid;
import org.acegisecurity.acls.sid.PrincipalSid;
import org.acegisecurity.acls.sid.Sid;
import org.acegisecurity.context.SecurityContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Classe récupérée du framework Acegi security :
 * <ul>
 * <li>Correction de la requete SQL IDENTITY_QUERY qui permet de récupérer l'id d'un nouvel élément.</li>
 * <li>Modification des requêtes d'insert pour insérer aussi cet id au lieu de laisser le soin à la base de données de le créer.</li>
 * </ul>
 *
 * @author Ben Alex
 * @author Ludovic Bertin
 */
public class JdbcMutableAclService extends JdbcAclService implements MutableAclService {
    //~ Instance fields ================================================================================================

	/**
	 * The cache.
	 */
    private final AclCache aclCache;

	/**
	 * SQL Query : delete all classes with given class name.
	 */
    private final static String DELETE_CLASS_BY_CLASSNAME_STRING = "DELETE FROM acl_class WHERE class=?";

	/**
	 * SQL Query : delete all ACL entries with given object identity.
	 */
    private final static String DELETE_ENTRY_BY_OBJECT_IDENTITY_FK = "DELETE FROM acl_entry WHERE acl_object_identity=?";

	/**
	 *  SQL Query : delete all ACL object identities with given id.
	 */
    private final static String DELETE_OBJECT_IDENTITY_BY_PRIMARY_KEY = "DELETE FROM acl_object_identity WHERE id=?";

	/**
	 * SQL Query : get next id.
	 */
    private final static String IDENTITY_QUERY = "select acl_seq.nextval from dual";

	/**
	 *  SQL Query : new class insertion.
	 */
    private final static String INSERT_CLASS = "INSERT INTO acl_class (id, class) VALUES (?, ?)";

	/**
	 *  SQL Query : new ACL Entry insertion.
	 */
    private final static String INSERT_ENTRY = "INSERT INTO acl_entry "
        + "(id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)"
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 *  SQL Query : new ACL object identity insertion.
	 */
    private final static String INSERT_OBJECT_IDENTITY = "INSERT INTO acl_object_identity "
        + "(id, object_id_class, object_id_identity, owner_sid, entries_inheriting) " + "VALUES (?, ?, ?, ?, ?)";

	/**
	 *  SQL Query : new SID insertion.
	 */
    private final static String INSERT_SID = "INSERT INTO acl_sid (id, principal, sid) VALUES (?, ?, ?)";

	/**
	 *  SQL Query : get class id from class name.
	 */
    private final static String SELECT_CLASS_PRIMARY_KEY = "SELECT id FROM acl_class WHERE class=?";

	/**
	 *  SQL Query : get number of ACL object identities rows for a given class name.
	 */
    private final static String SELECT_COUNT_OBJECT_IDENTITY_ROWS_FOR_PARTICULAR_CLASSNAME_STRING = "SELECT COUNT(acl_object_identity.id) "
        + "FROM acl_object_identity, acl_class WHERE acl_class.id = acl_object_identity.object_id_class and class=?";

	/**
	 *  SQL Query : get ACL object identity for a given class name and object id.
	 */
    private final static String SELECT_OBJECT_IDENTITY_PRIMARY_KEY = "SELECT acl_object_identity.id FROM acl_object_identity, acl_class "
        + "WHERE acl_object_identity.object_id_class = acl_class.id and acl_class.class=? "
        + "and acl_object_identity.object_id_identity = ?";

	/**
	 *  SQL Query : get SID id for a given pricipal and SID.
	 */
    private final static String SELECT_SID_PRIMARY_KEY = "SELECT id FROM acl_sid WHERE principal=? AND sid=?";

	/**
	 *  SQL Query : update SID.
	 */
    private final static String UPDATE_OBJECT_IDENTITY = "UPDATE acl_object_identity SET "
        + "owner_sid = ?, entries_inheriting = ?" + "where id = ?";

    private final static String UPDATE_OBJECT_IDENTITY_WITH_PARENT = "UPDATE acl_object_identity SET "
        + "parent_object = ?, owner_sid = ?, entries_inheriting = ?" + "where id = ?";

    //~ Constructors ===================================================================================================

    /**
     * Constructor.
     */
    public JdbcMutableAclService(DataSource dataSource, LookupStrategy lookupStrategy, AclCache aclCache) {
        super(dataSource, lookupStrategy);
        Assert.notNull(aclCache, "AclCache required");
        this.aclCache = aclCache;
    }

    //~ Methods ========================================================================================================

    /**
     * Create a new Acl for given object identity
     */
    public MutableAcl createAcl(ObjectIdentity objectIdentity)
        throws AlreadyExistsException {
        Assert.notNull(objectIdentity, "Object Identity required");

        // Check this object identity hasn't already been persisted
        if (retrieveObjectIdentityPrimaryKey(objectIdentity) != null) {
            throw new AlreadyExistsException("Object identity '" + objectIdentity + "' already exists");
        }

        // Need to retrieve the current principal, in order to know who "owns" this ACL (can be changed later on)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        PrincipalSid sid = new PrincipalSid(auth);

        // Create the acl_object_identity row
        createObjectIdentity(objectIdentity, sid);

        // Retrieve the ACL via superclass (ensures cache registration, proper retrieval etc)
        Acl acl = readAclById(objectIdentity);
        Assert.isInstanceOf(MutableAcl.class, acl, "MutableAcl should be been returned");

        return (MutableAcl) acl;
    }

    /**
     * Creates a new row in acl_entry for every ACE defined in the passed MutableAcl object.
     *
     * @param acl containing the ACEs to insert
     */
    protected void createEntries(final MutableAcl acl) {
        jdbcTemplate.batchUpdate(INSERT_ENTRY,
            new BatchPreparedStatementSetter() {
                public int getBatchSize() {
                    return acl.getEntries().length;
                }

                public void setValues(PreparedStatement stmt, int i)
                        throws SQLException {
                    AccessControlEntry entry_ = (AccessControlEntry) Array.get(acl.getEntries(), i);
                    Assert.isTrue(entry_ instanceof AccessControlEntryImpl, "Unknown ACE class");

                    AccessControlEntryImpl entry = (AccessControlEntryImpl) entry_;

		    stmt.setLong(1, new Long(jdbcTemplate.queryForInt(IDENTITY_QUERY)) );
                    stmt.setLong(2, ((Long) acl.getId()).longValue());
                    stmt.setInt(3, i);
                    stmt.setLong(4, createOrRetrieveSidPrimaryKey(entry.getSid(), true).longValue());
                    stmt.setInt(5, entry.getPermission().getMask());
                    stmt.setBoolean(6, entry.isGranting());
                    stmt.setBoolean(7, entry.isAuditSuccess());
                    stmt.setBoolean(8, entry.isAuditFailure());
                }
            });
    }

    /**
     * Creates an entry in the acl_object_identity table for the passed ObjectIdentity. The Sid is also
     * necessary, as acl_object_identity has defined the sid column as non-null.
     *
     * @param object to represent an acl_object_identity for
     * @param owner for the SID column (will be created if there is no acl_sid entry for this particular Sid already)
     */
    protected void createObjectIdentity(ObjectIdentity object, Sid owner) {
        Long sidId = createOrRetrieveSidPrimaryKey(owner, true);
        Long classId = createOrRetrieveClassPrimaryKey(object.getJavaType(), true);
        jdbcTemplate.update(INSERT_OBJECT_IDENTITY,
            new Object[] {new Long(jdbcTemplate.queryForInt(IDENTITY_QUERY)), classId, object.getIdentifier().toString(), sidId, Boolean.TRUE});
    }

    /**
     * Retrieves the primary key from acl_class, creating a new row if needed and the allowCreate property is
     * true.
     *
     * @param clazz to find or create an entry for (this implementation uses the fully-qualified class name String)
     * @param allowCreate true if creation is permitted if not found
     *
     * @return the primary key or null if not found
     */
    protected Long createOrRetrieveClassPrimaryKey(Class<?> clazz, boolean allowCreate) {
        List<?> classIds = jdbcTemplate.queryForList(SELECT_CLASS_PRIMARY_KEY, new Object[] {clazz.getName()}, Long.class);
        Long classId = null;

        if (classIds.isEmpty()) {
            if (allowCreate) {
                classId = new Long(jdbcTemplate.queryForInt(IDENTITY_QUERY));
                jdbcTemplate.update(INSERT_CLASS, new Object[] {classId, clazz.getName()});
                Assert.isTrue(TransactionSynchronizationManager.isSynchronizationActive(),
                        "Transaction must be running");
            }
        } else {
            classId = (Long) classIds.iterator().next();
        }

        return classId;
    }

    /**
     * Retrieves the primary key from acl_sid, creating a new row if needed and the allowCreate property is
     * true.
     *
     * @param sid to find or create
     * @param allowCreate true if creation is permitted if not found
     *
     * @return the primary key or null if not found
     *
     * @throws IllegalArgumentException DOCUMENT ME!
     */
    protected Long createOrRetrieveSidPrimaryKey(Sid sid, boolean allowCreate) {
        Assert.notNull(sid, "Sid required");

        String sidName = null;
        boolean principal = true;

        if (sid instanceof PrincipalSid) {
            sidName = ((PrincipalSid) sid).getPrincipal();
        } else if (sid instanceof GrantedAuthoritySid) {
            sidName = ((GrantedAuthoritySid) sid).getGrantedAuthority();
            principal = false;
        } else {
            throw new IllegalArgumentException("Unsupported implementation of Sid");
        }

        List<?> sidIds = jdbcTemplate.queryForList(SELECT_SID_PRIMARY_KEY, new Object[] {principal, sidName}, Long.class);
        Long sidId = null;

        if (sidIds.isEmpty()) {
            if (allowCreate) {
                sidId = new Long(jdbcTemplate.queryForInt(IDENTITY_QUERY));
                jdbcTemplate.update(INSERT_SID, new Object[] {sidId, principal, sidName});
                Assert.isTrue(TransactionSynchronizationManager.isSynchronizationActive(),
                        "Transaction must be running");
            }
        } else {
            sidId = (Long) sidIds.iterator().next();
        }

        return sidId;
    }

    /**
     *
     */
    public void deleteAcl(ObjectIdentity objectIdentity, boolean deleteChildren)
        throws ChildrenExistException {
        Assert.notNull(objectIdentity, "Object Identity required");
        Assert.notNull(objectIdentity.getIdentifier(), "Object Identity doesn't provide an identifier");

        // Recursively call this method for children, or handle children if they don't want automatic recursion
        ObjectIdentity[] children = findChildren(objectIdentity);

        if (deleteChildren) {
	        for (ObjectIdentity aChildren : children) {
		        deleteAcl(aChildren, true);
	        }
        } else if (children.length > 0) {
            throw new ChildrenExistException("Cannot delete '" + objectIdentity + "' (has " + children.length
                + " children)");
        }

        // Delete this ACL's ACEs in the acl_entry table
        deleteEntries(objectIdentity);

        // Delete this ACL's acl_object_identity row
        deleteObjectIdentityAndOptionallyClass(objectIdentity);

        // Clear the cache
        aclCache.evictFromCache(objectIdentity);
    }

    /**
     * Deletes all ACEs defined in the acl_entry table belonging to the presented ObjectIdentity
     *
     * @param oid the rows in acl_entry to delete
     */
    protected void deleteEntries(ObjectIdentity oid) {
        jdbcTemplate.update(DELETE_ENTRY_BY_OBJECT_IDENTITY_FK,
                new Object[] {retrieveObjectIdentityPrimaryKey(oid)});
    }

    /**
     * Deletes a single row from acl_object_identity that is associated with the presented ObjectIdentity. In
     * addition, deletes the corresponding row from acl_class if there are no more entries in acl_object_identity that
     * use that particular acl_class. This keeps the acl_class table reasonably small.
     *
     * @param oid to delete the acl_object_identity (and clean up acl_class for that class name if appropriate)
     */
    protected void deleteObjectIdentityAndOptionallyClass(ObjectIdentity oid) {
        // Delete the acl_object_identity row
        jdbcTemplate.update(DELETE_OBJECT_IDENTITY_BY_PRIMARY_KEY, new Object[] {retrieveObjectIdentityPrimaryKey(oid)});

        // Delete the acl_class row, assuming there are no other references to it in acl_object_identity
        Object[] className = {oid.getJavaType().getName()};
        long numObjectIdentities = jdbcTemplate.queryForLong(SELECT_COUNT_OBJECT_IDENTITY_ROWS_FOR_PARTICULAR_CLASSNAME_STRING,
                className);

        if (numObjectIdentities == 0) {
            // No more rows
            jdbcTemplate.update(DELETE_CLASS_BY_CLASSNAME_STRING, className);
        }
    }

    /**
     * Retrieves the primary key from the acl_object_identity table for the passed ObjectIdentity. Unlike some
     * other methods in this implementation, this method will NOT create a row (use {@link
     * #createObjectIdentity(ObjectIdentity, Sid)} instead).
     *
     * @param oid to find
     *
     * @return the object identity or null if not found
     */
    protected Long retrieveObjectIdentityPrimaryKey(ObjectIdentity oid) {
        try {
            return new Long(jdbcTemplate.queryForLong(SELECT_OBJECT_IDENTITY_PRIMARY_KEY,
                    new Object[] {oid.getJavaType().getName(), oid.getIdentifier()}));
        } catch (DataAccessException notFound) {
            return null;
        }
    }

    /**
     * This implementation will simply delete all ACEs in the database and recreate them on each invocation of
     * this method. A more comprehensive implementation might use dirty state checking, or more likely use ORM
     * capabilities for create, update and delete operations of {@link MutableAcl}.
     *
     * @param acl DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws NotFoundException DOCUMENT ME!
     */
    public MutableAcl updateAcl(MutableAcl acl) throws NotFoundException {
        Assert.notNull(acl.getId(), "Object Identity doesn't provide an identifier");

        // Delete this ACL's ACEs in the acl_entry table
        deleteEntries(acl.getObjectIdentity());

        // Create this ACL's ACEs in the acl_entry table
        createEntries(acl);

        // Change the mutable columns in acl_object_identity
        updateObjectIdentity(acl);

        // Clear the cache
        aclCache.evictFromCache(acl.getObjectIdentity());

        // Retrieve the ACL via superclass (ensures cache registration, proper retrieval etc)
        return (MutableAcl) super.readAclById(acl.getObjectIdentity());
    }

    /**
     * Updates an existing acl_object_identity row, with new information presented in the passed MutableAcl
     * object. Also will create an acl_sid entry if needed for the Sid that owns the MutableAcl.
     *
     * @param acl to modify (a row must already exist in acl_object_identity)
     *
     * @throws NotFoundException DOCUMENT ME!
     */
    protected void updateObjectIdentity(MutableAcl acl) {
        Long parentId = null;

        if (acl.getParentAcl() != null) {
            Assert.isInstanceOf(ObjectIdentityImpl.class, acl.getParentAcl().getObjectIdentity(),
                "Implementation only supports ObjectIdentityImpl");

            ObjectIdentityImpl oii = (ObjectIdentityImpl) acl.getParentAcl().getObjectIdentity();
            parentId = retrieveObjectIdentityPrimaryKey(oii);
        }

        Assert.notNull(acl.getOwner(), "Owner is required in this implementation");

        Long ownerSid = createOrRetrieveSidPrimaryKey(acl.getOwner(), true);
        int count = 0;
        if (parentId == null) {
        	count = jdbcTemplate.update(UPDATE_OBJECT_IDENTITY,
        			new Object[] {ownerSid, acl.isEntriesInheriting(), acl.getId()});
        }
        else {
        	count = jdbcTemplate.update(UPDATE_OBJECT_IDENTITY_WITH_PARENT,
        			new Object[] {parentId, ownerSid, acl.isEntriesInheriting(), acl.getId()});
        }

        if (count != 1) {
            throw new NotFoundException("Unable to locate ACL to update");
        }
    }

}
