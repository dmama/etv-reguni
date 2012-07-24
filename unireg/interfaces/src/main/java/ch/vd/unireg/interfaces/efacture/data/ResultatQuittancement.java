package ch.vd.unireg.interfaces.efacture.data;

import org.jetbrains.annotations.Nullable;

public class ResultatQuittancement {

	private static final ResultatQuittancement DEJA_INSCRIT = new ResultatQuittancement(TypeResultatQuittancement.DEJA_INSCRIT);
	private static final ResultatQuittancement CONTRIBUABLE_INEXISTANT = new ResultatQuittancement(TypeResultatQuittancement.CONTRIBUABLE_INEXISTANT);
	private static final ResultatQuittancement ETAT_EFACTURE_INCOHERENT  = new ResultatQuittancement(TypeResultatQuittancement.ETAT_EFACTURE_INCOHERENT);
	private static final ResultatQuittancement ETAT_FISCAL_INCOHERENT = new ResultatQuittancement(TypeResultatQuittancement.ETAT_FISCAL_INCOHERENT);
	private static final ResultatQuittancement AUCUNE_DEMANDE_EN_ATTENTE_SIGNATURE = new ResultatQuittancement(TypeResultatQuittancement.AUCUNE_DEMANDE_EN_ATTENTE_SIGNATURE);

	String businessId;
	TypeResultatQuittancement typeResultat;

	private ResultatQuittancement(TypeResultatQuittancement typeResultat) {
		this(null, typeResultat);
	}

	public static ResultatQuittancement enCours(String businessId) {
		return new ResultatQuittancement(businessId, TypeResultatQuittancement.QUITTANCEMENT_EN_COURS);
	}

	public static ResultatQuittancement ok(ResultatQuittancement rqEnCours) {
		if (rqEnCours.getType() != TypeResultatQuittancement.QUITTANCEMENT_EN_COURS) {
			throw new IllegalArgumentException("rq doit être du type " + TypeResultatQuittancement.QUITTANCEMENT_EN_COURS);
		}
		return new ResultatQuittancement(rqEnCours.businessId, TypeResultatQuittancement.QUITTANCEMENT_OK);
	}

	public static ResultatQuittancement dejaInscrit() {
		return DEJA_INSCRIT;
	}

	public static ResultatQuittancement contribuableInexistant() {
		return CONTRIBUABLE_INEXISTANT;
	}

	public static ResultatQuittancement etatEfactureIncoherent() {
		return ETAT_EFACTURE_INCOHERENT;
	}

	public static ResultatQuittancement etatFiscalIncoherent() {
		return ETAT_FISCAL_INCOHERENT;
	}

	public static ResultatQuittancement aucuneDemandeEnAttenteDeSignature() {
		return AUCUNE_DEMANDE_EN_ATTENTE_SIGNATURE;
	}

	private ResultatQuittancement(@Nullable String businessId, TypeResultatQuittancement typeResultat) {
		this.businessId = businessId;
		this.typeResultat = typeResultat;
	}

	public String getBusinessId() {
		return businessId;
	}

	public TypeResultatQuittancement getType() {
		return typeResultat;
	}

	public String getDescription(long ctbId) {
		return typeResultat.getDescription(ctbId);
	}

	public boolean isOk() {
		return typeResultat.isOk();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final ResultatQuittancement that = (ResultatQuittancement) o;

		if (businessId != null ? !businessId.equals(that.businessId) : that.businessId != null){
			return false;
		}
		//noinspection RedundantIfStatement
		if (typeResultat != that.typeResultat) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = businessId != null ? businessId.hashCode() : 0;
		result = 31 * result + typeResultat.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ResultatQuittancement{" +
				"businessId='" + businessId + '\'' +
				", typeResultat=" + typeResultat +
				'}';
	}
}
