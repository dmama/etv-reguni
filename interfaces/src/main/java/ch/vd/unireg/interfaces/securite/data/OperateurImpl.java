package ch.vd.unireg.interfaces.securite.data;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.wsclient.refsec.model.User;


public class OperateurImpl implements Serializable, Comparable, Operateur {

	private String nom;
	private String prenom;
	private String email;
	private String code;


	public static OperateurImpl get(ch.vd.securite.model.rest.Operateur o) {
		if (o == null) {
			return null;
		}

		final OperateurImpl op = new OperateurImpl();
		op.setNom(o.getNom());
		op.setPrenom(o.getPrenom());
		op.setEmail(o.getEmail());
		op.setCode(o.getCode());
		return op;
	}

	public static Operateur get(User user) {
		if (user == null) {
			return null;
		}

		final OperateurImpl op = new OperateurImpl();
		op.setNom(user.getLastName());
		op.setPrenom(user.getFirstName());
		op.setEmail(user.getEmail());
		op.setCode(user.getVisa());
		return op;
	}


	@Override
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Override
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Override
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public int compareTo(@NotNull Object o) {
		return ObjectUtils.compare(nom, ((Operateur) o).getNom(), true);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OperateurImpl)) return false;
		final OperateurImpl operateur = (OperateurImpl) o;
		return Objects.equals(nom, operateur.nom) &&
				Objects.equals(prenom, operateur.prenom) &&
				Objects.equals(email, operateur.email) &&
				Objects.equals(code, operateur.code);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nom, prenom, email, code);
	}
}
