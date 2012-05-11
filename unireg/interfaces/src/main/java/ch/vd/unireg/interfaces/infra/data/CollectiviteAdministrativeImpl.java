package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;

public class CollectiviteAdministrativeImpl implements CollectiviteAdministrative, Serializable {

	private static final long serialVersionUID = -8466081857429254813L;
	
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
		this.aci = target.isACI();
		this.oid = target.isOID();
		this.valide = target.isValide();
	}

	@Override
	public Adresse getAdresse() {
		return adresse;
	}

	@Override
	public String getAdresseEmail() {
		return adresseEmail;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public String getNoCCP() {
		return noCCP;
	}

	@Override
	public int getNoColAdm() {
		return noColAdm;
	}

	@Override
	public String getNoFax() {
		return noFax;
	}

	@Override
	public String getNoTelephone() {
		return noTelephone;
	}

	@Override
	public String getNomComplet1() {
		return nomComplet1;
	}

	@Override
	public String getNomComplet2() {
		return nomComplet2;
	}

	@Override
	public String getNomComplet3() {
		return nomComplet3;
	}

	@Override
	public String getNomCourt() {
		return nomCourt;
	}

	@Override
	public String getSigle() {
		return sigle;
	}

	@Override
	public String getSigleCanton() {
		return sigleCanton;
	}

	@Override
	public boolean isACI() {
		return aci;
	}

	@Override
	public boolean isOID() {
		return oid;
	}

	@Override
	public boolean isValide() {
		return valide;
	}

}
