package ch.vd.uniregctb.admin;

import org.displaytag.decorator.TableDecorator;

import ch.vd.uniregctb.admin.AuditLogBean.AuditView;

public class AuditTableDecorator extends TableDecorator {

	@Override
	public String addRowClass() {

		String clazz = "";

		AuditView line = (AuditView) getCurrentRowObject();
		switch (line.getLevel()) {
		case ERROR:
			clazz += " error";
			break;
		case WARN:
			clazz += " warn";
			break;
		case SUCCESS:
			clazz += " success";
			break;
		}

		return clazz;
	}
}
