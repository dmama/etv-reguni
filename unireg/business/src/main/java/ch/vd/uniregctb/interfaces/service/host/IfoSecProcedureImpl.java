package ch.vd.uniregctb.interfaces.service.host;

import ch.vd.securite.model.Procedure;
import ch.vd.uniregctb.security.IfoSecProcedure;

public class IfoSecProcedureImpl implements IfoSecProcedure {

	private String code;
	private String codeActivite;
	private String designation;
	private int numero;

	public IfoSecProcedureImpl() {
	}

	public IfoSecProcedureImpl(Procedure p) {
		this.code = p.getCode();
		this.codeActivite = p.getCodeActivite();
		this.designation = p.getDesignation();
		this.numero = p.getNumero();
	}

	@Override
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String getCodeActivite() {
		return codeActivite;
	}

	public void setCodeActivite(String codeActivite) {
		this.codeActivite = codeActivite;
	}

	@Override
	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	@Override
	public int getNumero() {
		return numero;
	}

	public void setNumero(int numero) {
		this.numero = numero;
	}

	public static IfoSecProcedure get(Procedure p) {
		if (p==null) {
			return null;
		}
		return new IfoSecProcedureImpl(p);
	}
}
