package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.xml.XmlUtils;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClient;

public class CollectiviteAdministrativeImpl implements CollectiviteAdministrativeUtilisateur, Serializable {

	private static final long serialVersionUID = -4299360772030801769L;

	public static final String SIGLE_CIR = "CIR";
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
	private final boolean parDefaut;

	private static final String IS_ACTIVE = "O";

	public static CollectiviteAdministrativeImpl get(ch.vd.infrastructure.model.rest.CollectiviteAdministrative target, ServiceInfrastructureClient client) {
		if (target == null) {
			return null;
		}
		if (SIGLE_CIR.equals(target.getType().getCodeTypeCollectivite())) {
			return new OfficeImpotImpl(target, client);
		}
		else {
			return new CollectiviteAdministrativeImpl(target, client);
		}

	}

	public static CollectiviteAdministrativeImpl get(ch.vd.infrastructure.model.rest.CollectiviteAdministrative target) {
		return get(target,null);
	}

	protected CollectiviteAdministrativeImpl(ch.vd.infrastructure.model.rest.CollectiviteAdministrative target, ServiceInfrastructureClient client) {
		this.adresse = AdresseImpl.get(target.getAdresse(), client);
		this.dateFin = XmlUtils.cal2regdate(target.getDateFinValidite());
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
		this.parDefaut = IS_ACTIVE.equals(target.getCodeActivite());
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

	@Override
	public boolean isCollectiviteParDefaut() {
		return parDefaut;
	}

	@Override
	public String toString() {
		return String.format("CollectiviteAdministrativeImpl{nomCourt='%s', noColAdm=%d, aci=%b, oid=%b, valide=%b, parDefaut=%b}", nomCourt, noColAdm, aci, oid, valide, parDefaut);
	}

}
