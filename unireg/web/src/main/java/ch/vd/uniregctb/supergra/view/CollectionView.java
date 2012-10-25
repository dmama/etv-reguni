package ch.vd.uniregctb.supergra.view;

import java.util.List;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.EntityType;

/**
 * Le form-backing object du {@link ch.vd.uniregctb.supergra.SuperGraCollectionController}.
 */
public class CollectionView {

	/**
	 * La clé de l'entité parente
	 */
	private EntityKey key;

	/**
	 * Le nom de la propriété sur l'entité parente qui expose la collection affichée
	 */
	private String name;

	/**
	 * Le nom de l'attribut représentant la clé primaire sur les entités de la collection.
	 */
	private String primaryKeyAtt;

	/**
	 * Le type des entités de la collection.
	 */
	private EntityType primaryKeyType;

	/**
	 * La collection des entités à afficher
	 */
	private List<EntityView> entities;

	/**
	 * La liste des noms d'attributs à afficher
	 */
	private List<String> attributeNames;

	/**
	 * La liste des entités concrètes pouvant être ajoutées dans la collection
	 */
	private List<Class<? extends HibernateEntity>> concreteEntityClasses;

	public EntityKey getKey() {
		return key;
	}

	public void setKey(EntityKey key) {
		this.key = key;
	}

	/**
	 * @return le nom de la collection
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrimaryKeyAtt() {
		return primaryKeyAtt;
	}

	public void setPrimaryKeyAtt(String primaryKeyAtt) {
		this.primaryKeyAtt = primaryKeyAtt;
	}

	public EntityType getPrimaryKeyType() {
		return primaryKeyType;
	}

	public void setPrimaryKeyType(EntityType primaryKeyType) {
		this.primaryKeyType = primaryKeyType;
	}

	/**
	 * @return les entités présentes dans la collection
	 */
	public List<EntityView> getEntities() {
		return entities;
	}

	public void setEntities(List<EntityView> entities) {
		this.entities = entities;
	}

	/**
	 * @return les noms de l'ensemble des attributs définis sur toutes les entités
	 */
	public List<String> getAttributeNames() {
		return attributeNames;
	}

	public void setAttributeNames(List<String> attributeNames) {
		this.attributeNames = attributeNames;
	}

	public List<Class<? extends HibernateEntity>> getConcreteEntityClasses() {
		return concreteEntityClasses;
	}

	public void setConcreteEntityClasses(List<Class<? extends HibernateEntity>> concreteEntityClasses) {
		this.concreteEntityClasses = concreteEntityClasses;
	}
}