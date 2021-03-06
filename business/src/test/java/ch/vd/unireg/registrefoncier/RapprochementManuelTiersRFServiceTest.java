package ch.vd.unireg.registrefoncier;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.unireg.evenement.identification.contribuable.Demande;
import ch.vd.unireg.evenement.identification.contribuable.EsbHeader;
import ch.vd.unireg.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.identification.contribuable.TypeDemande;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.Sexe;

public class RapprochementManuelTiersRFServiceTest extends BusinessTest {

	private RapprochementManuelTiersRFServiceImpl service;
	private IdentCtbDAO identCtbDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		identCtbDAO = getBean(IdentCtbDAO.class, "identCtbDAO");
		service = new RapprochementManuelTiersRFServiceImpl();
		service.setEmetteurDemandeIdentification("MON_EMETTEUR");
		service.setHibernateTemplate(hibernateTemplate);
		service.setIdentCtbDAO(identCtbDAO);
		service.setQueueRetourPourIdentification("unireg.tralala-tsoin-tsoin");
	}

	@Test
	public void testGenerationDemandePersonnePhysiqueRF() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final PersonnePhysiqueRF rf = addPersonnePhysiqueRF("Francis", "Delamaisonnoire", date(1987, 7, 31), "5473i743278", 42L, null);
			return rf.getId();
		});

		// appel du service de création de demande
		doInNewTransactionAndSession(status -> {
			final PersonnePhysiqueRF rf = hibernateTemplate.get(PersonnePhysiqueRF.class, id);
			Assert.assertNotNull(rf);

			// demande de création
			service.genererDemandeIdentificationManuelle(rf);
			return null;
		});

		// vérification de la demande en base
		doInNewTransactionAndSession(status -> {
			final List<IdentificationContribuable> all = identCtbDAO.getAll();
			Assert.assertNotNull(all);
			Assert.assertEquals(1, all.size());

			final IdentificationContribuable identification = all.get(0);
			Assert.assertNotNull(identification);
			Assert.assertFalse(identification.isAnnule());
			Assert.assertEquals(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT, identification.getEtat());
			Assert.assertNull(identification.getCommentaireTraitement());
			Assert.assertNull(identification.getDateTraitement());
			Assert.assertNull(identification.getNAVS13Upi());
			Assert.assertNull(identification.getNbContribuablesTrouves());
			Assert.assertNull(identification.getTraitementUser());
			Assert.assertNull(identification.getUtilisateurTraitant());
			Assert.assertNull(identification.getReponse());

			final Demande demande = identification.getDemande();
			Assert.assertNotNull(demande);
			Assert.assertEquals("MON_EMETTEUR", demande.getEmetteurId());
			Assert.assertNotNull(demande.getMessageId());
			Assert.assertEquals(Demande.ModeIdentificationType.MANUEL_SANS_ACK, demande.getModeIdentification());
			Assert.assertNull(demande.getMontant());
			Assert.assertEquals(RegDate.get().year(), demande.getPeriodeFiscale());
			Assert.assertEquals(Demande.PrioriteEmetteur.NON_PRIORITAIRE, demande.getPrioriteEmetteur());
			Assert.assertNull(demande.getTransmetteur());
			Assert.assertEquals(TypeTiers.PERSONNE_PHYSIQUE, demande.getTypeContribuableRecherche());
			Assert.assertEquals(TypeDemande.RAPPROCHEMENT_RF, demande.getTypeDemande());
			Assert.assertEquals("RapprochementTiersRF", demande.getTypeMessage());

			final CriteresPersonne personne = demande.getPersonne();
			Assert.assertNotNull(personne);
			Assert.assertNull(personne.getAdresse());
			Assert.assertEquals(date(1987, 7, 31), personne.getDateNaissance());
			Assert.assertNull(personne.getNAVS11());
			Assert.assertNull(personne.getNAVS13());
			Assert.assertEquals("Delamaisonnoire", personne.getNom());
			Assert.assertEquals("Francis", personne.getPrenoms());
			Assert.assertNull(personne.getSexe());

			final EsbHeader header = identification.getHeader();
			Assert.assertNotNull(header);
			Assert.assertTrue(header.getBusinessId(), header.getBusinessId().startsWith(String.format("%d ", id)));
			Assert.assertEquals(AuthenticationHelper.getCurrentPrincipal(), header.getBusinessUser());
			Assert.assertNull(header.getDocumentUrl());
			Assert.assertEquals("unireg.tralala-tsoin-tsoin", header.getReplyTo());

			final Map<String, String> metadata = header.getMetadata();
			Assert.assertNotNull(metadata);
			Assert.assertEquals(1, metadata.size());
			Assert.assertEquals(String.valueOf(id), metadata.get("idTiersRF"));
			return null;
		});

		// si fait une deuxième demande, il ne se passe rien
		doInNewTransactionAndSession(status -> {
			final PersonnePhysiqueRF rf = hibernateTemplate.get(PersonnePhysiqueRF.class, id);
			Assert.assertNotNull(rf);

			// demande de création
			service.genererDemandeIdentificationManuelle(rf);
			return null;
		});

		// vérification en base : rien de nouveau
		doInNewTransactionAndSession(status -> {
			final List<IdentificationContribuable> all = identCtbDAO.getAll();
			Assert.assertNotNull(all);
			Assert.assertEquals(1, all.size());

			// maintenant que l'on met cet état, on devrait pouvoir créer une nouvelle demande
			all.get(0).setEtat(IdentificationContribuable.Etat.TRAITE_MANUELLEMENT);
			return null;
		});

		// maintenant, on doit créer la demande (car la précédente est traitée)
		doInNewTransactionAndSession(status -> {
			final PersonnePhysiqueRF rf = hibernateTemplate.get(PersonnePhysiqueRF.class, id);
			Assert.assertNotNull(rf);

			// demande de création
			service.genererDemandeIdentificationManuelle(rf);
			return null;
		});

		// vérification des demandes en base
		doInNewTransactionAndSession(status -> {
			final List<IdentificationContribuable> all = identCtbDAO.getAll();
			Assert.assertNotNull(all);
			Assert.assertEquals(2, all.size());
			final List<IdentificationContribuable> sorted = all.stream()
					.sorted(Comparator.comparing(IdentificationContribuable::getId))
					.collect(Collectors.toList());

			// l'ancien
			{
				final IdentificationContribuable identification = sorted.get(0);
				Assert.assertNotNull(identification);
				Assert.assertFalse(identification.isAnnule());
				Assert.assertEquals(IdentificationContribuable.Etat.TRAITE_MANUELLEMENT, identification.getEtat());
				Assert.assertNull(identification.getCommentaireTraitement());
				Assert.assertNull(identification.getDateTraitement());
				Assert.assertNull(identification.getNAVS13Upi());
				Assert.assertNull(identification.getNbContribuablesTrouves());
				Assert.assertNull(identification.getTraitementUser());
				Assert.assertNull(identification.getUtilisateurTraitant());
				Assert.assertNull(identification.getReponse());

				final Demande demande = identification.getDemande();
				Assert.assertNotNull(demande);
				Assert.assertEquals("MON_EMETTEUR", demande.getEmetteurId());
				Assert.assertNotNull(demande.getMessageId());
				Assert.assertEquals(Demande.ModeIdentificationType.MANUEL_SANS_ACK, demande.getModeIdentification());
				Assert.assertNull(demande.getMontant());
				Assert.assertEquals(RegDate.get().year(), demande.getPeriodeFiscale());
				Assert.assertEquals(Demande.PrioriteEmetteur.NON_PRIORITAIRE, demande.getPrioriteEmetteur());
				Assert.assertNull(demande.getTransmetteur());
				Assert.assertEquals(TypeTiers.PERSONNE_PHYSIQUE, demande.getTypeContribuableRecherche());
				Assert.assertEquals(TypeDemande.RAPPROCHEMENT_RF, demande.getTypeDemande());
				Assert.assertEquals("RapprochementTiersRF", demande.getTypeMessage());

				final CriteresPersonne personne = demande.getPersonne();
				Assert.assertNotNull(personne);
				Assert.assertNull(personne.getAdresse());
				Assert.assertEquals(date(1987, 7, 31), personne.getDateNaissance());
				Assert.assertNull(personne.getNAVS11());
				Assert.assertNull(personne.getNAVS13());
				Assert.assertEquals("Delamaisonnoire", personne.getNom());
				Assert.assertEquals("Francis", personne.getPrenoms());
				Assert.assertNull(personne.getSexe());

				final EsbHeader header = identification.getHeader();
				Assert.assertNotNull(header);
				Assert.assertTrue(header.getBusinessId(), header.getBusinessId().startsWith(String.format("%d ", id)));
				Assert.assertEquals(AuthenticationHelper.getCurrentPrincipal(), header.getBusinessUser());
				Assert.assertNull(header.getDocumentUrl());
				Assert.assertEquals("unireg.tralala-tsoin-tsoin", header.getReplyTo());

				final Map<String, String> metadata = header.getMetadata();
				Assert.assertNotNull(metadata);
				Assert.assertEquals(1, metadata.size());
				Assert.assertEquals(String.valueOf(id), metadata.get("idTiersRF"));
			}

			// le nouveau
			{
				final IdentificationContribuable identification = sorted.get(1);
				Assert.assertNotNull(identification);
				Assert.assertFalse(identification.isAnnule());
				Assert.assertEquals(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT, identification.getEtat());
				Assert.assertNull(identification.getCommentaireTraitement());
				Assert.assertNull(identification.getDateTraitement());
				Assert.assertNull(identification.getNAVS13Upi());
				Assert.assertNull(identification.getNbContribuablesTrouves());
				Assert.assertNull(identification.getTraitementUser());
				Assert.assertNull(identification.getUtilisateurTraitant());
				Assert.assertNull(identification.getReponse());

				final Demande demande = identification.getDemande();
				Assert.assertNotNull(demande);
				Assert.assertEquals("MON_EMETTEUR", demande.getEmetteurId());
				Assert.assertNotNull(demande.getMessageId());
				Assert.assertEquals(Demande.ModeIdentificationType.MANUEL_SANS_ACK, demande.getModeIdentification());
				Assert.assertNull(demande.getMontant());
				Assert.assertEquals(RegDate.get().year(), demande.getPeriodeFiscale());
				Assert.assertEquals(Demande.PrioriteEmetteur.NON_PRIORITAIRE, demande.getPrioriteEmetteur());
				Assert.assertNull(demande.getTransmetteur());
				Assert.assertEquals(TypeTiers.PERSONNE_PHYSIQUE, demande.getTypeContribuableRecherche());
				Assert.assertEquals(TypeDemande.RAPPROCHEMENT_RF, demande.getTypeDemande());
				Assert.assertEquals("RapprochementTiersRF", demande.getTypeMessage());

				final CriteresPersonne personne = demande.getPersonne();
				Assert.assertNotNull(personne);
				Assert.assertNull(personne.getAdresse());
				Assert.assertEquals(date(1987, 7, 31), personne.getDateNaissance());
				Assert.assertNull(personne.getNAVS11());
				Assert.assertNull(personne.getNAVS13());
				Assert.assertEquals("Delamaisonnoire", personne.getNom());
				Assert.assertEquals("Francis", personne.getPrenoms());
				Assert.assertNull(personne.getSexe());

				final EsbHeader header = identification.getHeader();
				Assert.assertNotNull(header);
				Assert.assertTrue(header.getBusinessId(), header.getBusinessId().startsWith(String.format("%d ", id)));
				Assert.assertEquals(AuthenticationHelper.getCurrentPrincipal(), header.getBusinessUser());
				Assert.assertNull(header.getDocumentUrl());
				Assert.assertEquals("unireg.tralala-tsoin-tsoin", header.getReplyTo());

				final Map<String, String> metadata = header.getMetadata();
				Assert.assertNotNull(metadata);
				Assert.assertEquals(1, metadata.size());
				Assert.assertEquals(String.valueOf(id), metadata.get("idTiersRF"));
			}
			return null;
		});
	}

	@Test
	public void testGenerationDemandePersonneMoraleRF() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final PersonneMoraleRF rf = addPersonneMoraleRF("Chez Géo Trouvetou", "CH550-sfhdsghqw", "5473i743278", 42L, null);
			return rf.getId();
		});

		// appel du service de création de demande
		doInNewTransactionAndSession(status -> {
			final PersonneMoraleRF rf = hibernateTemplate.get(PersonneMoraleRF.class, id);
			Assert.assertNotNull(rf);

			// demande de création
			service.genererDemandeIdentificationManuelle(rf);
			return null;
		});

		// vérification de la demande en base
		doInNewTransactionAndSession(status -> {
			final List<IdentificationContribuable> all = identCtbDAO.getAll();
			Assert.assertNotNull(all);
			Assert.assertEquals(1, all.size());

			final IdentificationContribuable identification = all.get(0);
			Assert.assertNotNull(identification);
			Assert.assertFalse(identification.isAnnule());
			Assert.assertEquals(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT, identification.getEtat());
			Assert.assertNull(identification.getCommentaireTraitement());
			Assert.assertNull(identification.getDateTraitement());
			Assert.assertNull(identification.getNAVS13Upi());
			Assert.assertNull(identification.getNbContribuablesTrouves());
			Assert.assertNull(identification.getTraitementUser());
			Assert.assertNull(identification.getUtilisateurTraitant());
			Assert.assertNull(identification.getReponse());

			final Demande demande = identification.getDemande();
			Assert.assertNotNull(demande);
			Assert.assertEquals("MON_EMETTEUR", demande.getEmetteurId());
			Assert.assertNotNull(demande.getMessageId());
			Assert.assertEquals(Demande.ModeIdentificationType.MANUEL_SANS_ACK, demande.getModeIdentification());
			Assert.assertNull(demande.getMontant());
			Assert.assertEquals(RegDate.get().year(), demande.getPeriodeFiscale());
			Assert.assertEquals(Demande.PrioriteEmetteur.NON_PRIORITAIRE, demande.getPrioriteEmetteur());
			Assert.assertNull(demande.getTransmetteur());
			Assert.assertEquals(TypeTiers.ENTREPRISE, demande.getTypeContribuableRecherche());
			Assert.assertEquals(TypeDemande.RAPPROCHEMENT_RF, demande.getTypeDemande());
			Assert.assertEquals("RapprochementTiersRF", demande.getTypeMessage());

			final CriteresPersonne personne = demande.getPersonne();
			Assert.assertNotNull(personne);
			Assert.assertNull(personne.getAdresse());
			Assert.assertNull(personne.getDateNaissance());
			Assert.assertNull(personne.getNAVS11());
			Assert.assertNull(personne.getNAVS13());
			Assert.assertEquals("Chez Géo Trouvetou", personne.getNom());
			Assert.assertNull(personne.getPrenoms());
			Assert.assertNull(personne.getSexe());

			final EsbHeader header = identification.getHeader();
			Assert.assertNotNull(header);
			Assert.assertTrue(header.getBusinessId(), header.getBusinessId().startsWith(String.format("%d ", id)));
			Assert.assertEquals(AuthenticationHelper.getCurrentPrincipal(), header.getBusinessUser());
			Assert.assertNull(header.getDocumentUrl());
			Assert.assertEquals("unireg.tralala-tsoin-tsoin", header.getReplyTo());

			final Map<String, String> metadata = header.getMetadata();
			Assert.assertNotNull(metadata);
			Assert.assertEquals(1, metadata.size());
			Assert.assertEquals(String.valueOf(id), metadata.get("idTiersRF"));
			return null;
		});
	}

	@Test
	public void testGenerationDemandeCollectivitePubliqueRF() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			final CollectivitePubliqueRF rf = addCollectivitePubliqueRF("A votre bon coeur", "5478654", 43L, null);
			return rf.getId();
		});

		// appel du service de création de demande
		doInNewTransactionAndSession(status -> {
			final CollectivitePubliqueRF rf = hibernateTemplate.get(CollectivitePubliqueRF.class, id);
			Assert.assertNotNull(rf);

			// demande de création
			service.genererDemandeIdentificationManuelle(rf);
			return null;
		});

		// vérification de la demande en base
		doInNewTransactionAndSession(status -> {
			final List<IdentificationContribuable> all = identCtbDAO.getAll();
			Assert.assertNotNull(all);
			Assert.assertEquals(1, all.size());

			final IdentificationContribuable identification = all.get(0);
			Assert.assertNotNull(identification);
			Assert.assertFalse(identification.isAnnule());
			Assert.assertEquals(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT, identification.getEtat());
			Assert.assertNull(identification.getCommentaireTraitement());
			Assert.assertNull(identification.getDateTraitement());
			Assert.assertNull(identification.getNAVS13Upi());
			Assert.assertNull(identification.getNbContribuablesTrouves());
			Assert.assertNull(identification.getTraitementUser());
			Assert.assertNull(identification.getUtilisateurTraitant());
			Assert.assertNull(identification.getReponse());

			final Demande demande = identification.getDemande();
			Assert.assertNotNull(demande);
			Assert.assertEquals("MON_EMETTEUR", demande.getEmetteurId());
			Assert.assertNotNull(demande.getMessageId());
			Assert.assertEquals(Demande.ModeIdentificationType.MANUEL_SANS_ACK, demande.getModeIdentification());
			Assert.assertNull(demande.getMontant());
			Assert.assertEquals(RegDate.get().year(), demande.getPeriodeFiscale());
			Assert.assertEquals(Demande.PrioriteEmetteur.NON_PRIORITAIRE, demande.getPrioriteEmetteur());
			Assert.assertNull(demande.getTransmetteur());
			Assert.assertEquals(TypeTiers.ENTREPRISE, demande.getTypeContribuableRecherche());
			Assert.assertEquals(TypeDemande.RAPPROCHEMENT_RF, demande.getTypeDemande());
			Assert.assertEquals("RapprochementTiersRF", demande.getTypeMessage());

			final CriteresPersonne personne = demande.getPersonne();
			Assert.assertNotNull(personne);
			Assert.assertNull(personne.getAdresse());
			Assert.assertNull(personne.getDateNaissance());
			Assert.assertNull(personne.getNAVS11());
			Assert.assertNull(personne.getNAVS13());
			Assert.assertEquals("A votre bon coeur", personne.getNom());
			Assert.assertNull(personne.getPrenoms());
			Assert.assertNull(personne.getSexe());

			final EsbHeader header = identification.getHeader();
			Assert.assertNotNull(header);
			Assert.assertTrue(header.getBusinessId(), header.getBusinessId().startsWith(String.format("%d ", id)));
			Assert.assertEquals(AuthenticationHelper.getCurrentPrincipal(), header.getBusinessUser());
			Assert.assertNull(header.getDocumentUrl());
			Assert.assertEquals("unireg.tralala-tsoin-tsoin", header.getReplyTo());

			final Map<String, String> metadata = header.getMetadata();
			Assert.assertNotNull(metadata);
			Assert.assertEquals(1, metadata.size());
			Assert.assertEquals(String.valueOf(id), metadata.get("idTiersRF"));
			return null;
		});
	}

	@Test
	public void testMarquage() throws Exception {

		final class Ids {
			long pp;
			long pm;
		}

		// mise en place des données RF
		final Ids idsRF = doInNewTransactionAndSession(status -> {
			final PersonnePhysiqueRF pp = addPersonnePhysiqueRF("Francis", "Delamaisonnoire", date(1987, 7, 31), "5473i743278", 42L, null);
			final PersonneMoraleRF pm = addPersonneMoraleRF("A la mauvaise combine", null, "86437zfg", 4312L, null);

			final Ids res = new Ids();
			res.pp = pp.getId();
			res.pm = pm.getId();
			return res;
		});

		// mise en place des données unireg
		final Ids idsUnireg = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Francis", "Delamaisonnoire", date(1987, 7, 31), Sexe.MASCULIN);
			final RegDate dateDebutEntreprise = date(1987, 4, 3);
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebutEntreprise, null, "A la mauvaise combine");
			addFormeJuridique(pm, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);

			final Ids res = new Ids();
			res.pp = pp.getNumero();
			res.pm = pm.getNumero();
			return res;
		});

		// appel du service de création de demande pour la personne physique
		final Long idDemandePP = doInNewTransactionAndSession(status -> {
			final PersonnePhysiqueRF rf = hibernateTemplate.get(PersonnePhysiqueRF.class, idsRF.pp);
			Assert.assertNotNull(rf);

			// demande de création
			service.genererDemandeIdentificationManuelle(rf);

			// récupération de l'identifiant
			return identCtbDAO.getAll().stream()
					.mapToLong(IdentificationContribuable::getId)
					.max()
					.getAsLong();
		});

		// appel du service de création de demande pour la personne morale
		final Long idDemandePM = doInNewTransactionAndSession(status -> {
			final PersonneMoraleRF rf = hibernateTemplate.get(PersonneMoraleRF.class, idsRF.pm);
			Assert.assertNotNull(rf);

			// demande de création
			service.genererDemandeIdentificationManuelle(rf);

			// récupération de l'identifiant
			return identCtbDAO.getAll().stream()
					.mapToLong(IdentificationContribuable::getId)
					.max()
					.getAsLong();
		});

		// maintenant, on a une demande générée sans réponse, vérifions donc les appels à marquer... (PM)
		doInNewTransactionAndSession(status -> {
			final IdentificationContribuable identification = hibernateTemplate.get(IdentificationContribuable.class, idDemandePM);
			Assert.assertNotNull(identification);
			Assert.assertNull(identification.getReponse());
			Assert.assertEquals(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT, identification.getEtat());
			Assert.assertNull(identification.getCommentaireTraitement());
			Assert.assertNull(identification.getNbContribuablesTrouves());
			Assert.assertNull(identification.getDateTraitement());
			Assert.assertNull(identification.getTraitementUser());

			final TiersRF rf = hibernateTemplate.get(TiersRF.class, idsRF.pm);
			final Tiers unireg = hibernateTemplate.get(Tiers.class, idsUnireg.pm);
			service.marquerDemandesIdentificationManuelleEventuelles(rf, unireg);
			return null;
		});

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final IdentificationContribuable identification = hibernateTemplate.get(IdentificationContribuable.class, idDemandePM);
			Assert.assertNotNull(identification);
			Assert.assertNotNull(identification.getReponse());
			Assert.assertEquals((Long) idsUnireg.pm, identification.getReponse().getNoContribuable());
			Assert.assertNull(identification.getReponse().getNoMenageCommun());
			Assert.assertEquals(IdentificationContribuable.Etat.TRAITE_AUTOMATIQUEMENT, identification.getEtat());
			Assert.assertNull(identification.getCommentaireTraitement());
			Assert.assertEquals((Integer) 1, identification.getNbContribuablesTrouves());
			Assert.assertNotNull(identification.getDateTraitement());
			Assert.assertNotNull(identification.getTraitementUser());
			return null;
		});

		// maintenant, on a une demande générée sans réponse, vérifions donc les appels à marquer... (PP)
		doInNewTransactionAndSession(status -> {
			final IdentificationContribuable identification = hibernateTemplate.get(IdentificationContribuable.class, idDemandePP);
			Assert.assertNotNull(identification);
			Assert.assertNull(identification.getReponse());
			Assert.assertEquals(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT, identification.getEtat());
			Assert.assertNull(identification.getCommentaireTraitement());
			Assert.assertNull(identification.getNbContribuablesTrouves());
			Assert.assertNull(identification.getDateTraitement());
			Assert.assertNull(identification.getTraitementUser());

			final TiersRF rf = hibernateTemplate.get(TiersRF.class, idsRF.pp);
			final Tiers unireg = hibernateTemplate.get(Tiers.class, idsUnireg.pp);
			service.marquerDemandesIdentificationManuelleEventuelles(rf, unireg);
			return null;
		});

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final IdentificationContribuable identification = hibernateTemplate.get(IdentificationContribuable.class, idDemandePP);
			Assert.assertNotNull(identification);
			Assert.assertNotNull(identification.getReponse());
			Assert.assertEquals((Long) idsUnireg.pp, identification.getReponse().getNoContribuable());
			Assert.assertNull(identification.getReponse().getNoMenageCommun());
			Assert.assertEquals(IdentificationContribuable.Etat.TRAITE_AUTOMATIQUEMENT, identification.getEtat());
			Assert.assertNull(identification.getCommentaireTraitement());
			Assert.assertEquals((Integer) 1, identification.getNbContribuablesTrouves());
			Assert.assertNotNull(identification.getDateTraitement());
			Assert.assertNotNull(identification.getTraitementUser());
			return null;
		});

	}
}
