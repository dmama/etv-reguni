package ch.vd.uniregctb.interfaces.model.mock;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.vd.infrastructure.model.EnumTypeSupportEchangeInformation;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.type.TexteCasePostale;

public class MockCollectiviteAdministrative implements CollectiviteAdministrative {

	private static final Map<Long, MockCollectiviteAdministrative> all = new HashMap<Long, MockCollectiviteAdministrative>();
	//Mapping oid District
	public static final Map<Integer, Integer> districts = initDistrict();
	//Mapping oid Region
	public static final Map<Integer, Integer> regions = initRegions();


	public static final MockCollectiviteAdministrative OTG =
			new MockCollectiviteAdministrative(ServiceInfrastructureService.noTuteurGeneral, new MockAdresse("Chemin de Mornex", "32", "1014", "Lausanne"), "Office Tuteur général", null, null, "OTG");
	public static final MockCollectiviteAdministrative CEDI =
			new MockCollectiviteAdministrative(ServiceInfrastructureService.noCEDI, new MockAdresse("", "", "1014", "Lausanne Adm cant"), "Centre d'enregistrement", "des déclarations d'impôt", null,
					"CEDI");
	public static final MockCollectiviteAdministrative CAT =
			new MockCollectiviteAdministrative(ServiceInfrastructureService.noCAT, null, "Administration cantonale des impôts", null, null, "CAT", "0213160000", "0213162140");
	public static final MockCollectiviteAdministrative ACI =
			new MockCollectiviteAdministrative(ServiceInfrastructureService.noACI, new MockAdresse("Route de Berne 46", "46", "1014", "Lausanne Adm cant"), "Administration cantonale des impôts", null,
					null, "ACI");
	public static final MockCollectiviteAdministrative ACISUCCESSIONS =
			new MockCollectiviteAdministrative(ServiceInfrastructureService.noACISuccessions, new MockAdresse("Route de Berne", "46", "1014", "Lausanne Adm cant"),
					"Administration cantonale des impôts SUCCESSIONS", "SUCCESSIONS", null, "ACI-SUCCESSIONS");
	public static final MockCollectiviteAdministrative ACIIMPOTSOURCE =
			new MockCollectiviteAdministrative(ServiceInfrastructureService.noACIImpotSource, new MockAdresse("Rue Caroline 9bis", "9bis", "1014", "Lausanne Adm cant"),
					"Administration cantonale des impôts", "IMPOT A LA SOURCE", null, "ACI-IMPOT-SOURCE", "0213162065", "0213162898");

	public static final class JusticePaix {
		public static MockCollectiviteAdministrative DistrictsJuraNordVaudoisEtGrosDeVaud =
				new MockCollectiviteAdministrative(970, new MockAdresse("Rue du Pré", "2", new CasePostale(TexteCasePostale.CASE_POSTALE, 693), "1400", "Yverdon-les-Bains"),
						"Justice de Paix des districts du",
						"Jura-Nord Vaudois et du Gros-de-Vaud", null, "JUSPX");
	}

	private Adresse adresse = null;
	private String adresseEmail = null;
	private RegDate dateFinValidite = null;
	private String noCCP = null;
	private long noColAdm;
	private String noFax = null;
	private String noTelephone = null;
	private String nomComplet1 = null;
	private String nomComplet2 = null;
	private String nomComplet3 = null;
	private String nomCourt = null;
	private String sigle = null;
	private String sigleCanton = null;
	private EnumTypeSupportEchangeInformation supportEchanAO = null;
	private boolean isACI;
	private boolean isOID;
	private boolean isValide;

	/**
	 * Crée une collectivité administrative <b>sans</b> l'enregistrer dans le mock par défaut de l'infrastructure
	 */
	public MockCollectiviteAdministrative() {
	}

