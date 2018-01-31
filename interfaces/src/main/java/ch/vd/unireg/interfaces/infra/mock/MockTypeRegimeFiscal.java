package ch.vd.unireg.interfaces.infra.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.ModeExoneration;
import ch.vd.unireg.interfaces.infra.data.PlageExonerationFiscale;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscalFidor;
import ch.vd.uniregctb.type.CategorieEntreprise;

import static ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration.IBC;
import static ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration.ICI;
import static ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration.IFONC;
import static ch.vd.unireg.interfaces.infra.data.ModeExoneration.DE_FAIT;
import static ch.vd.unireg.interfaces.infra.data.ModeExoneration.TOTALE;
import static ch.vd.uniregctb.type.CategorieEntreprise.APM;
import static ch.vd.uniregctb.type.CategorieEntreprise.INDET;
import static ch.vd.uniregctb.type.CategorieEntreprise.PM;
import static ch.vd.uniregctb.type.CategorieEntreprise.SP;

public class MockTypeRegimeFiscal extends TypeRegimeFiscalFidor {

	private static final long serialVersionUID = 7983766633170770086L;

	// SIFISC-22648: Type spécial signalant que le régime fiscal est encore à déterminer pour cette entité.
	public static final MockTypeRegimeFiscal INDETERMINE = new MockTypeRegimeFiscal                         ("00", 2016, null, "En attente de détermination", true, true, INDET);

