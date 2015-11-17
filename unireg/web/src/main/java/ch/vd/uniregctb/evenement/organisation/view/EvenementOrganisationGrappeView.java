package ch.vd.uniregctb.evenement.organisation.view;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;

public class EvenementOrganisationGrappeView implements Serializable, Iterable<EvenementOrganisationBasicInfo> {

	private static final long serialVersionUID = -1635507422917611747L;

	private final List<EvenementOrganisationBasicInfo> grappe;

	public EvenementOrganisationGrappeView(List<EvenementOrganisationBasicInfo> grappe) {
		this.grappe = grappe;
	}

	@Override
	public Iterator<EvenementOrganisationBasicInfo> iterator() {
		return grappe.iterator();
	}

	@SuppressWarnings("UnusedDeclaration")
	public RegDate getEffectiveDate() {
		return grappe.get(grappe.size() - 1).getDate();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isMultiElement() {
		return grappe.size() > 1;
	}
}
