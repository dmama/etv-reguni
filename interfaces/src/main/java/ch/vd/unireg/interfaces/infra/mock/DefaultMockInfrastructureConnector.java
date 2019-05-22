package ch.vd.unireg.interfaces.infra.mock;

import java.lang.reflect.Field;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;

public class DefaultMockInfrastructureConnector extends MockInfrastructureConnector {

	protected static final MockInfrastructureConnector staticInstance = new MockInfrastructureConnector() {
		// CHECKSTYLE:OFF
		@Override
		protected void init() {
		}
		// CHECKSTYLE:ON
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
			// CHECKSTYLE:OFF
			catch (IllegalAccessException e) {
				// tant pis, on ignore
			}
			// CHECKSTYLE:ON
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
		//SIFISC-3468
		//Essayez de faire ce qui a dessous en static pour voir ...
		//La Vallee avait urgemment besoin d'une adresse avec Localité donc ...
		final Adresse adresseSentier = new MockAdresse(TypeAdresseCivil.COURRIER, new CasePostale(TexteCasePostale.CASE_POSTALE, 256), null, MockLocalite.LeSentier, RegDate.get(2002, 1, 1), null);
		MockOfficeImpot.OID_LA_VALLEE.setAdresse(adresseSentier);

		final MockAdresse adresseVevey = new MockAdresse(TypeAdresseCivil.COURRIER, new CasePostale(TexteCasePostale.CASE_POSTALE, 1032), "Rue du Simplon 22", MockLocalite.Vevey, RegDate.get(2002, 1, 1), null);
		MockOfficeImpot.OID_VEVEY.setAdresse(adresseVevey);
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
}
