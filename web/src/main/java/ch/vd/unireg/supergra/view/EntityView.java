package ch.vd.unireg.supergra.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.supergra.EntityKey;
import ch.vd.unireg.supergra.delta.AttributeUpdate;

/**
 * Le form-backing object de la méthode {@link ch.vd.unireg.supergra.SuperGraController#showEntity(ch.vd.unireg.supergra.EntityType, long, org.springframework.ui.Model, javax.servlet.http.HttpServletRequest)}.
 */
public class EntityView implements Duplicable<EntityView> {

	private EntityKey key;
	private ValidationResults validationResults;
	private List<AttributeView> attributes;
	private Map<String, AttributeView> attributesMap;
	private boolean isMenageCommun;
	private boolean isPersonnePhysique;
	private boolean isCommunauteRF;
	private boolean readonly;

	public EntityView() {
	}

	private EntityView(EntityView right) {
		this.key = right.key;
		if (right.attributes != null) {
			// deep copy
			this.attributes = new ArrayList<>(right.attributes.size());
			this.attributesMap = new HashMap<>(right.attributes.size());
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
		this.readonly = right.readonly;
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
		this.attributesMap = new HashMap<>(attributes.size());
		for (AttributeView a : attributes) {
			attributesMap.put(a.getName(), a);
		}
	}

	public boolean isMenageCommun() {
		return isMenageCommun;
	}

	public void setMenageCommun(boolean menageCommun) {
		isMenageCommun = menageCommun;
	}

	public boolean isPersonnePhysique() {
		return isPersonnePhysique;
	}

	public void setPersonnePhysique(boolean personnePhysique) {
		isPersonnePhysique = personnePhysique;
	}

	public boolean isCommunauteRF() {
		return isCommunauteRF;
	}

	public void setCommunauteRF(boolean communauteRF) {
		isCommunauteRF = communauteRF;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	@Override
	public EntityView duplicate() {
		return new EntityView(this);
	}

	/**
	 * Calcul la liste des différences entre cette entité et celle passée en paramètre.
	 *
	 * @param right une autre entité
	 * @return une liste de patches qui correspondent aux différences constatées.
	 */
	public List<AttributeUpdate> delta(EntityView right) {

		if (readonly) {
			// une entité read-only ne peut pas générer de deltas, par définition
			return Collections.emptyList();
		}

		final List<AttributeUpdate> deltas = new ArrayList<>();

		for (AttributeView rightAttribute : right.attributes) {
			final AttributeView leftAttribute = attributesMap.get(rightAttribute.getName());
			if (leftAttribute == null) {
				throw new IllegalArgumentException();
			}
			if (leftAttribute.getType() != rightAttribute.getType()) {
				throw new IllegalArgumentException();
			}

			if (leftAttribute.isReadonly() || leftAttribute.isCollection()) {
				// les attributs read-only et les collections ne peuvent pas être mises-à-jour depuis l'écran d'édition d'une entité
				continue;
			}

			final Object leftValue = leftAttribute.getValue();
			final Object rightValue = rightAttribute.getValue();
			if (leftValue == null && rightValue == null) {
				continue;
			}

			if (leftValue != null && leftValue.equals(rightValue)) {
				continue;
			}

			if ((Boolean.FALSE.equals(leftValue) && rightValue == null) || (leftValue == null && Boolean.FALSE.equals(rightValue))) {
				// [UNIREG-2962] il n'est pas possible de distinguer entre une valeur booléenne nulle et false après l'avoir stockée dans une checkbox,
				// en conséquence pour éviter des fausses détections on ignore ces différences.
				continue;
			}

			if ((leftValue instanceof String && StringUtils.isBlank((String) leftValue) && rightValue == null) || (leftValue == null && rightValue instanceof String && StringUtils.isBlank((String) rightValue))) {
				// on ne peut pas non plus distinguer une chaîne nulle d'une chaîne vide dans un champ texte
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
