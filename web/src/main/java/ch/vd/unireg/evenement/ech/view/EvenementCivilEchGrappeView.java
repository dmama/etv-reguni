package ch.vd.uniregctb.evenement.ech.view;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;

public class EvenementCivilEchGrappeView implements Serializable, Iterable<EvenementCivilEchBasicInfo> {

	private static final long serialVersionUID = -1635507422917611747L;

	private final List<EvenementCivilEchBasicInfo> grappe;

	public EvenementCivilEchGrappeView(List<EvenementCivilEchBasicInfo> grappe) {
		this.grappe = grappe;
	}

	@NotNull
	@Override
	public Iterator<EvenementCivilEchBasicInfo> iterator() {
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