	public static final MockTypeRegimeFiscal ORDINAIRE_PM = new MockTypeRegimeFiscal                        ("01", 1994, null, "Ordinaire",                                         true, true, PM);
	public static final MockTypeRegimeFiscal EXO_90G = new MockTypeRegimeFiscal                             ("109", 2003, null, "PM Pure utilité publique (Art. 90g LI)",           true, true, PM, createExo(2003, IBC, DE_FAIT));
	public static final MockTypeRegimeFiscal PARTICIPATIONS = new MockTypeRegimeFiscal                      ("11", 1994, null, "Société de participations",                         true, false, PM);
	public static final MockTypeRegimeFiscal PARTICIPATIONS_PART_IMPOSABLE = new MockTypeRegimeFiscal       ("12", 2001, null, "Société de participations avec immeuble(s)",        true, false, PM);
	public static final MockTypeRegimeFiscal COMMUNAUTE_PERSONNES_ETRANGERES_PM = new MockTypeRegimeFiscal  ("13", 2016, null, "Communauté de pers. étrangères - PM (Art. 84 LI)",  true, true, PM);
	public static final MockTypeRegimeFiscal EXO_90E = new MockTypeRegimeFiscal                             ("190-1", 2003, null, "PM Institutions de prévoyance (Art. 90e LI)",    true, true, PM, createExo(2003, IBC, DE_FAIT));
	public static final MockTypeRegimeFiscal EXO_90F = new MockTypeRegimeFiscal                             ("190-2", 2003, null, "PM Caisses assurances sociales (Art. 90f LI)",   true, true, PM, createExo(2003, IBC, DE_FAIT));
	public static final MockTypeRegimeFiscal EXO_90H = new MockTypeRegimeFiscal                             ("190-3", 2003, null, "PM Buts culturels (Art. 90h LI)",                true, true, PM, createExo(2003, IBC, DE_FAIT), createExo(2003, IFONC, DE_FAIT));
	public static final MockTypeRegimeFiscal EXO_90C = new MockTypeRegimeFiscal                             ("190-4", 2003, null, "PM Communes + établiss. (Art. 90c LI)",          true, true, PM, createExo(2003, IBC, TOTALE), createExo(2003, ICI, TOTALE), createExo(2003, IFONC, TOTALE));
	public static final MockTypeRegimeFiscal EXO_90C_IBC_DE_FAIT = new MockTypeRegimeFiscal                 ("190-5", 2003, null, "PM Communes + établiss. (Art. 90c LI), IBC exonéré de fait", true, true, PM, createExo(2003, IBC, DE_FAIT), createExo(2003, ICI, TOTALE), createExo(2003, IFONC, TOTALE));
	public static final MockTypeRegimeFiscal ORDINAIRE_ICC_BASE_MIXTE_RPT = new MockTypeRegimeFiscal        ("41", 2001, null, "Ordinaire (ICC base mixte - RPT)",                  true, false, PM);
	public static final MockTypeRegimeFiscal STE_BASE_MIXTE = new MockTypeRegimeFiscal                      ("41C", 2001, null, "Société de base (mixte)",                          true, false, PM);
	public static final MockTypeRegimeFiscal ORDINAIRE_ICC_BASE_DOMICILE_RPT = new MockTypeRegimeFiscal     ("42", 2001, null, "Ordinaire (ICC base domicile - RPT)",               false, true, PM);
	public static final MockTypeRegimeFiscal STE_DOMICILE = new MockTypeRegimeFiscal                        ("42C", 2001, null, "Société de domicile",                              true, false, PM);
	public static final MockTypeRegimeFiscal FONDS_PLACEMENT = new MockTypeRegimeFiscal                     ("50", 1992, null, "Placement collectif avec immeuble(s) (Art. 103 LI)",true, true, APM);
	public static final MockTypeRegimeFiscal TRANSPORT_CONCESSIONNE = new MockTypeRegimeFiscal              ("60", 1994, null, "Transport concessionné",                            true, true, PM);
	public static final MockTypeRegimeFiscal ORDINAIRE_APM = new MockTypeRegimeFiscal                       ("70", 1994, null, "Ordinaire Assoc-Fond.",                             true, true, APM);
	public static final MockTypeRegimeFiscal ART90G = new MockTypeRegimeFiscal                              ("709", 1994, null, "APM Pure utilité publique (Art. 90g LI)",          true, true, APM, createExo(1994, IBC, DE_FAIT));
	public static final MockTypeRegimeFiscal COMMUNAUTE_PERSONNES_ETRANGERES_APM = new MockTypeRegimeFiscal ("71", 2016, null, "Communauté de pers. étrangères - APM (Art. 84 LI)", true, true, APM);
	public static final MockTypeRegimeFiscal ART90D = new MockTypeRegimeFiscal                              ("715", 2001, null, "Eglises et paroisses (Art. 90d LI)",               true, true, APM, createExo(2001, IBC, DE_FAIT), createExo(2001, IFONC, DE_FAIT));
	public static final MockTypeRegimeFiscal ART90H = new MockTypeRegimeFiscal                              ("719", 1994, null, "APM Buts cultuels (Art. 90h LI)",                  true, true, APM, createExo(1994, IBC, DE_FAIT), createExo(1994, IFONC, DE_FAIT));
	public static final MockTypeRegimeFiscal ART90E = new MockTypeRegimeFiscal                              ("729", 1994, null, "APM Institutions de prévoyance (Art. 90e LI)",     true, true, APM, createExo(1994, IBC, DE_FAIT));
	public static final MockTypeRegimeFiscal ART90F = new MockTypeRegimeFiscal                              ("739", 2001, null, "APM Caisses assurances sociales (Art. 90f LI)",    true, true, APM, createExo(2001, IBC, DE_FAIT));
	public static final MockTypeRegimeFiscal ART90A = new MockTypeRegimeFiscal                              ("749-1", 2016, null, "Confédération (Art. 90a LI)",                    true, true, APM, createExo(2001, IBC, TOTALE), createExo(2001, ICI, TOTALE), createExo(2001, IFONC, TOTALE));
	public static final MockTypeRegimeFiscal ART90I = new MockTypeRegimeFiscal                              ("749-2", 2016, null, "Etats étrangers (Art. 90i LI)",                  true, true, APM, createExo(2001, IBC, TOTALE), createExo(2001, ICI, TOTALE));
	public static final MockTypeRegimeFiscal ART90I_ACTIVITE = new MockTypeRegimeFiscal                     ("749-3", 2016, null, "Etats étrangers (Art. 90i LI), avec activité",    true, true, APM);
	public static final MockTypeRegimeFiscal ART90B = new MockTypeRegimeFiscal                              ("759", 2001, null, "Canton + établiss. (Art. 90b LI)",                 true, true, APM, createExo(2001, IBC, TOTALE), createExo(2001, ICI, TOTALE), createExo(2001, IFONC, TOTALE));
	public static final MockTypeRegimeFiscal ART90C = new MockTypeRegimeFiscal                              ("769", 2001, null, "APM Communes + établiss. (Art. 90c LI)",           true, true, APM, createExo(2001, IBC, TOTALE), createExo(2001, ICI, TOTALE), createExo(2001, IFONC, TOTALE));
	public static final MockTypeRegimeFiscal ART90J = new MockTypeRegimeFiscal                              ("779", 1992, null, "Placement collectif exonéré (Art. 90j LI)",        true, true, APM, createExo(1992, IBC, DE_FAIT));

