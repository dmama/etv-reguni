package ch.vd.uniregctb.dbunit;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.hibernate.meta.MetaEntity;
import ch.vd.uniregctb.hibernate.meta.Property;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Faux-test mais vrai utilitaire qui permet de lire un fichier DBUnit et de générer le code qui charge les mêmes données mais en pure Java.
 */
@SuppressWarnings({"JavaDoc"})
public class DbUnit2Java extends BusinessTest {

	private final static String DB_UNIT_FILE = "/home/msi/projets/registre/unireg/trunk/04-Implementation/unireg/web/src/main/resources/DBUnit4Import/tiers-basic.xml";
	private final static List<Class> hibernateBaseClasses = new ArrayList<Class>();
	private final Map<Class, List<Class>> baseClassToConcreteOnes = new HashMap<Class, List<Class>>();
	private final Map<Class, ClassInfo> concreteClassInfo = new HashMap<Class, ClassInfo>();
	private final Map<HibernateEntity, String> entityInstanceNames = new HashMap<HibernateEntity, String>();

	static {
		hibernateBaseClasses.add(PeriodeFiscale.class);
		hibernateBaseClasses.add(ParametrePeriodeFiscale.class);
		hibernateBaseClasses.add(ModeleDocument.class);
		hibernateBaseClasses.add(ModeleFeuilleDocument.class);

		hibernateBaseClasses.add(Tiers.class);
		hibernateBaseClasses.add(AdresseTiers.class);
		hibernateBaseClasses.add(Declaration.class);
		hibernateBaseClasses.add(EtatDeclaration.class);
		hibernateBaseClasses.add(DelaiDeclaration.class);
		hibernateBaseClasses.add(RapportEntreTiers.class);
		hibernateBaseClasses.add(ForFiscal.class);
		hibernateBaseClasses.add(SituationFamille.class);
		hibernateBaseClasses.add(IdentificationPersonne.class);
		hibernateBaseClasses.add(Periodicite.class);
		hibernateBaseClasses.add(Tache.class);
	}

	private static class ClassInfo {
		public final Class baseClass;
		public final Class clazz;
		public final MetaEntity meta;
		public final String instanceName;
		public int instanceCount = 0;

		private ClassInfo(Class baseClass, Class clazz) throws Exception {
			this.baseClass = baseClass;
			this.clazz = clazz;
			this.meta = MetaEntity.determine(clazz);
			this.instanceName = buildInstanceName(clazz);
		}

		private static String buildInstanceName(Class clazz) {
			String name = "";
			String classname = clazz.getSimpleName();
			for (int i = 0; i < classname.length(); i++) {
				final char c = classname.charAt(i);
				if (Character.isUpperCase(c)) {
					name += c;
				}
			}
			return name.toLowerCase();
		}

		public String nextInstanceName() {
			return instanceName + instanceCount++;
		}
	}

	/**
	 * Cette méthode charge les données du fichier DBUnit pointé par {@link ch.vd.uniregctb.dbunit.DbUnit2Java#DB_UNIT_FILE} dans la base de données, puis imprime dans <i>stdout</i> le code qui charge
	 * les mêmes données dans la base mais en pure Java.
	 */
	@Ignore(value = "à lancer à la main lorsque nécessaire")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void convertXMLToJavaCode() throws Exception {

		initMetaData();
		loadDatabase(DB_UNIT_FILE);
		printData();
	}

	@SuppressWarnings({"unchecked"})
	private void initMetaData() throws Exception {
		final List<String> annotatedClasseNames = getBean(List.class, "hibernateAnnotatedClasses");
		for (String classname : annotatedClasseNames) {
			final Class clazz = Class.forName(classname);

			// construit les relations classe de basse -> classes concrètes
			Class baseClass = null;
			for (Class c : hibernateBaseClasses) {
				if (c.isAssignableFrom(clazz)) {
					baseClass = c;
					final List<Class> list = getConcreteClasses(c);
					list.add(clazz);
					break;
				}
			}

			if (baseClass != null) { // on ignore certaines classes comme MigregError
				// construit la méta-entité
				concreteClassInfo.put(clazz, new ClassInfo(baseClass, clazz));
			}
		}
	}

	private void printData() throws Exception {
		for (Class baseClass : hibernateBaseClasses) {
			final List<?> entities = hibernateTemplate.find("from " + baseClass.getSimpleName());
			for (Object o : entities) {
				print((HibernateEntity) o);
				System.out.println();
			}
		}
	}

	private List<Class> getConcreteClasses(Class baseClass) {
		List<Class> list = baseClassToConcreteOnes.get(baseClass);
		if (list == null) {
			list = new ArrayList<Class>();
			baseClassToConcreteOnes.put(baseClass, list);
		}
		return list;
	}

