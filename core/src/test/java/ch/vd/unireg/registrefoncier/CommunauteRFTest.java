package ch.vd.unireg.registrefoncier;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapprochementRF;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CommunauteRFTest {

	/**
	 * [SIFISC-20373] Ce test vérifie que les informations de la communauté sont bien retournées quand tous les membres de la communautés sont rapprochés (cas passant).
	 */
	@Test
	public void testBuildCommunauteInfoCommunauteAvecTousLesTiersRapproches() throws Exception {

		final long idArnold = 333L;
		final long idEvelyne = 555L;
		final RegDate dateAchat = RegDate.get(1995, 3, 24);

		// on crée les tiers RF
		final PersonnePhysiqueRF arnoldRf = addPersonnePhysiqueRF("Arnold", "Totore", RegDate.get(1950, 4, 2), "3783737", 1233L, null);
		final PersonnePhysiqueRF evelyneRf = addPersonnePhysiqueRF("Evelyne", "Fondu", RegDate.get(1944, 12, 12), "472382", 9239L, null);
		final CommunauteRF communauteRF = new CommunauteRF();

		// on crée les tiers Unireg
		final PersonnePhysique arnoldUnireg = addNonHabitant(idArnold, "Arnold", "Totore", RegDate.get(1950, 4, 2), Sexe.MASCULIN);
		final PersonnePhysique evelyneUnireg = addNonHabitant(idEvelyne, "Evelyne", "Fondu", RegDate.get(1944, 12, 12), Sexe.FEMININ);

		// on crée les rapprochements qui vont bien
		addRapprochementRF(null, null, TypeRapprochementRF.MANUEL, arnoldUnireg, arnoldRf, false);
		addRapprochementRF(null, null, TypeRapprochementRF.MANUEL, evelyneUnireg, evelyneRf, false);

		final BienFondsRF immeuble = addImmeubleRF("382929efa218");

		final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(123, 1995, 23, 3);
		addDroitCommunauteRF(dateAchat, dateAchat, null, null, "Achat", null, "48234923829", "48234923828", numeroAffaire, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, communauteRF, immeuble);
		addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "47840038", "47840037", numeroAffaire, new Fraction(1, 2), GenrePropriete.COMMUNE, arnoldRf, immeuble, communauteRF);
		addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "84893923", "84893922", numeroAffaire, new Fraction(1, 2), GenrePropriete.COMMUNE, evelyneRf, immeuble, communauteRF);


		final CommunauteRFMembreInfo info = communauteRF.buildMembreInfoNonTries();
		assertNotNull(info);
		assertEquals(2, info.getCount());

		final Collection<Long> memberIds = info.getCtbIds();
		assertNotNull(memberIds);
		assertTrue(memberIds.contains(idArnold));
		assertTrue(memberIds.contains(idEvelyne));
		assertEmpty(info.getTiersRF());
	}

	/**
	 * [SIFISC-20373] Ce test vérifie que le nombre de membres dans la communauté est bien renseigné, même si tous les tiers de la communauté ne sont pas rapprochés.
	 */
	@Test
	public void testBuildCommunauteInfoCommunauteAvecCertainsTiersRapproches() throws Exception {

		final long idArnold = 333L;
		final long idEvelyne = 555L;
		final RegDate dateAchat = RegDate.get(1995, 3, 24);

		// on crée les tiers RF
		final PersonnePhysiqueRF arnoldRf = addPersonnePhysiqueRF("Arnold", "Totore", RegDate.get(1950, 4, 2), "3783737", 1233L, null);
		final PersonnePhysiqueRF evelyneRf = addPersonnePhysiqueRF("Evelyne", "Fondu", RegDate.get(1944, 12, 12), "472382", 9239L, null);
		final PersonnePhysiqueRF totrRf = addPersonnePhysiqueRF("Totor", "Fantomas", RegDate.get(1923, 6, 6), "3437", 28920L, null);
		final CommunauteRF communauteRF = new CommunauteRF();

		// on crée les tiers Unireg
		final PersonnePhysique arnoldUnireg = addNonHabitant(idArnold, "Arnold", "Totore", RegDate.get(1950, 4, 2), Sexe.MASCULIN);
		final PersonnePhysique evelyneUnireg = addNonHabitant(idEvelyne, "Evelyne", "Fondu", RegDate.get(1944, 12, 12), Sexe.FEMININ);

		// on crée les rapprochements qui vont bien (attention, totor n'est pas rapproché)
		addRapprochementRF(null, null, TypeRapprochementRF.MANUEL, arnoldUnireg, arnoldRf, false);
		addRapprochementRF(null, null, TypeRapprochementRF.MANUEL, evelyneUnireg, evelyneRf, false);

		// on crée l'immeuble et les droits associés
		final BienFondsRF immeuble = addImmeubleRF("382929efa218");

		final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(123, 1995, 23, 3);
		addDroitCommunauteRF(dateAchat, dateAchat, null, null, "Achat", null, "48234923829", "48234923828", numeroAffaire, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, communauteRF, immeuble);
		addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "47840038", "47840037", numeroAffaire, new Fraction(1, 3), GenrePropriete.COMMUNE, arnoldRf, immeuble, communauteRF);
		addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "84893923", "84893922", numeroAffaire, new Fraction(1, 3), GenrePropriete.COMMUNE, evelyneRf, immeuble, communauteRF);
		addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "3403892", "3403891", numeroAffaire, new Fraction(1, 3), GenrePropriete.COMMUNE, totrRf, immeuble, communauteRF);

		final CommunauteRFMembreInfo info = communauteRF.buildMembreInfoNonTries();
		assertNotNull(info);
		assertEquals(3, info.getCount());

		final Collection<Long> memberIds = info.getCtbIds();
		assertNotNull(memberIds);
		assertTrue(memberIds.contains(idArnold));
		assertTrue(memberIds.contains(idEvelyne));

		final Collection<TiersRF> tiersRF = info.getTiersRF();
		assertNotNull(tiersRF);
		assertEquals(1, tiersRF.size());
		final PersonnePhysiqueRF tiers0 = (PersonnePhysiqueRF) tiersRF.iterator().next();
		assertEquals("Totor", tiers0.getPrenom());
		assertEquals("Fantomas", tiers0.getNom());
	}

	private PersonnePhysiqueRF addPersonnePhysiqueRF(String prenom, String nom, RegDate dateNaissance, String idRf, long noRf, Long numeroContribuable) {
		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setDateNaissance(dateNaissance);
		pp.setIdRF(idRf);
		pp.setNoRF(noRf);
		pp.setPrenom(prenom);
		pp.setNom(nom);
		pp.setNoContribuable(numeroContribuable);
		return pp;
	}

	protected PersonnePhysique addNonHabitant(@Nullable Long noTiers, String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		final PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNumero(noTiers);
		nh.setPrenomUsuel(prenom);
		nh.setNom(nom);
		nh.setDateNaissance(dateNaissance);
		nh.setSexe(sexe);
		return nh;
	}

	protected RapprochementRF addRapprochementRF(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, TypeRapprochementRF type, Contribuable ctb, TiersRF tiersRF, boolean annule) {
		final RapprochementRF rrf = new RapprochementRF();
		rrf.setAnnule(annule);
		rrf.setDateDebut(dateDebut);
		rrf.setDateFin(dateFin);
		rrf.setContribuable(ctb);
		rrf.setTiersRF(tiersRF);
		rrf.setTypeRapprochement(type);
		ctb.addRapprochementRF(rrf);
		tiersRF.addRapprochementRF(rrf);
		return rrf;
	}

	protected BienFondsRF addImmeubleRF(String idRF) {
		final BienFondsRF immeuble = new BienFondsRF();
		immeuble.setIdRF(idRF);
		return immeuble;
	}

	protected DroitProprieteCommunauteRF addDroitCommunauteRF(RegDate dateDebut, RegDate dateDebutMetier, RegDate dateFin, RegDate dateFinMetier, String motifDebut, String motifFin, String masterIdRF, String versionIdRF,
	                                                          IdentifiantAffaireRF numeroAffaire,
	                                                          Fraction part, GenrePropriete regime,
	                                                          CommunauteRF communauteRF, ImmeubleRF immeuble) {
		final DroitProprieteCommunauteRF droit = new DroitProprieteCommunauteRF();
		droit.setImmeuble(immeuble);
		droit.setAyantDroit(communauteRF);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutMetier(dateDebutMetier);
		droit.setDateFin(dateFin);
		droit.setDateFinMetier(dateFinMetier);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setMasterIdRF(masterIdRF);
		droit.setVersionIdRF(versionIdRF);
		droit.setPart(part);
		droit.setRegime(regime);
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(dateDebutMetier, motifDebut, numeroAffaire));

		// [SIFISC-24553] on met-à-jour à la main de la liste des servitudes pour pouvoir parcourir le graphe des dépendances dans le DatabaseChangeInterceptor
		immeuble.addDroitPropriete(droit);
		communauteRF.addDroitPropriete(droit);

		return droit;
	}

	protected DroitProprietePersonnePhysiqueRF addDroitPersonnePhysiqueRF(RegDate dateDebut, RegDate dateDebutMetier, RegDate dateFin, RegDate dateFinMetier, String motifDebut, String motifFin, String masterIdRF, String versionIdRF,
	                                                                      IdentifiantAffaireRF numeroAffaire, Fraction part, GenrePropriete regime,
	                                                                      PersonnePhysiqueRF ayantDroit,
	                                                                      ImmeubleRF immeuble, CommunauteRF communaute) {
		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setAyantDroit(ayantDroit);
		droit.setImmeuble(immeuble);
		droit.setDateDebut(dateDebut);
		droit.setDateDebutMetier(dateDebutMetier);
		droit.setDateFin(dateFin);
		droit.setDateFinMetier(dateFinMetier);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setMasterIdRF(masterIdRF);
		droit.setVersionIdRF(versionIdRF);
		droit.setPart(part);
		droit.setRegime(regime);
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(dateDebutMetier, motifDebut, numeroAffaire));

		if (communaute != null) {
			droit.setCommunaute(communaute);
			communaute.addMembre(droit);
		}

		// [SIFISC-24553] on met-à-jour à la main de la liste des servitudes pour pouvoir parcourir le graphe des dépendances dans le DatabaseChangeInterceptor
		immeuble.addDroitPropriete(droit);
		ayantDroit.addDroitPropriete(droit);

		return droit;
	}

}