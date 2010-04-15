package ch.vd.uniregctb.interfaces.model.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.infrastructure.model.EnumSigleUsageEmail;
import ch.vd.infrastructure.model.EnumTypeSupportEchangeInformation;
import ch.vd.infrastructure.model.TypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Region;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class MockCollectiviteAdministrative implements CollectiviteAdministrative {

	private static final long serialVersionUID = 4272590042497410216L;

	private static final List<MockCollectiviteAdministrative> all = new ArrayList<MockCollectiviteAdministrative>();

	public static MockCollectiviteAdministrative OTG = new MockCollectiviteAdministrative(ServiceInfrastructureService.noTuteurGeneral, null, "Office Tuteur général", null, null, "OTG");
	public static MockCollectiviteAdministrative CEDI = new MockCollectiviteAdministrative(ServiceInfrastructureService.noCEDI, new MockAdresse("", "", "1014", "Lausanne Adm cant"), "Centre d'enregistrement", "des déclarations d'impôt", null, "CEDI");
	public static MockCollectiviteAdministrative CAT = new MockCollectiviteAdministrative(ServiceInfrastructureService.noCAT, null, "Administration cantonale des impôts", null, null, "CAT");
	public static MockCollectiviteAdministrative ACI = new MockCollectiviteAdministrative(ServiceInfrastructureService.noACI, new MockAdresse("Route de Berne", "46", "1014", "Lausanne Adm cant"), "Administration cantonale des impôts", null, null, "ACI");

	/**
	 * Permet de forcer le chargement des Mock dans le DefaultMockService
	 * Il faut ajouter les nouveaux Mock dans cette methode
	 */
	@SuppressWarnings("unused")
	public static void forceLoad() {
		 MockCollectiviteAdministrative ca = OTG;
	}

	private Adresse adresse = null;
	private String adresseEmail = null;
	private List<Commune> communes = null;
	private RegDate dateFinValidite = null;
	private String noCCP = null;
	private long noColAdm;
	private String noFax = null;
	private String noTelephone = null;
	private String nomComplet1 = null;
	private String nomComplet2 = null;
	private String nomComplet3 = null;
	private String nomCourt = null;
	private Region regionRattachement = null;
	private String sigle = null;
	private String sigleCanton = null;
	private EnumTypeSupportEchangeInformation supportEchanAO = null;
	private TypeCollectivite type = null;
	private boolean isACI;
	private boolean isOID;
	private boolean isTiersTAO;
	private boolean isValide;

	public MockCollectiviteAdministrative() {
	}

	public MockCollectiviteAdministrative(long noColAdm, Adresse adresse, String nomComplet1, String nomComplet2, String nomComplet3, String nomCourt) {
		super();
		this.noColAdm = noColAdm;
		this.adresse = adresse;
		this.nomComplet1 = nomComplet1;
		this.nomComplet2 = nomComplet2;
		this.nomComplet3 = nomComplet3;
		this.nomCourt = nomCourt;
		all.add(this);
	}

	/**
	 * @return the adresse
	 */
	public Adresse getAdresse() {
		return adresse;
	}

	/**
	 * @param adresse
	 *            the adresse to set
	 */
	public void setAdresse(Adresse adresse) {
		this.adresse = adresse;
	}

	/**
	 * @return the adresseEmail
	 */
	public String getAdresseEmail() {
		return adresseEmail;
	}

	/**
	 * @param adresseEmail
	 *            the adresseEmail to set
	 */
	public void setAdresseEmail(String adresseEmail) {
		this.adresseEmail = adresseEmail;
	}

	/**
	 * @return the communes
	 */
	public List<Commune> getCommunes() {
		return communes;
	}

	/**
	 * @param communes
	 *            the communes to set
	 */
	public void setCommunes(List<Commune> communes) {
		this.communes = communes;
	}

	/**
	 * @return the dateFinValidite
	 */
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	/**
	 * @param dateFinValidite
	 *            the dateFinValidite to set
	 */
	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	/**
	 * @return the noCCP
	 */
	public String getNoCCP() {
		return noCCP;
	}

	/**
	 * @param noCCP
	 *            the noCCP to set
	 */
	public void setNoCCP(String noCCP) {
		this.noCCP = noCCP;
	}

	/**
	 * @return the noColAdm
	 */
	public int getNoColAdm() {
		return (int) noColAdm;
	}

	/**
	 * @param noColAdm
	 *            the noColAdm to set
	 */
	public void setNoColAdm(int noColAdm) {
		this.noColAdm = noColAdm;
	}

	/**
	 * @return the noFax
	 */
	public String getNoFax() {
		return noFax;
	}

	/**
	 * @param noFax
	 *            the noFax to set
	 */
	public void setNoFax(String noFax) {
		this.noFax = noFax;
	}

	/**
	 * @return the noTelephone
	 */
	public String getNoTelephone() {
		return noTelephone;
	}

	/**
	 * @param noTelephone
	 *            the noTelephone to set
	 */
	public void setNoTelephone(String noTelephone) {
		this.noTelephone = noTelephone;
	}

	/**
	 * @return the nomComplet1
	 */
	public String getNomComplet1() {
		return nomComplet1;
	}

	/**
	 * @param nomComplet1
	 *            the nomComplet1 to set
	 */
	public void setNomComplet1(String nomComplet1) {
		this.nomComplet1 = nomComplet1;
	}

	/**
	 * @return the nomComplet2
	 */
	public String getNomComplet2() {
		return nomComplet2;
	}

	/**
	 * @param nomComplet2
	 *            the nomComplet2 to set
	 */
	public void setNomComplet2(String nomComplet2) {
		this.nomComplet2 = nomComplet2;
	}

	/**
	 * @return the nomComplet3
	 */
	public String getNomComplet3() {
		return nomComplet3;
	}

	/**
	 * @param nomComplet3
	 *            the nomComplet3 to set
	 */
	public void setNomComplet3(String nomComplet3) {
		this.nomComplet3 = nomComplet3;
	}

	/**
	 * @return the nomCourt
	 */
	public String getNomCourt() {
		return nomCourt;
	}

	/**
	 * @param nomCourt
	 *            the nomCourt to set
	 */
	public void setNomCourt(String nomCourt) {
		this.nomCourt = nomCourt;
	}

	/**
	 * @return the regionRattachement
	 */
	public Region getRegionRattachement() {
		return regionRattachement;
	}

	/**
	 * @param regionRattachement
	 *            the regionRattachement to set
	 */
	public void setRegionRattachement(Region regionRattachement) {
		this.regionRattachement = regionRattachement;
	}

	/**
	 * @return the sigle
	 */
	public String getSigle() {
		return sigle;
	}

	/**
	 * @param sigle
	 *            the sigle to set
	 */
	public void setSigle(String sigle) {
		this.sigle = sigle;
	}

	/**
	 * @return the sigleCanton
	 */
	public String getSigleCanton() {
		return sigleCanton;
	}

	/**
	 * @param sigleCanton
	 *            the sigleCanton to set
	 */
	public void setSigleCanton(String sigleCanton) {
		this.sigleCanton = sigleCanton;
	}

	/**
	 * @return the supportEchanAO
	 */
	public EnumTypeSupportEchangeInformation getSupportEchanAO() {
		return supportEchanAO;
	}

	/**
	 * @param supportEchanAO
	 *            the supportEchanAO to set
	 */
	public void setSupportEchanAO(EnumTypeSupportEchangeInformation supportEchanAO) {
		this.supportEchanAO = supportEchanAO;
	}

	/**
	 * @return the type
	 */
	public TypeCollectivite getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(TypeCollectivite type) {
		this.type = type;
	}

	/**
	 * @return the isACI
	 */
	public boolean isACI() {
		return isACI;
	}

	/**
	 * @param isACI
	 *            the isACI to set
	 */
	public void setACI(boolean isACI) {
		this.isACI = isACI;
	}

	/**
	 * @return the isOID
	 */
	public boolean isOID() {
		return isOID;
	}

	/**
	 * @param isOID
	 *            the isOID to set
	 */
	public void setOID(boolean isOID) {
		this.isOID = isOID;
	}

	/**
	 * @return the isTiersTAO
	 */
	public boolean isTiersTAO() {
		return isTiersTAO;
	}

	/**
	 * @param isTiersTAO
	 *            the isTiersTAO to set
	 */
	public void setTiersTAO(boolean isTiersTAO) {
		this.isTiersTAO = isTiersTAO;
	}

	/**
	 * @return the isValide
	 */
	public boolean isValide() {
		return isValide;
	}

	/**
	 * @param isValide
	 *            the isValide to set
	 */
	public void setValide(boolean isValide) {
		this.isValide = isValide;
	}


	public String getAdresseEmail(EnumSigleUsageEmail arg0) {
		return null;
	}

	public EnumTypeSupportEchangeInformation getSupportEchangeTAO() {
		return null;
	}

	public static List<MockCollectiviteAdministrative> getAll() {
		return Collections.unmodifiableList(all);
	}
}
