package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;

public class MockOfficeImpot extends MockCollectiviteAdministrative implements OfficeImpot {

	public static MockOfficeImpot OID_AIGLE = new MockOfficeImpot(1, new MockAdresse("rue de la Gare", "27", "1860", "Aigle"), "Office d'impôt du district", "d'Aigle", null, "OID AIGLE");
	public static MockOfficeImpot OID_ROLLE = new MockOfficeImpot(2, null, "Administration cantonale des impôts", "Office d'impôt de Rolle et d'Aubonne", null, "OID AUBONNE/ROLLE");
	public static MockOfficeImpot OID_AVENCHE = new MockOfficeImpot(3, null, "Office d'impôt du district", "de la Broye - Vully", "Bureau d'Avenches", "OID AVENCHES");
	public static MockOfficeImpot OID_COSSONAY = new MockOfficeImpot(4, null, "Office d'impôt du district", "de Morges", "Bureau de Cossonay", "OID COSSONAY");
	public static MockOfficeImpot OID_ECHALLENS = new MockOfficeImpot(5, null, "Office d'impôt du district", "du Gros-de-Vaud", null, "OID ECHALLENS");
	public static MockOfficeImpot OID_GRANDSON = new MockOfficeImpot(6, null, "Office d'impôt du district", "Jura - Nord vaudois", "Bureau de Grandson", "OID GRANDSON");
	public static MockOfficeImpot OID_LAUSANNE_OUEST = new MockOfficeImpot(7, new MockAdresse("rue de la Paix", "1", "1000", "Lausanne"), "Office d'impôt des districts de", "Lausanne et Ouest lausannois", null, "OID LAUSANNE");
	public static MockOfficeImpot OID_LA_VALLEE = new MockOfficeImpot(8, null, "Office d'impôt du district", "du Jura - Nord vaudois", "Bureau de La Vallée", "OID LA VALLEE");
	public static MockOfficeImpot OID_LAVAUX = new MockOfficeImpot(9, null, "Office d'impôt du district", "de Lavaux - Oron", null, "OID LAVAUX");
	public static MockOfficeImpot OID_MORGES = new MockOfficeImpot(10, new MockAdresse("rue de la Paix", "1", "1110", "Morges"), "Office d'impôt du district", "de Morges", null, "OID MORGES");
	public static MockOfficeImpot OID_MOUDON = new MockOfficeImpot(11, null, "Office d'impôt du district", "de la Broye - Vully", "Bureau de Moudon", "OID MOUDON");
	public static MockOfficeImpot OID_NYON = new MockOfficeImpot(12, new MockAdresse("Avenue Reverdil", "4-6", "1341", "Nyon"), "Office d'impôt du district", "de Nyon", null, "OID NYON");
	public static MockOfficeImpot OID_ORBE = new MockOfficeImpot(13, new MockAdresse("rue de la Poste", "2", "1350", "Orbe"), "Office d'impôt du district", "du Jura - Nord vaudois", "Bureau d'Orbe", "OID ORBE");
	public static MockOfficeImpot OID_ORON = new MockOfficeImpot(14, null, "Office d'impôt du district", "de Lavaux - Oron", "Bureau d'Oron", "OID ORON");
	public static MockOfficeImpot OID_PAYERNE = new MockOfficeImpot(15, null, "Office d'impôt du district", "de la Broye - Vully", null, "OID PAYERNE");
	public static MockOfficeImpot OID_PAYS_D_ENHAUT = new MockOfficeImpot(16, null, "Office d'impôt du district", "de la Riviera - Pays-d'Enhaut", "Bureau du Pays-d'Enhaut", "OID PAYS-D'ENHAUT");
	public static MockOfficeImpot OID_ROLLE_AUBONNE = new MockOfficeImpot(17, null, "Office d'impôt des districts", "de Nyon et Morges", "Bureau de Rolle-Aubonne", "OID ROLLE/AUBONNE");
	public static MockOfficeImpot OID_VEVEY = new MockOfficeImpot(18, new MockAdresse("Rue du Simplon", "22", "Case postale 1032", "1800", "Vevey 1"), "Office d'impôt du district", "de la Riviera - Pays-d'Enhaut", null, "OID VEVEY");
	public static MockOfficeImpot OID_YVERDON = new MockOfficeImpot(19, null, "Office d'impôt du district", "du Jura - Nord vaudois", null, "OID YVERDON");
	public static MockOfficeImpot OID_LAUSANNE_VILLE = new MockOfficeImpot(20, new MockAdresse("rue de la Paix", "1", "1000", "Lausanne"), "Office d'impôt des districts de", "Lausanne et Ouest lausannois", null, "OID LAUSANNE (VILLE)");
	public static MockOfficeImpot OID_PM = new MockOfficeImpot(21, null, "Administration cantonale des impôts", "Office d'impôt des Personnes Morales", null, "OI PERSONNES MORALES");
	public static MockOfficeImpot OID_ST_CROIX = new MockOfficeImpot(121, null, "Office d'impôt du district de Grandson", "Bureau de Sainte-Croix", null, "OID GRANDSON,  STE-CROIX");

	public MockOfficeImpot() {
		super();
	}

	public MockOfficeImpot(long noColAdm, Adresse adresse, String nomComplet1, String nomComplet2, String nomComplet3,
			String nomCourt) {
		super(noColAdm, adresse, nomComplet1, nomComplet2, nomComplet3, nomCourt);
	}

}
