package ch.vd.unireg.supergra.view;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.supergra.EntityKey;
import ch.vd.unireg.supergra.EntityType;

/**
 * Vue d'un attribut d'une entité Hibernate édité dans le mode SuperGra.
 */
public class AttributeView {

	/**
	 * L'id HTML du champ d'édition (optionel)
	 */
	private String id;

	private final String name;
	private final String displayName;
	private Object value;
	private final Class<?> type;
	/**
	 * Type d'entité si le {@link #type} est {@link EntityKey}.
	 */
	private final EntityType entityType;
	private final Object category;
	private boolean entityForeignKey;
	private final boolean collection;
	private final boolean readonly;

	public AttributeView(String name, Class<?> type, Object value, boolean entityForeignKey, boolean isCollection, boolean isReadonly) {
		this.name = name;
		this.displayName = name;
		this.value = resolveValue(value);
		this.type = resolveType(type, entityForeignKey);
		this.entityType = resolveEntityType(type);
		this.category = this.entityType;
		this.entityForeignKey = entityForeignKey;
		this.collection = isCollection;
		this.readonly = isReadonly;
	}

	public AttributeView(String name, String displayName, Class<?> type, Object value, boolean entityForeignKey, boolean isCollection, boolean isReadonly) {
		this.name = name;
		this.displayName = displayName;
		this.value = resolveValue(value);
		this.type = resolveType(type, entityForeignKey);
		this.entityType = resolveEntityType(type);
		this.category = this.entityType;
		this.collection = isCollection;
		this.readonly = isReadonly;
	}

	public AttributeView(String id, String name, String displayName, Class<?> type, Object value, Object category, boolean readonly) {
		this.id = id;
		this.name = name;
		this.displayName = displayName;
		this.value = resolveValue(value);
		this.entityType = resolveEntityType(type);
		this.type = type;
		this.category = category;
		this.entityForeignKey = false;
		this.collection = false;
		this.readonly = readonly;
	}

	public AttributeView(AttributeView right) {
		this.name = right.name;
		this.displayName = right.displayName;
		this.value = right.value;
		this.type = right.type;
		this.entityType = right.entityType;
		this.category = right.category;
		this.entityForeignKey = right.entityForeignKey;
		this.collection = right.collection;
		this.readonly = right.readonly;
	}

	/**
	 * Cette méthode traduit le type réel de la valeur stockée en base en un type logique utilisé pour la présentation des données.
	 *
	 * @param type             le type réel de l'attribut
	 * @param entityForeignKey vrai si l'attribut est la foreign key de l'entité parente
	 * @return la classe logique de la valeur présentée à l'utilisateur
	 */
	private Class<?> resolveType(Class<?> type, boolean entityForeignKey) {
		final Class<?> t;
		if (entityForeignKey || HibernateEntity.class.isAssignableFrom(type)) {
			t = EntityKey.class; // dans la cas d'un lien vers une autre entité, on utilise une EntityKey.
		}
		else {
			t = type;
		}
		return t;
	}

	/**
	 * Cette méthode détermine le type d'entité hibernate correspondant au type réel spécifié.
	 *
	 * @param javaType le type réel de l'attribut
	 * @return le type d'entité hibernate de la valeur présentée à l'utilisateur
	 */
	private EntityType resolveEntityType(Class<?> javaType) {
		final EntityType t;
		if (HibernateEntity.class.isAssignableFrom(javaType)) {
			t = EntityType.fromHibernateClass(javaType);
		}
		else {
			t = null;
		}
		return t;
	}

	/**
	 * Cette méthode traduit les entités hibernate en entity keys et laisse les autres valeurs comme telles.
	 *
	 * @param value une valeur, qui peut être nulle ou non, et de n'importe quel type
	 * @return la value passée en entrée, sauf dans le cas d'entités hibernate qui sont traduites en entity keys.
	 */
	private static Object resolveValue(Object value) {
		final Object v;
		if (value == null) {
			v = value;
		}
		else {
			if (value instanceof HibernateEntity) {
				final HibernateEntity maybeProxy = (HibernateEntity) value;

				// on s'assure de ne pas retourner un proxy
				final Mutable<HibernateEntity> ref = new MutableObject<>();
				maybeProxy.tellMeAboutYou(ref);
				final HibernateEntity hibernateEntity = ref.getValue();

				// on retourne la clé de l'entité
				v = new EntityKey(EntityType.fromHibernateClass(hibernateEntity.getClass()), (Long) hibernateEntity.getKey());
			}
			else {
				v = value;
			}
		}
		return v;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Class<?> getType() {
		return type;
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public Object getCategory() {
		return category;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public boolean isEntityForeignKey() {
		return entityForeignKey;
	}

	public boolean isCollection() {
		return collection;
	}

	public boolean isEntity() {
		return type == EntityKey.class;
	}

	public boolean isReadonly() {
		return readonly;
	}
}
