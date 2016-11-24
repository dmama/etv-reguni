package ch.vd.uniregctb.registrefoncier;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.EsbHeader;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.tiers.TypeTiers;

public class RapprochementManuelTiersRFServiceTest extends BusinessTest {

	private RapprochementManuelTiersRFServiceImpl service;
	private IdentCtbDAO identCtbDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
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
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysiqueRF rf = addPersonnePhysiqueRF("Francis", "Delamaisonnoire", date(1987, 7, 31), "5473i743278", 42L, null);
				return rf.getId();
			}
		});

		// appel du service de création de demande
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysiqueRF rf = hibernateTemplate.get(PersonnePhysiqueRF.class, id);
				Assert.assertNotNull(rf);

				// demande de création
				service.genererDemandeIdentificationManuelle(rf);
			}
		});

		// vérification de la demande en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
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
			}
		});

		// si fait une deuxième demande, il ne se passe rien
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysiqueRF rf = hibernateTemplate.get(PersonnePhysiqueRF.class, id);
				Assert.assertNotNull(rf);

				// demande de création
				service.genererDemandeIdentificationManuelle(rf);
			}
		});

		// vérification en base : rien de nouveau
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<IdentificationContribuable> all = identCtbDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());

				// maintenant que l'on met cet état, on devrait pouvoir créer une nouvelle demande
				all.get(0).setEtat(IdentificationContribuable.Etat.TRAITE_MANUELLEMENT);
			}
		});

		// maintenant, on doit créer la demande (car la précédente est traitée)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysiqueRF rf = hibernateTemplate.get(PersonnePhysiqueRF.class, id);
				Assert.assertNotNull(rf);

				// demande de création
				service.genererDemandeIdentificationManuelle(rf);
			}
		});

		// vérification des demandes en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
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
			}
		});
	}

	@Test
	public void testGenerationDemandePersonneMoraleRF() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonneMoraleRF rf = addPersonneMoraleRF("Chez Géo Trouvetou", "CH550-sfhdsghqw", "5473i743278", 42L, null);
				return rf.getId();
			}
		});

		// appel du service de création de demande
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonneMoraleRF rf = hibernateTemplate.get(PersonneMoraleRF.class, id);
				Assert.assertNotNull(rf);

				// demande de création
				service.genererDemandeIdentificationManuelle(rf);
			}
		});

		// vérification de la demande en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
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
			}
		});
	}

	@Test
	public void testGenerationDemandeCollectivitePubliqueRF() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final CollectivitePubliqueRF rf = addCollectivitePubliqueRF("A votre bon coeur", "5478654", 43L, null);
				return rf.getId();
			}
		});

		// appel du service de création de demande
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final CollectivitePubliqueRF rf = hibernateTemplate.get(CollectivitePubliqueRF.class, id);
				Assert.assertNotNull(rf);

				// demande de création
				service.genererDemandeIdentificationManuelle(rf);
			}
		});

		// vérification de la demande en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
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
			}
		});
	}
}
