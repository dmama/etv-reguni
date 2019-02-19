package ch.vd.unireg.interfaces.service.host;

import java.io.Serializable;

import ch.vd.securite.model.rest.Procedure;
import ch.vd.unireg.security.IfoSecProcedure;

public class IfoSecProcedureImpl implements IfoSecProcedure, Serializable {

	private static final long serialVersionUID = -7517333928144158983L;

	private String code;
	private String codeActivite;
	private String designation;
	private Integer numero;

	public IfoSecProcedureImpl() {
	}

	public IfoSecProcedureImpl(String code, String codeActivite, String designation, Integer numero) {
		this.code = code;
		this.codeActivite = codeActivite;
		this.designation = designation;
		this.numero = numero;
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
	public Integer getNumero() {
		return numero;
	}

	public void setNumero(Integer numero) {
		this.numero = numero;
	}

	public static IfoSecProcedure get(Procedure p) {
		if (p == null) {
			return null;
		}
		return new IfoSecProcedureImpl(p);
	}
}