	// SIFISC-22648: Type spécial société de personnes
	public static final MockTypeRegimeFiscal SOCIETE_PERS = new MockTypeRegimeFiscal                        ("80", 2016, null, "Société de personnes",                              true, true, SP, createExo(1994, IBC, TOTALE), createExo(1994, ICI, TOTALE), createExo(1994, IFONC, TOTALE));

	//
	// mocks spécialisés pour les tests d'exonerations
	//
	public static final MockTypeRegimeFiscal EXO_IBC_TOTALE = new MockTypeRegimeFiscal                      ("IBCTOTAL", 2010, 2020, "Exonéré total IBC", true, true, PM, createExo(2010, IBC, TOTALE));
	public static final MockTypeRegimeFiscal EXO_IBC_FAIT = new MockTypeRegimeFiscal                        ("IBCFAIT", 2010, 2020, "Exonéré de fait IBC", true, true, PM, createExo(2010, IBC, DE_FAIT));
	public static final MockTypeRegimeFiscal EXO_ICI_TOTALE = new MockTypeRegimeFiscal                      ("ICITOTAL", 2010, 2020, "Exonéré total ICI", true, true, PM, createExo(2010, ICI, TOTALE));
	public static final MockTypeRegimeFiscal EXO_ICI_FAIT = new MockTypeRegimeFiscal                        ("ICIFAIT", 2010, 2020, "Exonéré de fait ICI", true, true, PM, createExo(2010, ICI, DE_FAIT));
	public static final MockTypeRegimeFiscal EXO_IFONC_TOTALE = new MockTypeRegimeFiscal                    ("IFONCTOTAL", 2010, 2020, "Exonéré total IFONC", true, true, PM, createExo(2010, IFONC, TOTALE));
	public static final MockTypeRegimeFiscal EXO_IFONC_FAIT = new MockTypeRegimeFiscal                      ("IFONCFAIT", 2010, 2020, "Exonéré de fait IFONC", true, true, PM, createExo(2010, IFONC, DE_FAIT));


	public static final MockTypeRegimeFiscal[] ALL = buildAllMocks();

	private static MockTypeRegimeFiscal[] buildAllMocks() {
		final List<MockTypeRegimeFiscal> mocks = new ArrayList<>();
		for (Field field : MockTypeRegimeFiscal.class.getFields()) {
			if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
				if (MockTypeRegimeFiscal.class.isAssignableFrom(field.getType())) {
					try {
						final MockTypeRegimeFiscal value = (MockTypeRegimeFiscal) field.get(null);
						if (value != null) {
							mocks.add(value);
						}
					}
					catch (IllegalAccessException e) {
						 throw new RuntimeException(e);
					}
				}
			}
		}

		return mocks.toArray(new MockTypeRegimeFiscal[mocks.size()]);
	}

	private MockTypeRegimeFiscal(String code, Integer premierePF, Integer dernierePF, String libelle, boolean cantonal, boolean federal, CategorieEntreprise categorie, PlageExonerationFiscale... exonerations) {
		super(code, premierePF, dernierePF, libelle, cantonal, federal, categorie, Arrays.asList(exonerations));
	}

	private static PlageExonerationFiscale createExo(int periodeDebut, GenreImpotExoneration genreImpot, ModeExoneration mode) {
		return new PlageExonerationFiscale(periodeDebut, null, genreImpot, mode);
	}
}
