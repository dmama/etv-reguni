package ch.vd.uniregctb.remarque;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.HtmlHelper;
import ch.vd.uniregctb.tiers.Remarque;

@SuppressWarnings("UnusedDeclaration")
public class RemarqueView {

	private final String date;
	private final String user;
	private final String text;
	private final String htmlText;

	public RemarqueView(Remarque remarque) {
		this.date = DateHelper.dateTimeToDisplayString(remarque.getLogModifDate());
		this.user = remarque.getLogModifUser();
		this.text = remarque.getTexte();
		this.htmlText = HtmlHelper.renderMultilines(this.text);
	}

	public String getDate() {
		return date;
	}

	public String getUser() {
		return user;
	}

	public String getText() {
		return text;
	}

	public String getHtmlText() {
		return htmlText;
	}
}
