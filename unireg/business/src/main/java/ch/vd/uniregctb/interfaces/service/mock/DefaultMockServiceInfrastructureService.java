package ch.vd.uniregctb.interfaces.service.mock;

import java.lang.reflect.Field;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;

public class DefaultMockServiceInfrastructureService extends MockServiceInfrastructureService {

	protected static MockServiceInfrastructureService staticInstance = new MockServiceInfrastructureService() {
		@Override
		protected void init() {
		}
	};

	/**
	 * Force le chargement par le classloader de toutes les membres statiques de la classe spécifiée (autrement, les membres statiques sont initialisés à la demande).
	 *
	 * @param clazz la classe dont on veut provoquer le chargement
	 */
	private static void forceLoad(Class clazz) {
		// charge tous les membres statiques de la classe
		for (Field f : clazz.getFields()) {
			try {
				f.get(null);
			}
			catch (IllegalAccessException e) {
				// tant pis, on ignore
			}
		}
		// charge toutes les inner classes statiques
		for (Class c : clazz.getDeclaredClasses()) {
			forceLoad(c);
		}
	}

	@Override
	protected void init() {
		forceLoad(MockPays.class);
		forceLoad(MockCanton.class);
		forceLoad(MockCommune.class);
		forceLoad(MockLocalite.class);
		forceLoad(MockRue.class);
		forceLoad(MockCollectiviteAdministrative.class);

		copyFrom(staticInstance);
	}

	public static void addLocalite(MockLocalite loc) {
		staticInstance.add(loc);
	}

	public static void addCanton(MockCanton c) {
		staticInstance.add(c);
	}

	public static void addPays(MockPays p) {
		staticInstance.add(p);
	}

	public static void addRue(MockRue r) {
		staticInstance.add(r);
	}

	public static void addCommune(MockCommune c) {
		staticInstance.add(c);
	}

	public static void addColAdm(MockCollectiviteAdministrative collectiviteAdministrative) {
		staticInstance.add(collectiviteAdministrative);
	}

	@Override
	public Pays getPaysInconnu() throws InfrastructureException {
		return MockPays.PaysInconnu;
	}
}
