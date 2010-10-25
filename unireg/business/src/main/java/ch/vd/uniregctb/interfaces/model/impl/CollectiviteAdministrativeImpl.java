package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.model.TypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;

public class CollectiviteAdministrativeImpl implements CollectiviteAdministrative, Serializable {

	private static final long serialVersionUID = 2060844289310860920L;
	
	private final Adresse adresse;
	private final RegDate dateFin;
	private final String adresseEmail;
	private final String noCCP;
	private final int noColAdm;
	private final String noFax;
	private final String noTelephone;
	private final String nomComplet1;
	private final String nomComplet2;
	private final String nomComplet3;
	private final String nomCourt;
	private final String sigle;
	private final String sigleCanton;
	private final TypeCollectivite type;
	private final boolean aci;
	private final boolean oid;
	private final boolean valide;

	public static CollectiviteAdministrativeImpl get(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		if (target == null) {
			return null;
		}
		if (EnumTypeCollectivite.SIGLE_CIR.equals(target.getType().getEnumTypeCollectivite())) {
			return new OfficeImpotImpl(target);
		}
		else {
			return new CollectiviteAdministrativeImpl(target);
		}
	}

	protected CollectiviteAdministrativeImpl(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		this.adresse = AdresseImpl.get(target.getAdresse());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.adresseEmail = target.getAdresseEmail();
		this.noCCP = target.getNoCCP();
		this.noColAdm = target.getNoColAdm();
		this.noFax = target.getNoFax();
		this.noTelephone = target.getNoTelephone();
		this.nomComplet1 = target.getNomComplet1();
		this.nomComplet2 = target.getNomComplet2();
		this.nomComplet3 = target.getNomComplet3();
		this.nomCourt = target.getNomCourt();
		this.sigle = target.getSigle();
		this.sigleCanton = target.getSigleCanton();
		this.type = target.getType();
		this.aci = target.isACI();
		this.oid = target.isOID();
		this.valide = target.isValide();
	}

	public Adresse getAdresse() {
		return adresse;
	}

	public String getAdresseEmail() {
		return adresseEmail;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getNoCCP() {
		return noCCP;
	}

	public int getNoColAdm() {
		return noColAdm;
	}

	public String getNoFax() {
		return noFax;
	}

	public String getNoTelephone() {
		return noTelephone;
	}

	public String getNomComplet1() {
		return nomComplet1;
	}

	public String getNomComplet2() {
		return nomComplet2;
	}

	public String getNomComplet3() {
		return nomComplet3;
	}

	public String getNomCourt() {
		return nomCourt;
	}

	public String getSigle() {
		return sigle;
	}

	public String getSigleCanton() {
		return sigleCanton;
	}

	public TypeCollectivite getType() {
		return type;
	}

	public boolean isACI() {
		return aci;
	}

	public boolean isOID() {
		return oid;
	}

	public boolean isValide() {
		return valide;
	}

}
