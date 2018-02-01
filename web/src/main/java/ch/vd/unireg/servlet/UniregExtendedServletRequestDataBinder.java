package ch.vd.unireg.servlet;

import java.beans.PropertyEditor;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

/**
 * Data binder spécialisé pour gérer correctement les checkboxes génériques dans SuperGra.
 */
public class UniregExtendedServletRequestDataBinder extends ExtendedServletRequestDataBinder {

	public UniregExtendedServletRequestDataBinder(Object target, String objectName) {
		super(target, objectName);
	}

	@Override
	protected void checkFieldMarkers(MutablePropertyValues mpvs) {
		// note : copié-collé de la méthode super.checkFieldMarkers() avec fix pour SuperGra (voir ci-dessous)
		if (getFieldMarkerPrefix() != null) {
			String fieldMarkerPrefix = getFieldMarkerPrefix();
			PropertyValue[] pvArray = mpvs.getPropertyValues();
			for (PropertyValue pv : pvArray) {
				if (pv.getName().startsWith(fieldMarkerPrefix)) {
					String field = pv.getName().substring(fieldMarkerPrefix.length());
					if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
						Class fieldType = getPropertyAccessor().getPropertyType(field);
						if (fieldType == Object.class) {
							// [UNIREG-2962] les AttributeView retournent un type 'Object' et pour que Spring reconnaisse correctement
							// les valeurs booléennes (problème des checkboxes), on force le type lorsque c'est nécessaire
							PropertyEditor editor = findCustomEditor(null, field);
							if (editor instanceof CustomBooleanEditor) {
								fieldType = Boolean.class;
							}
						}
						mpvs.add(field, getEmptyValue(field, fieldType));
					}
					mpvs.removePropertyValue(pv);
				}
			}
		}
	}
}
