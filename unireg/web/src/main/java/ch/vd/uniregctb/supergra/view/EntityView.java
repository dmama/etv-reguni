package ch.vd.uniregctb.supergra.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.delta.AttributeUpdate;

/**
 * Le form-backing object du {@link ch.vd.uniregctb.supergra.SuperGraEntityController}.
 */
public class EntityView implements Cloneable {

	private EntityKey key;
	private ValidationResults validationResults;
	private List<AttributeView> attributes;
	private Map<String, AttributeView> attributesMap;

	public EntityView() {
	}

	public EntityView(EntityView right) {
		this.key = right.key;
		if (right.attributes != null) {
			// deep copy
			this.attributes = new ArrayList<AttributeView>(right.attributes.size());
			this.attributesMap = new HashMap<String, AttributeView>(right.attributes.size());
			for (AttributeView r : right.attributes) {
				final AttributeView l = new AttributeView(r);
				this.attributes.add(l);
				this.attributesMap.put(l.getName(), l);
			}
		}
		else {
			this.attributes = null;
			this.attributesMap = null;
		}
	}

	public void setKey(EntityKey key) {
		this.key = key;
	}

	public EntityKey getKey() {
		return key;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public ValidationResults getValidationResults() {
		return validationResults;
	}

	public void setValidationResults(ValidationResults validationResults) {
		this.validationResults = validationResults;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public Map<String, AttributeView> getAttributesMap() {
		return attributesMap;
	}

	public List<AttributeView> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<AttributeView> attributes) {
		this.attributes = attributes;
		this.attributesMap = new HashMap<String, AttributeView>(attributes.size());
		for (AttributeView a : attributes) {
			attributesMap.put(a.getName(), a);
		}
	}

	@SuppressWarnings({"CloneDoesntCallSuperClone"})
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new EntityView(this);
	}

	/**
	 * Calcul la liste des différences entre cette entité et celle passée en paramètre.
	 *
	 * @param right une autre entité
	 * @return une liste de patches qui correspondent aux différences constatées.
	 */
	public List<AttributeUpdate> delta(EntityView right) {

		final List<AttributeUpdate> deltas = new ArrayList<AttributeUpdate>();

		for (AttributeView rightAttribute : right.attributes) {
			final AttributeView leftAttribute = attributesMap.get(rightAttribute.getName());
			Assert.notNull(leftAttribute);
			Assert.isEqual(leftAttribute.getType(), rightAttribute.getType());

			final Object leftValue = leftAttribute.getValue();
			final Object rightValue = rightAttribute.getValue();
			if (leftValue == null && rightValue == null) {
				continue;
			}

			if (leftValue != null && rightValue != null && leftValue.equals(rightValue)) {
				continue;
			}

			deltas.add(new AttributeUpdate(key, leftAttribute.getName(), leftValue, rightValue));
		}

		return deltas;
	}

	public boolean isAnnule() {
		final AttributeView annulationDate = (attributesMap == null ? null : attributesMap.get("annulationDate"));
		return annulationDate != null && annulationDate.getValue() != null;
	}
}
