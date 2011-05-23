package ch.vd.uniregctb.tiers.picker;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springmodules.xt.ajax.component.Anchor;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.Container;
import org.springmodules.xt.ajax.component.Table;
import org.springmodules.xt.ajax.component.TableData;
import org.springmodules.xt.ajax.component.TableHeader;
import org.springmodules.xt.ajax.component.TableRow;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.web.xt.component.SimpleText;
import ch.vd.uniregctb.web.xt.handler.BreakLineComponent;

public class TiersPickerResultsTable extends Table {
	private final String buttonId;

	public TiersPickerResultsTable(List<TiersIndexedData> list, String buttonId) {
		this.buttonId = buttonId;
		addAttribute("border", "0");
		addAttribute("cellspacing", "0");

		TableHeader headerRow = new TableHeader(new String[]{"Numéro", "Nom / Raison sociale", "Date de naissance", "Domicile", "For principal"});
		headerRow.addAttribute("class", "header");
		setTableHeader(headerRow);

		for (int i = 0, listSize = list.size(); i < listSize; i++) {
			final TiersIndexedData data = list.get(i);
			TableRow row = new TableRow();
			row.addAttribute("class", i % 2 != 0 ? "even" : "odd");
			row.addTableData(new TableData(getNumeroContribuable(data)));
			row.addTableData(new TableData(getNomRaisonSocialeComponent(data)));
			row.addTableData(new TableData(new SimpleText(getDateNaissance(data))));
			row.addTableData(new TableData(new SimpleText(data.getNpa() + " " + data.getLocaliteOuPays())));
			row.addTableData(new TableData(new SimpleText(data.getForPrincipal())));
			addTableRow(row);
		}
	}

	private Component getNumeroContribuable(TiersIndexedData data) {
		final Anchor anchor = new Anchor("#", new SimpleText(FormatNumeroHelper.numeroCTBToDisplay(data.getNumero())));
		anchor.addAttribute("onclick", "document.getElementById('" + buttonId + "').select_tiers_id(this); return false;");
		return anchor;
	}

	private Component getNomRaisonSocialeComponent(TiersIndexedData data) {
		final Component component;
		if (StringUtils.isBlank(data.getNom2())) {
			component = new SimpleText(data.getNom1());
		}
		else {
			Container c = new Container(Container.Type.SPAN);
			c.addComponent(new SimpleText(data.getNom1()));
			c.addComponent(new BreakLineComponent());
			c.addComponent(new SimpleText(data.getNom2()));
			component = c;
		}
		return component;
	}

	private String getDateNaissance(TiersIndexedData data) {
		final RegDate dateNaissance = data.getRegDateNaissance();
		if (dateNaissance == null) {
			return null;
		}
		else {
			return RegDateHelper.StringFormat.DISPLAY.toString(dateNaissance);
		}
	}
}
