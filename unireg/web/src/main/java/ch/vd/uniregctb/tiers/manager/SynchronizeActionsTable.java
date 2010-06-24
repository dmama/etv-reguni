package ch.vd.uniregctb.tiers.manager;

import java.util.List;

import org.springmodules.xt.ajax.component.Table;
import org.springmodules.xt.ajax.component.TableData;
import org.springmodules.xt.ajax.component.TableRow;

import ch.vd.uniregctb.common.ExceptionUtils;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.web.xt.component.SimpleText;

public class SynchronizeActionsTable extends Table {

	public SynchronizeActionsTable(String titre) {
		addAttribute("border", "0");
//		addAttribute("cellpadding", "0");
		addAttribute("cellspacing", "0");
		addAttribute("class", "sync_actions");

		TableRow headerRow = new TableRow();
		{
			headerRow.addAttribute("class", "header");
			TableData data = new TableData(new SimpleText(titre));
			data.addAttribute("colspan", "2");
			headerRow.addTableData(data);
		}
		addTableRow(headerRow);
	}

	public void addException(Exception e) {

		TableRow row = new TableRow();
		{
			row.addAttribute("class", "exception");
			TableData data = new TableData(new SimpleText(ExceptionUtils.extractCallStack(e)));
			data.addAttribute("colspan", "2");
			row.addTableData(data);
		}
		addTableRow(row);
	}

	public void addAction(SynchronizeAction action) {

		TableRow row = new TableRow();
		{
			row.addAttribute("class", "action");
			TableData data = new TableData(new SimpleText("»"));
			data.addAttribute("class", "rowheader");
			row.addTableData(data);
			data = new TableData(new SimpleText(action.toString()));
			data.addAttribute("class", "action");
			row.addTableData(data);
		}
		addTableRow(row);
	}

	public void addActions(List<SynchronizeAction> actions) {
		for (SynchronizeAction action : actions) {
			addAction(action);
		}
	}

	public void addErrors(List<String> errors) {
		for (String error : errors) {
			addError(error);
		}
	}

	public void addError(String error) {

		TableRow row = new TableRow();
		{
			row.addAttribute("class", "action");
			TableData data = new TableData(new SimpleText("»"));
			data.addAttribute("class", "rowheader");
			row.addTableData(data);
			data = new TableData(new SimpleText(error));
			data.addAttribute("class", "error");
			row.addTableData(data);
		}
		addTableRow(row);
	}
}
