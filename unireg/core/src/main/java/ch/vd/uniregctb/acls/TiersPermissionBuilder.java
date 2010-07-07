package ch.vd.uniregctb.acls;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.acegisecurity.acls.Permission;
import org.acegisecurity.acls.domain.CumulativePermission;
import org.springframework.util.Assert;

/**
 * Implementation de PermissionBuilder pour les permissions de type TiersPermission.
 *
 * @author Ludovic Bertin (OOSphère)
 */
public class TiersPermissionBuilder implements PermissionBuilder {

    /**
     * Map pour retrouver une permission a partir d'une valeur entière.
     */
	private static Map<Integer, TiersPermission> locallyDeclaredPermissionsByInteger = new HashMap<Integer, TiersPermission>();
	private static boolean permsInited = false;

	/**
	 * Bloc d'initialisation pour charger la map de permissions.
	 */
    private void initPerms() throws RuntimeException {
    	if (!permsInited) {

    		permsInited = true;

    		Field[] fields = TiersPermission.class.getDeclaredFields();

		    for (Field field : fields) {
			    try {
				    Object fieldValue = field.get(null);

				    if (TiersPermission.class.isAssignableFrom(fieldValue.getClass())) {
					    // Found a BasePermission static field
					    TiersPermission perm = (TiersPermission) fieldValue;
					    locallyDeclaredPermissionsByInteger.put(new Integer(perm.getMask()), perm);
				    }
			    }
			    catch (Exception e) {
				    throw new RuntimeException(e);
			    }
		    }
    	}
    }

    /**
     * Crée dynamiquement une <code>CumulativePermission</code> ou <code>TiersPermission</code>
     * représentant les bits actifs dans le masque donnée en paramètre.
     *
     * @param mask 	le masque
     *
     * @return une permission correspondant au masque
     */
    public Permission buildFromMask(int mask) {

    	initPerms();

        if ( locallyDeclaredPermissionsByInteger.containsKey(mask) ) {

            // le masque correspond exactement a une permission déclaréé statiquement
            return locallyDeclaredPermissionsByInteger.get(mask);
        }

        // sinon, il faut construire un objet CumulativePermission
        CumulativePermission permission = new CumulativePermission();

        for (int i = 0; i < 32; i++) {
            int permissionToCheck = 1 << i;

            if ((mask & permissionToCheck) == permissionToCheck) {
                Permission p = locallyDeclaredPermissionsByInteger.get(permissionToCheck);
                Assert.state(p != null,
                    "Mask " + permissionToCheck + " does not have a corresponding static BasePermission");
                permission.set(p);
            }
        }

        return permission;
    }

}
