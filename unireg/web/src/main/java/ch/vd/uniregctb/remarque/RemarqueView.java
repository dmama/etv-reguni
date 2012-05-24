package ch.vd.uniregctb.remarque;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.tiers.Remarque;

@SuppressWarnings("UnusedDeclaration")
public class RemarqueView {

	private String date;
	private String user;
	private String text;

	public RemarqueView(Remarque remarque) {
		this.date = DateHelper.dateTimeToDisplayString(remarque.getLogModifDate());
		this.user = remarque.getLogModifUser();
		this.text = remarque.getTexte();
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
}
