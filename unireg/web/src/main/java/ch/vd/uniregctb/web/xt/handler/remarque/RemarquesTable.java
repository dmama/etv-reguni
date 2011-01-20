package ch.vd.uniregctb.web.xt.handler.remarque;

import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringEscapeUtils;
import org.springmodules.xt.ajax.component.Table;
import org.springmodules.xt.ajax.component.TableData;
import org.springmodules.xt.ajax.component.TableRow;

import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.web.xt.component.PreservedText;
import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Composant qui permet d'afficher les remarques d'un tiers sous forme tabulaire.
 */
public class RemarquesTable extends Table {

	private int count = 0;

	public RemarquesTable() {
		addAttribute("border", "0");
		addAttribute("cellspacing", "0");
		addAttribute("class", "remarques");
	}

	public void addRemarque(Remarque remarque) {

		TableRow row = new TableRow();
		row.addAttribute("class", count++ % 2 == 0 ? "even" : "odd");
		{
			final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy à HH:mm");
			final String t = String.format("Ajoutée le %s par %s", format.format(remarque.getLogModifDate()), remarque.getLogModifUser());
			final SimpleText entete = new SimpleText(StringEscapeUtils.escapeXml(t));

			TableData data = new TableData(entete);
			data.addAttribute("class", "entete");
			row.addTableData(data);

			data = new TableData(new PreservedText(remarque.getTexte()));
			data.addAttribute("class", "texte");
			row.addTableData(data);
		}
		addTableRow(row);
	}

	/**
	 * @return le nombre de remarques
	 */
	public int getCount() {
		return count;
	}
}
