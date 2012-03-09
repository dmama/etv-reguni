package ch.vd.uniregctb.hibernate.meta;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserType;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.ReflexionUtils;
import ch.vd.uniregctb.hibernate.EnumUserType;
import ch.vd.uniregctb.hibernate.RegDateUserType;
import ch.vd.uniregctb.hibernate.TypeAdresseCivilLegacyUserType;
import ch.vd.uniregctb.hibernate.URLUserType;

/**
 * Classe qui expose de manière pratique les méta-informations (colonnes, nom de table, discriminant, ...) d'une entité Hibernate.
 */
public class MetaEntity {

	protected static final Logger LOGGER = Logger.getLogger(MetaEntity.class);

	private final String table;
	private final String discriminant;
	private final Class<?> type;
	private Sequence sequence;
	private List<Property> properties;

	private MetaEntity(String table, String discriminant, Class<?> type) {
		this.table = table;
		this.discriminant = discriminant;
		this.type = type;
	}

	/**
	 * @return le nom de la table dans laquelle les données sont stockées
	 */
	public String getTable() {
		return table;
	}

	/**
	 * @return la valeur du discriminant lorsque l'entité Hibernate fait partir d'une hiérarchie de classes stockée à plat dans une table; ou <b>null</b> lorsque ce n'est pas le cas.
	 */
	public String getDiscriminant() {
		return discriminant;
	}

	/**
	 * @return la classe de l'entité Hibernate considérée.
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * @return la liste des colonnes (= propriétés) déclarées sur l'entité Hibernate.
	 */
	public List<Property> getProperties() {
		return properties;
	}

