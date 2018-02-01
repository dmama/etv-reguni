package ch.vd.unireg.admin;

import org.displaytag.decorator.TableDecorator;

import ch.vd.unireg.admin.AuditLogBean.AuditView;

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
