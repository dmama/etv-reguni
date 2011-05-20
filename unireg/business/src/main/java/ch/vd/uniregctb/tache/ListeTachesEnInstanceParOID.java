package ch.vd.uniregctb.tache;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;

public class ListeTachesEnInstanceParOID extends TachesResults<Long, ListeTachesEnInstanceParOID> {

	/**
	 * Classe pour stocker les informations d'une ligne du fichier csv resulat
	 *
	 * @author XSIBNM
	 */

	private double nombreTacheMoyen;


	public void setNombreTacheMoyen(double nombreTacheMoyen) {
		this.nombreTacheMoyen = nombreTacheMoyen;
	}

	public double getNombreTacheMoyen() {
		return nombreTacheMoyen;
	}

	public class LigneTacheInstance {

		private long numeroOID;
		private String typeTache;
		private long nombreTache;

		public LigneTacheInstance(long numeroOID, String typeTache, long nombreTache) {
			super();
			this.numeroOID = numeroOID;
			this.typeTache = typeTache;
			this.nombreTache = nombreTache;


		}

		public long getNumeroOID() {
			return numeroOID;
		}

		public void setNumeroOID(long numeroOID) {
			this.numeroOID = numeroOID;
		}

		public String getTypeTache() {
			return typeTache;
		}

		public void setTypeTache(String typeTache) {
			this.typeTache = typeTache;
		}

		public long getNombreTache() {
			return nombreTache;
		}

		public void setNombreTache(long nombreTache) {
			this.nombreTache = nombreTache;
		}


	}


	private final List<LigneTacheInstance> tachesEnIsntancesParOID = new ArrayList<LigneTacheInstance>();


	public void addTypeDeTacheEnInstance(long numeroOID, String typeTache, long nombreTache) {
		tachesEnIsntancesParOID.add(
				new LigneTacheInstance(numeroOID, typeTache, nombreTache)
		);
	}


	public void addErrorException(Long numeroOID, Exception e) {
		tachesEnIsntancesParOID.add(
				new LigneTacheInstance(numeroOID, "ErreurType.EXCEPTION.description()", 0)
		);
	}


	public List<LigneTacheInstance> getLignes() {
		return tachesEnIsntancesParOID;
	}


	public ListeTachesEnInstanceParOID(RegDate dateTraitement) {
		super(dateTraitement);
	}

	public void addAll(ListeTachesEnInstanceParOID rapport) {
		tachesEnIsntancesParOID.addAll(rapport.tachesEnIsntancesParOID);
	}
}
