package ch.vd.unireg.interfaces.service.host;

import java.io.Serializable;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.wsclient.refsec.model.User;


public class Operateur implements Serializable, Comparable {

	private String nom;
	private String prenom;
	private String email;
	private long individuNoTechnique;
	private String code;


	public static Operateur get(ch.vd.securite.model.rest.Operateur o) {
		if (o == null) {
			return null;
		}

		final Operateur op = new Operateur();
		op.setNom(o.getNom());
		op.setPrenom(o.getPrenom());
		op.setEmail(o.getEmail());
		op.setIndividuNoTechnique(o.getIndividuNoTechnique());
		op.setCode(o.getCode());


		return op;
	}

	public static Operateur get(User user, String visa) {
		if (user == null) {
			return null;
		}

		final Operateur op = new Operateur();
		op.setNom(user.getLastName());
		op.setPrenom(user.getFirstName());
		op.setEmail(user.getEmail());
		op.setCode(visa);
		return op;
	}


	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Deprecated
	public Long getIndividuNoTechnique() {
		return individuNoTechnique;
	}

	@Deprecated
	public void setIndividuNoTechnique(long individuNoTechnique) {
		this.individuNoTechnique = individuNoTechnique;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public int compareTo(@NotNull Object o) {
		return ObjectUtils.compare(nom, ((Operateur) o).getNom(), false);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (!(o instanceof Operateur)) return false;

		final Operateur operateur = (Operateur) o;

		return new EqualsBuilder()
				.append(getNom(), operateur.getNom())
				.append(getPrenom(), operateur.getPrenom())
				.append(getCode(), operateur.getCode())
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(getNom())
				.append(getPrenom())
				.append(getCode())
				.toHashCode();
	}
}
