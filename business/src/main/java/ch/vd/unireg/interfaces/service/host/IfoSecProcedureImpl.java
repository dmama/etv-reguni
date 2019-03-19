package ch.vd.unireg.interfaces.service.host;

import java.io.Serializable;

import ch.vd.securite.model.rest.Procedure;
import ch.vd.unireg.security.IfoSecProcedure;

public class IfoSecProcedureImpl implements IfoSecProcedure, Serializable {

	private static final long serialVersionUID = -6351212176447760801L;

	private final String code;
	private final String designation;

	public IfoSecProcedureImpl(String code, String designation) {
		this.code = code;
		this.designation = designation;
	}

	public IfoSecProcedureImpl(Procedure p) {
		this.code = p.getCode();
		this.designation = p.getDesignation();
	}

	public IfoSecProcedureImpl(ch.vd.unireg.wsclient.refsec.model.Procedure procedure) {
		this.code = procedure.getCode();
		this.designation = procedure.getDescription();
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getDesignation() {
		return designation;
	}

	public static IfoSecProcedure get(Procedure p) {
		if (p == null) {
			return null;
		}
		return new IfoSecProcedureImpl(p);
	}
}