	private void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	public Property getProperty(String s) {
		for (Property property : properties) {
			if (property.getName().equals(s)) {
				return property;
			}
		}
		return null;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	/**
	 * Analyse la classe (qui doit être une entité hibernate) spécifiée, et crée une instance de la méta-entité.
	 *
	 * @param clazz la classe d'une entité hibernate
	 * @return une nouvelle instance de la classe 'MetaEntity' avec les méta-informations trouvées
	 * @throws Exception en cas d'erreur inattendue
	 */
	@SuppressWarnings({"unchecked"})
	public static MetaEntity determine(Class clazz) throws MetaException {

		boolean entityFound = false;
		boolean isEmbeddable = false;
		String table = null;
		String discriminatorValue = null;
		String discriminatorColumn = null;
		final List<Property> properties = new ArrayList<Property>();

		final List<Annotation> annotations = ReflexionUtils.getAllAnnotations(clazz);
		for (Annotation a : annotations) {
			if (a instanceof Entity) {
				entityFound = true;
			}
			else if (a instanceof Embeddable) {
				isEmbeddable = true;
			}
			else if (a instanceof DiscriminatorValue) {
				final DiscriminatorValue d = (DiscriminatorValue) a;
				if (discriminatorValue != null) {
					throw new IllegalArgumentException("Duplicated discriminator = [" + discriminatorValue + ", " + d.value() + "]) on class [" + clazz.getSimpleName() + ']');
				}
				discriminatorValue = d.value();
			}
			else if (a instanceof DiscriminatorColumn) {
				final DiscriminatorColumn d = (DiscriminatorColumn) a;
				discriminatorColumn = d.name();
			}
			else if (a instanceof Table) {
				final Table t = (Table) a;
				table = t.name();
			}
		}

		if (!entityFound && !isEmbeddable) {
			throw new MetaException("La classe [" + clazz.getSimpleName() + "] n'est pas une entité hibernate !");
		}

		if (discriminatorValue != null) {
			properties.add(new Property(null, PropertyType.stringPropType, discriminatorColumn, discriminatorValue, false, false, false));
		}

		final MetaEntity entity = new MetaEntity(table, discriminatorValue, clazz);
		try {
			final Map<String, PropertyDescriptor> descriptors = ReflexionUtils.getPropertyDescriptors(clazz);
			for (PropertyDescriptor descriptor : descriptors.values()) {
				List<Property> props = determineProps(clazz, entity, descriptor);
				if (props != null) {
					properties.addAll(props);
				}
			}
		}
		catch (MetaException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MetaException(e);
		}

		// on trie les attributs par ordre alphabétique, excepté les clés primaires et les descriminants qui restent toujours en premier
		Collections.sort(properties, new Comparator<Property>() {
			@Override
			public int compare(Property o1, Property o2) {
				if (o1.isPrimaryKey() && !o2.isPrimaryKey()) {
					return -1;
				}
				else if (o2.isPrimaryKey() && !o1.isPrimaryKey()) {
					return 1;
				}
				else {
					if (o1.isDiscriminator() && !o2.isDiscriminator()) {
						return -1;
					}
					else if (o2.isDiscriminator() && !o1.isDiscriminator()) {
						return 1;
					}
					else if (o1.isDiscriminator() && o2.isDiscriminator()) {
						return 0;
					}
					else {
						return o1.getName().compareTo(o2.getName());
					}
				}
			}
		});
		
		entity.setProperties(properties);
		return entity;
	}

	private static List<Property> determineProps(Class clazz, MetaEntity entity, PropertyDescriptor descriptor) throws IntrospectionException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, MetaException {
		if (descriptor.getName().equals("class")) {
			return null;
		}

		final Method readMethod = descriptor.getReadMethod();
		if (readMethod == null) {
			// LOGGER.debug("Ignoring descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] without read method");
			return null;
		}

		final Method writeMethod = descriptor.getWriteMethod();
		if (writeMethod == null) {
			// LOGGER.debug("Ignoring descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] without write method");
			return null;
		}
		if (!Modifier.isPublic(writeMethod.getModifiers())) {
			LOGGER.warn("Write method for descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] is not public");
			return null;
		}

		boolean estTransient = false;
		boolean estColonne = false;
		boolean estCollection = false;
		String columnName = null;
		Class<?> returnType = null;
		UserType userType = null;
		boolean primaryKey = false;
		boolean parentForeignKey = false;
		boolean otherForeignKey = false;
		String sequenceName = null;
		String generatorClassname = null;

		final Annotation[] fieldAnnotations = readMethod.getAnnotations();
		for (Annotation a : fieldAnnotations) {
			if (a instanceof javax.persistence.Column) {
				final javax.persistence.Column c = (javax.persistence.Column) a;
				columnName = c.name();
				returnType = readMethod.getReturnType();
				estColonne = true;
			}
			else if (a instanceof Id) {
				columnName = "ID";
				returnType = Long.class;
				primaryKey = true;
				estColonne = true;
			}
			else if (a instanceof JoinColumn) {
				final JoinColumn j = (JoinColumn) a;
				columnName = j.name();
				if (isParentOf(readMethod.getReturnType(), columnName, clazz, descriptor.getName())) {
					parentForeignKey = true;
					returnType = Long.class;
				}
				else {
					otherForeignKey = true;
					returnType = readMethod.getReturnType();
				}
				estColonne = true;
			}
			else if (a instanceof OneToMany) {
				estCollection = true;
				final OneToMany otm = (OneToMany) a;
				if (StringUtils.isNotBlank(otm.mappedBy())) {
					estColonne = true;
					columnName = getMappedColumnName(readMethod, otm);
				}
			}
			else if (a instanceof Type) {
				final Type t = (Type) a;
				final String userTypeClassname = t.type();
				final Class<?> userTypeClass = Class.forName(userTypeClassname);
				userType = (UserType) userTypeClass.newInstance();
			}
			else if (a instanceof Transient) {
				estTransient = true;
			}
			else if (a instanceof GeneratedValue) {
				final GeneratedValue g = (GeneratedValue) a;
				if (g.strategy() == GenerationType.AUTO) {
					sequenceName = "hibernate_sequence";
				}
			}
			else if (a instanceof GenericGenerator) {
				final GenericGenerator gg = (GenericGenerator) a;
				generatorClassname = gg.strategy();
			}
			else if (a instanceof Embedded) {
				// une propriété emdeddée : on l'expose à plat.
				return determineEmbeddedProps(descriptor);
			}
		}

		if (sequenceName != null) {
			entity.setSequence(new Sequence(sequenceName));
		}
		else if (generatorClassname != null) {
			Class generatorClass = Class.forName(generatorClassname);
			entity.setSequence(new Sequence(generatorClass));
		}

		if (estTransient) {
			return null;
		}

		if (!estColonne) {
			LOGGER.warn("No @Transient nor @Column annotation found on descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + ']');
			return null;
		}

		final PropertyType propertyType;
		if (estCollection) {
			// dans le cas de collections, on va stocker le type générique de la collection comme valeur de retour
			final Class genericType = getGenericParamReturnType(readMethod);
			Assert.notNull(genericType);
			propertyType = new CollectionPropertyType(genericType);
		}
		else if (userType != null) {
			if (userType instanceof RegDateUserType) {
				propertyType = new RegDatePropertyType((RegDateUserType) userType);
			}
			else if (userType instanceof EnumUserType) {
				propertyType = new EnumUserTypePropertyType(returnType, (EnumUserType) userType);
			}
			else if (userType instanceof TypeAdresseCivilLegacyUserType) {
				propertyType = new TypeAdresseCivilLegacyPropertyType((TypeAdresseCivilLegacyUserType) userType);
			}
			else if (userType instanceof URLUserType) {
				propertyType = new URLPropertyType((URLUserType) userType);
			}
			else {
				throw new NotImplementedException("Type de user-type inconnu = [" + userType.getClass().getName() + ']');
			}

		}
		else if (otherForeignKey) {
			propertyType = new JoinPropertyType(returnType);
		}
		else {
			propertyType = PropertyType.byJavaType.get(returnType);
			Assert.notNull(propertyType, "Type java non-enregistré [" + returnType.getName() + "] (propriété = [" + descriptor.getName() + "] de la classe [" + clazz.getSimpleName() + "])");
		}

		return Arrays.asList(new Property(descriptor.getName(), propertyType, columnName, null, primaryKey, parentForeignKey, estCollection));
	}

	/**
	 * Détermine les propriétés <i>à plat</i> d'un attribut qui utilise une structure embeddée (annotation @Embedded). Les noms des propriétés embeddées sont préfixées avec le nom de l'attribut parent.
	 * <p/>
	 * <b>Exemple :</b> un attribut <i>fraction</i> qui pointe vers une classe composée de deux entiers <i>numerator</i> et <i>denominator</i> retournera la liste de propriétés suivantes : <ol>
	 * <li>"fraction.numerator" : integer</li> <li>"fraction.denominator" : integer</li> </ol>
	 *
	 * @param descriptor le descripteur de l'attribut embeddé
	 * @return la liste des propriétés correpondantes.
	 * @throws MetaException en cas d'exception
	 */
	private static List<Property> determineEmbeddedProps(PropertyDescriptor descriptor) throws MetaException {

		boolean embeddedFound = false;
		AttributeOverride[] overrides = null;

		final Method readMethod = descriptor.getReadMethod();

		// Détermine les annotations du getter de la classe principale
		final Annotation[] fieldAnnotations = readMethod.getAnnotations();
		for (Annotation a : fieldAnnotations) {
			if (a instanceof Embedded) {
				embeddedFound = true;
			}
			else if (a instanceof AttributeOverrides) {
				final AttributeOverrides ao = (AttributeOverrides) a;
				overrides = ao.value();
			}
		}

		if (!embeddedFound) {
			throw new MetaException("L'attribut [" + descriptor.getName() + "] n'est pas embeddé !");
		}

		// Détermine la méta-classe de la classe embeddée
		final Class<?> embeddedClass = readMethod.getReturnType();
		MetaEntity meta = determine(embeddedClass);

		// Construit les attributs résultants (notation pointée pour les noms des attributs + éventuellement surcharge du nom de la colonne)
		final List<Property> embeddedProps = meta.getProperties();
		final List<Property> results = new ArrayList<Property>(embeddedProps.size());
		for (Property e : embeddedProps) {
			final AttributeOverride override = getOverride(overrides, e.getName());
			final String fullName = descriptor.getName() + '.' + e.getName();
			final String columnName = override == null ? e.getColumnName() : override.column().name();
			Property n = new Property(fullName, e.getType(), columnName, null, false, false, false);
			results.add(n);
		}

		return results;
	}

	private static AttributeOverride getOverride(AttributeOverride[] overrides, String attributeName) {
		AttributeOverride override = null;
		if (overrides != null) {
			for (AttributeOverride ao : overrides) {
				if (ao.name().equals(attributeName)) {
					override = ao;
					break;
				}
			}
		}
		return override;
	}

	private static String getMappedColumnName(Method readMethod, OneToMany otm) throws IntrospectionException {
		final Class elementType = getGenericParamReturnType(readMethod);
		final PropertyDescriptor descr = new PropertyDescriptor(otm.mappedBy(), elementType);
		final JoinColumn j = descr.getReadMethod().getAnnotation(JoinColumn.class);
		return j.name();
	}

	/**
	 * Détermine si la classe <i>main</i> est le parent de la classe <i>other</i>.
	 *
	 * @param main        la classe parent supposée
	 * @param joinColumn  le nom jdbc de la colonne de jointure
	 * @param other       la classe enfant supposée
	 * @param otherGetter le nom de la propriété pour aller de la classe <i>other</i> à la classe <i>main</i>
	 * @return <b>vrai</b> si la classe <i>main</i> est bien le parent de la classe <i>other</i>; <b>false</b> autrement.
	 * @throws java.beans.IntrospectionException
	 *          en cas de problème d'introspection
	 */
	private static boolean isParentOf(Class<?> main, String joinColumn, Class other, String otherGetter) throws IntrospectionException {

		final Map<String, PropertyDescriptor> descriptors = ReflexionUtils.getPropertyDescriptors(main);
		for (PropertyDescriptor descriptor : descriptors.values()) {
			if (descriptor.getName().equals("class")) {
				continue;
			}

			final Method readMethod = descriptor.getReadMethod();
			if (readMethod == null) {
				continue;
			}

			boolean hasOneToMany = false;
			String mappedByName = null;
			String joinColumnName = null;

			final Annotation[] fieldAnnotations = readMethod.getAnnotations();
			for (Annotation a : fieldAnnotations) {
				if (a instanceof OneToMany) {
					final OneToMany o = (OneToMany) a;
					hasOneToMany = true;
					mappedByName = o.mappedBy();
				}
				else if (a instanceof JoinColumn) {
					final JoinColumn j = (JoinColumn) a;
					joinColumnName = j.name();
				}
			}

			// la méthode courant détermine une relation parent->enfant si :
			//  - il possède une annotation OneToMany
			//  - il possède une annotation JoinColumn avec la même colonne de jointure
			//  - son type de retour est une collection générique dont le type est celui voulu
			if (hasOneToMany && (joinColumn.equals(joinColumnName) || otherGetter.equals(mappedByName)) && Collection.class.isAssignableFrom(readMethod.getReturnType())) {
				final Class genericType = getGenericParamReturnType(readMethod);
				if (genericType != null && genericType.isAssignableFrom(other)) {
					// le type de retour est une collection parametrisée et le type paramétrisé est celui voulu
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Retourne le type paramétrisé de la valeur de retour d'une méthode (par exemple <i>Long</i> pour la méthode avec la signature: <i>Collection&lt;Long&gt; getList()</i>).
	 *
	 * @param readMethod une méthode qui retourne une valeur
	 * @return le type paramétrisé de la valeur de retour; ou <i>null</i> si la valeur de retour n'est pas paramétrisé
	 */
	public static Class getGenericParamReturnType(Method readMethod) {
		final java.lang.reflect.Type returnType = readMethod.getGenericReturnType();
		if (returnType instanceof ParameterizedType) { // le type de return est une collection parametrisée
			final ParameterizedType parameterizedType = (ParameterizedType) returnType;
			final java.lang.reflect.Type[] types = parameterizedType.getActualTypeArguments();
			for (java.lang.reflect.Type typeArgument : types) {
				if (typeArgument instanceof Class) {
					return (Class) typeArgument;
				}
			}
		}
		return null;
	}
}
