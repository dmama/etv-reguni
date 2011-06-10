package ch.vd.uniregctb.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.type.Niveau;

/**
 * Mock du service de SecurityProvider (qui ne s'occupe pour le moment que des droits ifosec)
 */
public class MockSecurityProvider implements SecurityProviderInterface {

	private final Set<Role> roles;

	public MockSecurityProvider(Collection<Role> roles) {
		this.roles = new HashSet<Role>(roles);
	}

	public MockSecurityProvider(Role... roles) {
		this(Arrays.asList(roles));
	}

	@Override
	public boolean isGranted(Role role, String visaOperateur, int codeCollectivite) {
		return roles.contains(role);
	}

	@Override
	public Niveau getDroitAcces(String visaOperateur, long tiersId) throws ObjectNotFoundException {
		return Niveau.ECRITURE;
	}

	@Override
	public List<Niveau> getDroitAcces(String visa, List<Long> ids) {
		final List<Niveau> res = new ArrayList<Niveau>(ids.size());
		for (Long id : ids) {
			if (id != null) {
				res.add(Niveau.ECRITURE);
			}
			else {
				res.add(null);
			}
		}
		return res;
	}
}
