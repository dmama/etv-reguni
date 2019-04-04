package ch.vd.unireg.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.type.Niveau;

/**
 * Mock du service de SecurityProvider (qui ne s'occupe pour le moment que des rôles)
 */
public class MockSecurityProvider implements SecurityProviderInterface {

	private final Set<Role> roles;
	private Set<Long> dossiersProteges;

	public MockSecurityProvider(Collection<Role> roles) {
		this.roles = roles.stream().filter(Objects::nonNull).collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
		this.dossiersProteges = Collections.emptySet();
	}

	public MockSecurityProvider(Role... roles) {
		this(Arrays.asList(roles));
	}

	public void setDossiersProteges(Set<Long> dossiersProteges) {
		this.dossiersProteges = dossiersProteges;
	}

	public void setDossiersProteges(Long... dossiersProteges) {
		this.dossiersProteges = new HashSet<>(Arrays.<Long>asList(dossiersProteges));
	}

	@Override
	public boolean isGranted(Role role, String visaOperateur, int codeCollectivite) {
		return roles.contains(role);
	}

	@Override
	public Niveau getDroitAcces(String visaOperateur, long tiersId) throws ObjectNotFoundException {
		if (dossiersProteges.contains(tiersId)) {
			return null;
		}
		return Niveau.ECRITURE;
	}

	@Override
	public List<Niveau> getDroitsAcces(String visa, List<Long> ids) {
		final List<Niveau> res = new ArrayList<>(ids.size());
		for (Long id : ids) {
			if (id != null) {
				res.add(getDroitAcces(visa, id));
			}
			else {
				res.add(null);
			}
		}
		return res;
	}
}