	/**
	 * Crée une collectivité administrative <b>sans</b> l'enregistrer dans le mock par défaut de l'infrastructure
	 *
	 * @param noColAdm le numéro de la collectivité
	 * @param nom      le nom de la collectivité
	 */
	public MockCollectiviteAdministrative(long noColAdm, String nom) {
		this.noColAdm = noColAdm;
		this.adresse = null;
		this.nomComplet1 = nom;
		this.nomComplet2 = null;
		this.nomComplet3 = null;
		this.nomCourt = nom;
	}

	/**
	 * Crée une nouvelle collectivité administrative qui sera enregistrée automatiquement dans le mock par défaut du service infrastructure.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected MockCollectiviteAdministrative(long noColAdm, Adresse adresse, String nomComplet1, String nomComplet2, String nomComplet3, String nomCourt, String noTelephone, String noFax) {
		this.noColAdm = noColAdm;
		this.adresse = adresse;
		this.nomComplet1 = nomComplet1;
		this.nomComplet2 = nomComplet2;
		this.nomComplet3 = nomComplet3;
		this.nomCourt = nomCourt;
		this.noTelephone = noTelephone;
		this.noFax = noFax;

		DefaultMockServiceInfrastructureService.addColAdm(this);
		addToAll(noColAdm, this);
	}

	/**
	 * Crée une nouvelle collectivité administrative qui sera enregistrée automatiquement dans le mock par défaut du service infrastructure.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected MockCollectiviteAdministrative(long noColAdm, Adresse adresse, String nomComplet1, String nomComplet2, String nomComplet3, String nomCourt) {
		this.noColAdm = noColAdm;
		this.adresse = adresse;
		this.nomComplet1 = nomComplet1;
		this.nomComplet2 = nomComplet2;
		this.nomComplet3 = nomComplet3;
		this.nomCourt = nomCourt;

		DefaultMockServiceInfrastructureService.addColAdm(this);
		addToAll(noColAdm, this);
	}

	private static void addToAll(long noColAdm, MockCollectiviteAdministrative ca) {
		if (all.containsKey(noColAdm)) {
			throw new IllegalArgumentException("La collectivité administrative avec le numéro technique = " + noColAdm + " est déjà enregistrée dans la collection !");
		}
		all.put(noColAdm, ca);
	}

	private static Map<Integer, Integer> initDistrict() {
		final Map<Integer, Integer> districts = new HashMap<Integer, Integer>();
		//Aigle
		districts.put(1, 1);
		//Echallens
		districts.put(5, 2);
		// Grandson
		districts.put(6, 3);
		//Lausanne
		districts.put(7, 4);
		//La Vallée
		districts.put(8, 5);
		//Lavaux
		districts.put(9, 6);
		//Morges
		districts.put(10, 7);
		//Moudon
		districts.put(11, 8);
		//Nyon
		districts.put(12, 9);
		//Orbe
		districts.put(13, 10);
		// Payerne
		districts.put(15, 11);
		//Pays d'Enhaut
		districts.put(16, 12);
		//Rolle-Aubonne
		districts.put(17, 13);
		//Vevey
		districts.put(18, 14);
		//Yverson
		districts.put(19, 15);

		return districts;
	}

	private static Map<Integer, Integer> initRegions() {
		final Map<Integer, Integer> regions = new HashMap<Integer, Integer>();
		//Lausanne
		regions.put(7, 1);
		//Nyon
		regions.put(12, 2);
		//Vevey
		regions.put(18, 3);
		//Yverson
		regions.put(19, 4);

		return regions;
	}


	/**
	 * @return the adresse
	 */
	@Override
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
	@Override
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
	 * @return the dateFinValidite
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	 * @return the sigle
	 */
	@Override
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
	@Override
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
	 * @return the isACI
	 */
	@Override
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
	@Override
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
	 * @return the isValide
	 */
	@Override
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

	public static Collection<MockCollectiviteAdministrative> getAll() {
		return Collections.unmodifiableCollection(all.values());
	}
}
