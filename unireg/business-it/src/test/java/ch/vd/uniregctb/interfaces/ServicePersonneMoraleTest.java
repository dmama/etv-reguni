package ch.vd.uniregctb.interfaces;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.TypeMandataire;
import ch.vd.uniregctb.interfaces.service.PartPM;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ServicePersonneMoraleTest extends BusinessItTest {

	private ServicePersonneMoraleService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServicePersonneMoraleService.class, "servicePersonneMoraleService");
	}

	@Test
	public void testGetPM() throws Exception {

		PersonneMorale pm = service.getPersonneMorale(10245L);
		assertNotNull(pm);
		assertContains("Mon Foyer", pm.getRaisonSociale());
	}

	@Test
	public void testGetEtablissement() throws Exception {
		final Etablissement etablissement = service.getEtablissement(5192);
		assertNotNull(etablissement);
		assertEquals(5192L, etablissement.getNoEtablissement());
		assertNull(etablissement.getDateDebut());
		assertNull(etablissement.getDateFin());
		assertEquals("Boulangerie Durussel S", trimValiPatternToNull(etablissement.getRaisonSociale1()));
		assertNull(trimValiPatternToNull(etablissement.getRaisonSociale2()));
		assertNull(trimValiPatternToNull(etablissement.getRaisonSociale3()));
		assertEquals("DURUSSEL BOULANGERIE", trimValiPatternToNull(etablissement.getEnseigne()));
		assertNull(etablissement.getNoTelephone());
		assertNull(etablissement.getNoFax());
		assertNull(etablissement.getChez());
		assertEquals("Ormont-Dessous", etablissement.getRue());
		assertNull(etablissement.getNoPolice());
		assertNull(etablissement.getCCP());
		assertNull(etablissement.getCompteBancaire());
		assertNull(etablissement.getIBAN());
		assertNull(etablissement.getBicSwift());
		assertNull(etablissement.getNomInstitutionFinanciere());
		assertEquals(Long.valueOf(1105), etablissement.getNoOrdreLocalitePostale());
		assertNull(etablissement.getNoRue());
	}

	@Test
	public void testGetMandatsPM() throws Exception {

		PersonneMorale pm = service.getPersonneMorale(10245L, PartPM.MANDATS);
		assertNotNull(pm);
		assertEquals(10245, pm.getNumeroEntreprise());
		assertContains("Mon Foyer", pm.getRaisonSociale());

		final List<Mandat> mandats = pm.getMandats();
		assertNotNull(mandats);
		assertEquals(2, mandats.size());
		assertMandat(date(1993, 6, 11), date(1995, 6, 20), "G", null, null, "021 311'12'84", "021 311'12'87", null, null, null, null, null, TypeMandataire.PERSONNE_MORALE, 149L, mandats.get(0));
		assertMandat(date(1995, 6, 21), date(1999, 1, 28), "G", null, null, "021 801'08'26", "021 802'35'65", null, null, null, null, null, TypeMandataire.PERSONNE_MORALE, 2054L, mandats.get(1));
	}

	private static void assertMandat(final RegDate dataDebut, final RegDate dateFin, final String code, final String prenom, final String nom, final String noTel, final String noFax, final String ccp,
	                                 final String compteBancaire, final String iban, final String bicSwift, final Long noInstit, final TypeMandataire type, final long numeroMandataire, Mandat mandat
	) {
		assertNotNull(mandat);
		assertEquals(dataDebut, mandat.getDateDebut());
		assertEquals(dateFin, mandat.getDateFin());
		assertEquals(code, mandat.getCode());
		assertEquals(prenom, trimValiPatternToNull(mandat.getPrenomContact()));
		assertEquals(nom, trimValiPatternToNull(mandat.getNomContact()));
		assertEquals(noTel, mandat.getNoTelephoneContact());
		assertEquals(noFax, mandat.getNoFaxContact());
		assertEquals(ccp, mandat.getCCP());
		assertEquals(compteBancaire, mandat.getCompteBancaire());
		assertEquals(iban, mandat.getIBAN());
		assertEquals(bicSwift, mandat.getBicSwift());
		assertEquals(noInstit, mandat.getNumeroInstitutionFinanciere());
		assertEquals(type, mandat.getTypeMandataire());
		assertEquals(numeroMandataire, mandat.getNumeroMandataire());
	}
}