	private void print(HibernateEntity o) throws Exception {

		final ClassInfo info = concreteClassInfo.get(o.getClass());
		Assert.notNull(info);
		final String clazz = o.getClass().getSimpleName();
		final String instance = info.nextInstanceName();
		entityInstanceNames.put(o, instance);

		String n = clazz + ' ' + instance + " = new " + clazz + "();";
		System.out.println(n);

		Property parentProp = null;
		HibernateEntity parent = null;

		for (Property prop : info.meta.getProperties()) {
			if (prop.isDiscriminator()) {
				continue;
			}

			if (prop.isCollection()) {
				// on initialise une collection vide, comme le fait Hibernate
				String s = instance + '.' + toSetter(prop.getName()) + "(new HashSet());";
				System.out.println(s);
			}
			else {
				// Les ID ne peuvent pas être spécifiés sur les classes autres que Tiers (et descendants)
				if (prop.isPrimaryKey() && !(o instanceof Tiers)) {
					continue;
				}

				final Object value = o.getValue(prop.getName());
				if (value == null) {
					continue;
				}

				if (prop.isParentForeignKey()) {
					parentProp = prop;
					parent = (HibernateEntity) value;
				}
				else {
					// on initialise la valeur trouvée
					String s = instance + '.' + toSetter(prop.getName()) + '(' + toCode(value) + ");";
					System.out.println(s);
				}
			}
		}

		if (parentProp != null && !(o instanceof Declaration)) {
			final String parentInstanceName = entityInstanceNames.get(parent);
			Assert.notNull(parentInstanceName);
			String s = parentInstanceName + ".add" + info.baseClass.getSimpleName() + '(' + instance + ");";
			System.out.println(s);

			// On sauve le parent qui va sauver l'instance
			String m = parentInstanceName + " = (" + parent.getClass().getSimpleName() + ") hibernateTemplate.merge(" + parentInstanceName + ");";
			System.out.println(m);
		}
		else {

			// Cas spécial pour les déclarations qui doivent être sauvées par elles-mêmes
			if (o instanceof Declaration) {
				final String parentInstanceName = entityInstanceNames.get(parent);
				Assert.notNull(parentInstanceName);
				System.out.println(instance + ".setTiers(" + parentInstanceName + ");");
			}

			// On sauve l'instance
			String m = instance + " = (" + clazz + ") hibernateTemplate.merge(" + instance + ");";
			System.out.println(m);

			// Cas spécial pour les rapports-entre-tiers qui sont déconnectés artificellement des parents,
			// mais qui doivent quand même être insérés dans les collections qui vont bien
			if (o instanceof RapportEntreTiers) {
				final RapportEntreTiers ret = (RapportEntreTiers) o;
				final Long sujetId = ret.getSujetId();
				final Long objetId = ret.getObjetId();

				String sujetName = null;
				String objetName = null;
				for (Map.Entry<HibernateEntity, String> e : entityInstanceNames.entrySet()) {
					final HibernateEntity entity = e.getKey();
					if (entity instanceof Tiers) {
						final Tiers tiers = (Tiers) entity;
						if (tiers.getId().equals(sujetId)) {
							sujetName = e.getValue();
						}
						else if (tiers.getId().equals(objetId)) {
							objetName = e.getValue();
						}
					}
				}
				Assert.notNull(sujetName);
				Assert.notNull(objetName);

				System.out.println(sujetName + ".addRapportSujet(" + instance + ");");
				System.out.println(objetName + ".addRapportObjet(" + instance + ");");
			}
		}
	}

	private String toCode(Object value) {
		if (value instanceof Timestamp) {
			return "new Timestamp(" + ((Date) value).getTime() + "L)";
		}
		else if (value instanceof Date) {
			return "new Date(" + ((Date) value).getTime() + "L)";
		}
		else if (value instanceof RegDate) {
			final RegDate rd = (RegDate) value;
			return "RegDate.get(" + rd.year() + ", " + rd.month() + ", " + rd.day() + ')';
		}
		else if (value instanceof Enum) {
			final Enum e = (Enum) value;
			return e.getDeclaringClass().getSimpleName() + '.' + e.name();
		}
		else if (value instanceof Long) {
			return value.toString() + 'L';
		}
		else if (value instanceof String) {
			return '\"' + value.toString() + '\"';
		}
		else if (value instanceof HibernateEntity) {
			final HibernateEntity entity = (HibernateEntity) value;
			final String name = entityInstanceNames.get(entity);
			Assert.notNull(name);
			return name;
		}
		else {
			return value.toString();
		}
	}

	private String toSetter(String property) {
		return "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
	}
}
