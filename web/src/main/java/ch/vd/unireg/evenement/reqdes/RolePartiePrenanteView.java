package ch.vd.unireg.evenement.reqdes;

import java.io.Serializable;

import ch.vd.unireg.reqdes.ModeInscription;
import ch.vd.unireg.reqdes.RolePartiePrenante;
import ch.vd.unireg.reqdes.TransactionImmobiliere;
import ch.vd.unireg.reqdes.TypeInscription;
import ch.vd.unireg.reqdes.TypeRole;

public class RolePartiePrenanteView implements Serializable {

	private static final long serialVersionUID = 8847874198694677316L;

	private final ModeInscription modeInscription;
	private final TypeInscription typeInscription;
	private final String libelleInscription;
	private final int ofsCommune;
	private final TypeRole type;

	public RolePartiePrenanteView(RolePartiePrenante source) {
		final TransactionImmobiliere ti = source.getTransaction();
		this.modeInscription = ti.getModeInscription();
		this.typeInscription = ti.getTypeInscription();
		this.libelleInscription = ti.getDescription();
		this.ofsCommune = ti.getOfsCommune();
		this.type = source.getRole();
	}

	public ModeInscription getModeInscription() {
		return modeInscription;
	}

	public TypeInscription getTypeInscription() {
		return typeInscription;
	}

	public String getLibelleInscription() {
		return libelleInscription;
	}

	public int getOfsCommune() {
		return ofsCommune;
	}

	public TypeRole getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RolePartiePrenanteView that = (RolePartiePrenanteView) o;

		if (ofsCommune != that.ofsCommune) return false;
		if (libelleInscription != null ? !libelleInscription.equals(that.libelleInscription) : that.libelleInscription != null) return false;
		if (modeInscription != that.modeInscription) return false;
		if (type != that.type) return false;
		if (typeInscription != that.typeInscription) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = modeInscription != null ? modeInscription.hashCode() : 0;
		result = 31 * result + (typeInscription != null ? typeInscription.hashCode() : 0);
		result = 31 * result + (libelleInscription != null ? libelleInscription.hashCode() : 0);
		result = 31 * result + ofsCommune;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}
}
