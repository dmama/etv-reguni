package ch.vd.uniregctb.hibernate.meta;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserType;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.ReflexionUtils;
import ch.vd.uniregctb.hibernate.EnumTypeAdresseUserType;
import ch.vd.uniregctb.hibernate.EnumUserType;
import ch.vd.uniregctb.hibernate.RegDateUserType;

/**
 * Classe qui expose de manière pratique les méta-informations (colonnes, nom de table, discriminant, ...) d'une entité Hibernate.
 */
public class MetaEntity {

	protected static final Logger LOGGER = Logger.getLogger(MetaEntity.class);

	private String table;
	private String discriminant;
	private Class<?> type;
	private List<Property> properties;

	public MetaEntity(String table, String discriminant, Class<?> type) {
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
	public List<Property> getColumns() {
		return properties;
	}

	private void setColumns(List<Property> properties) {
		this.properties = properties;
	}

	/**
	 * Analyse la classe (qui doit être une entité hibernate) spécifiée, et crée une instance de la méta-entité.
	 *
	 * @param clazz la classe d'une entité hibernate
	 * @return une nouvelle instance de la classe 'MetaEntity' avec les méta-informations trouvées
	 * @throws Exception en cas d'erreur inattendue
	 */
	public static MetaEntity determine(Class clazz) throws Exception {

		String table = null;
		String discriminant = null;
		final List<Property> properties = new ArrayList<Property>();

		final List<Annotation> annotations = ReflexionUtils.getAllAnnotations(clazz);
		for (Annotation a : annotations) {
			if (a instanceof DiscriminatorValue) {
				final DiscriminatorValue d = (DiscriminatorValue) a;
				if (discriminant != null) {
					throw new IllegalArgumentException("Duplicated discriminator = [" + discriminant + ", " + d.value() + "]) on class [" + clazz.getSimpleName() + "]");
				}
				discriminant = d.value();
			}
			else if (a instanceof DiscriminatorColumn) {
				final DiscriminatorColumn d = (DiscriminatorColumn) a;
				properties.add(new Property(null, PropertyType.stringPropType, d.name(), true, false, false));
			}
			else if (a instanceof Table) {
				final Table t = (Table) a;
				table = t.name();
			}
		}

		MetaEntity entity = new MetaEntity(table, discriminant, clazz);

		final Map<String, PropertyDescriptor> descriptors = ReflexionUtils.getPropertyDescriptors(clazz);
		for (PropertyDescriptor descriptor : descriptors.values()) {
			if (descriptor.getName().equals("class")) {
				continue;
			}

			final Method readMethod = descriptor.getReadMethod();
			if (readMethod == null) {
				// LOGGER.debug("Ignoring descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] without read method");
				continue;
			}

			final Method writeMethod = descriptor.getWriteMethod();
			if (writeMethod == null) {
				// LOGGER.debug("Ignoring descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] without write method");
				continue;
			}
			if (!Modifier.isPublic(writeMethod.getModifiers())) {
				LOGGER.warn("Write method for descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] is not public");
				continue;
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
			}


			if (estTransient || estCollection) { // on ignore les collections, elles seront chargées indépendemment
				continue;
			}
			if (!estColonne) {
				LOGGER.warn("No @Transient nor @Column annotation found on descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "]");
				continue;
			}

			final PropertyType propertyType;
			if (userType != null) {
				if (userType instanceof RegDateUserType) {
					propertyType = new RegDatePropertyType((RegDateUserType) userType);
				}
				else if (userType instanceof EnumUserType) {
					propertyType = new EnumUserTypePropertyType(returnType, (EnumUserType) userType);
				}
				else if (userType instanceof EnumTypeAdresseUserType) {
					propertyType = new EnumTypeAdressePropertyType((EnumTypeAdresseUserType) userType);
				}
				else {
					throw new NotImplementedException("Type de user-type inconnu = [" + userType.getClass().getName() + "]");
				}

			}
			else if (otherForeignKey) {
				propertyType = new JoinPropertyType(returnType);
			}
			else {
				propertyType = PropertyType.byJavaType.get(returnType);
				Assert.notNull(propertyType, "Type java non-enregistré [" + returnType.getName() + "] (propriété = [" + descriptor.getName() + "] de la classe [" + clazz.getSimpleName() + "])");
			}

			properties.add(new Property(descriptor.getName(), propertyType, columnName, false, primaryKey, parentForeignKey));
		}

		Collections.sort(properties);
		entity.setColumns(properties);
		return entity;
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
				if (readMethod.getGenericReturnType() instanceof ParameterizedType) { // le type de return est une collection parametrisée
					final ParameterizedType returnType = (ParameterizedType) readMethod.getGenericReturnType();
					for (java.lang.reflect.Type typeArgument : returnType.getActualTypeArguments()) {
						if (typeArgument instanceof Class) {
							final Class typeClass = (Class) typeArgument;
							if (typeClass.isAssignableFrom(other)) { // le type générique est celui voulu
								return true;
							}
						}
					}
				}
			}
		}

		return false;
	}
}
