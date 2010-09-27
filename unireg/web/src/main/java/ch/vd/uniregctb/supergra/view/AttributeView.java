package ch.vd.uniregctb.supergra.view;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.RefParam;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.EntityType;

/**
 * Vue d'un attribut d'une entité Hibernate édité dans le mode SuperGra.
 */
public class AttributeView {

	private String name;
	private String displayName;
	private Object value;
	private Class<?> type;
	private boolean parentForeignKey;
	private boolean collection;
	private boolean readonly;

	public AttributeView(String name, Class<?> type, Object value, boolean isParentForeignKey, boolean isCollection, boolean isReadonly) {
		this.name = name;
		this.displayName = name;
		this.value = resolveValue(value);
		this.type = resolveType(type, value, isParentForeignKey);
		this.parentForeignKey = isParentForeignKey;
		this.collection = isCollection;
		this.readonly = isReadonly;
	}

	public AttributeView(String name, String displayName, Class<?> type, Object value, boolean isParentForeignKey, boolean isCollection, boolean isReadonly) {
		this.name = name;
		this.displayName = displayName;
		this.value = resolveValue(value);
		this.type = resolveType(type, value, isParentForeignKey);
		this.collection = isCollection;
		this.readonly = isReadonly;
	}

	public AttributeView(AttributeView right) {
		this.name = right.name;
		this.displayName = right.displayName;
		this.value = right.value;
		this.type = right.type;
		this.parentForeignKey = right.parentForeignKey;
		this.collection = right.collection;
		this.readonly = right.readonly;
	}

	/**
	 * Cette méthode traduit le type réel de la valeur stockée en base en un type logique utilisé pour la présentation des données.
	 *
	 * @param type             le type réel de l'attribut
	 * @param value            la valeur réelle de l'attribut
	 * @param parentForeignKey vrai si l'attribut est la foreign key de l'entité parente
	 * @return la classe logique de la valeur présentée à l'utilisateur
	 */
	private Class<?> resolveType(Class<?> type, Object value, boolean parentForeignKey) {
		final Class<?> t;
		if (parentForeignKey || HibernateEntity.class.isAssignableFrom(type)) {
			t = EntityKey.class; // dans la cas d'un lien vers une autre entité, on utilise une EntityKey.
		}
		else {
			t = type;
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
				final RefParam<HibernateEntity> ref = new RefParam<HibernateEntity>();
				maybeProxy.tellMeAboutYou(ref);
				final HibernateEntity hibernateEntity = ref.ref;

				// on retourne la clé de l'entité
				v = new EntityKey(EntityType.fromHibernateClass(hibernateEntity.getClass()), (Long) hibernateEntity.getKey());
			}
			else {
				v = value;
			}
		}
		return v;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public boolean isParentForeignKey() {
		return parentForeignKey;
	}

	public boolean isCollection() {
		return collection;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
}
