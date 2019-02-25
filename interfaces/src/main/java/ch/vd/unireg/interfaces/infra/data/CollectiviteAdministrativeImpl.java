package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.xml.XmlUtils;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.fidor.ServiceInfrastructureFidor;

public class CollectiviteAdministrativeImpl implements CollectiviteAdministrative, Serializable {

	private static final long serialVersionUID = -4299360772030801769L;

	public static final String SIGLE_OID = "CIR";
	public static final String SIGLE_ACI = "ACI";

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
	private final List<EchangeAciCom> echangeAciCom;

	public static CollectiviteAdministrativeImpl get(ch.vd.infrastructure.model.rest.CollectiviteAdministrative target) {
		if (target == null) {
			return null;
		}
		if (SIGLE_OID.equals(target.getType().getCodeTypeCollectivite())) {
			return new OfficeImpotImpl(target);
		}
		else {
			return new CollectiviteAdministrativeImpl(target);
		}
	}

	@Nullable
	public static CollectiviteAdministrative get(@Nullable ch.vd.fidor.xml.colladm.v1.CollectiviteAdministrative right, @NotNull ServiceInfrastructureFidor service) {
		if (right == null) {
			return null;
		}
		if (SIGLE_OID.equals(right.getCodeType())) {
			return new OfficeImpotImpl(right, service);
		}
		else {
			return new CollectiviteAdministrativeImpl(right, service);
		}
	}

	protected CollectiviteAdministrativeImpl(ch.vd.infrastructure.model.rest.CollectiviteAdministrative target) {
		this.adresse = AdresseImpl.get(target.getAdresse());
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
		echangeAciCom = null;
	}

	protected CollectiviteAdministrativeImpl(@NotNull ch.vd.fidor.xml.colladm.v1.CollectiviteAdministrative right, @NotNull ServiceInfrastructureFidor service) {
		this.adresse = AdresseImpl.getAt(right.getAdresses(), null, service);
		this.dateFin = XmlUtils.cal2regdate(right.getDateFin());
		this.adresseEmail = right.getEmail();
		this.noCCP = null;
		this.noColAdm = right.getId();
		this.noFax = null;
		this.noTelephone = right.getNoTelephone();
		this.nomComplet1 = right.getNomComplet();   // TODO (msi) vérifier les utilisations du nom complet (longueur !)
		this.nomComplet2 = null;
		this.nomComplet3 = null;
		this.nomCourt = right.getNomCourt();
		this.sigle = null;
		this.sigleCanton = right.getCanton();
		this.aci = Objects.equals(right.getCodeType(), SIGLE_ACI);
		this.oid = Objects.equals(right.getCodeType(), SIGLE_OID);
		this.valide = right.getDateFin() == null;
		this.echangeAciCom = EchangeAciComImpl.get(right.getEchangesAciCom());
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
	public List<EchangeAciCom> getEchangeAciCom() {
		return echangeAciCom;
	}

	@Override
	public String toString() {
		return String.format("CollectiviteAdministrativeImpl{nomCourt='%s', noColAdm=%d, aci=%b, oid=%b, valide=%b}", nomCourt, noColAdm, aci, oid, valide);
	}

}
