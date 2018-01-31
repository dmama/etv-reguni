package ch.vd.uniregctb.evenement.reqdes.reception;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ch.ech.ech0011.v5.PlaceOfOrigin;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.xml.event.reqdes.v1.Actor;
import ch.vd.unireg.xml.event.reqdes.v1.CreationModification;
import ch.vd.unireg.xml.event.reqdes.v1.FullName;
import ch.vd.unireg.xml.event.reqdes.v1.Identity;
import ch.vd.unireg.xml.event.reqdes.v1.MaritalStatus;
import ch.vd.unireg.xml.event.reqdes.v1.Nationality;
import ch.vd.unireg.xml.event.reqdes.v1.NotarialDeed;
import ch.vd.unireg.xml.event.reqdes.v1.NotarialInformation;
import ch.vd.unireg.xml.event.reqdes.v1.ObjectFactory;
import ch.vd.unireg.xml.event.reqdes.v1.Partner;
import ch.vd.unireg.xml.event.reqdes.v1.Residence;
import ch.vd.unireg.xml.event.reqdes.v1.Stakeholder;
import ch.vd.unireg.xml.event.reqdes.v1.StakeholderReferenceWithRole;
import ch.vd.unireg.xml.event.reqdes.v1.Transaction;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.IdentityKey;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.evenement.reqdes.engine.EvenementReqDesProcessor;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.EvenementReqDesDAO;
import ch.vd.uniregctb.reqdes.InformationsActeur;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.RolePartiePrenante;
import ch.vd.uniregctb.reqdes.TransactionImmobiliere;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.tiers.OriginePersonnePhysique;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.xml.DataHelper;

public class ReqDesEventHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReqDesEventHandler.class);
	private static final String VISA = "ReqDesEvent";

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private ServiceInfrastructureService infraService;
	private EvenementReqDesDAO evenementDAO;
	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private EvenementReqDesProcessor processor;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setEvenementDAO(EvenementReqDesDAO evenementDAO) {
		this.evenementDAO = evenementDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setProcessor(EvenementReqDesProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		final String businessId = message.getBusinessId();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Réception d'un message ReqDes {businessId='%s'}", businessId));
		}
		final long start = System.nanoTime();
		try {
			final Source src = message.getBodyAsSource();
			final String xml = message.getBodyAsString();

			AuthenticationHelper.pushPrincipal(VISA);
			try {
				onMessage(src, xml, businessId);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
		finally {
			if (LOGGER.isInfoEnabled()) {
				final long end = System.nanoTime();
				LOGGER.info(String.format("Réception du message ReqDes {businessId='%s'} traitée en %d ms", businessId, TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
		}
	}

	protected void onMessage(Source xml, String xmlContent, String businessId) throws IOException, EsbBusinessException {
		final CreationModification data;
		try {
			data = parse(xml);
		}
		catch (SAXException | JAXBException e) {
			LOGGER.error(String.format("Format XML invalide : %s", e.getMessage()), e);
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}

		// a-t-on déjà reçu un message pour la même affaire ?
		final long noAffaire = Long.parseLong(businessId);      // TODO il faudra changer cela pour avoir un vrai champ dans le XML...
		final List<EvenementReqDes> dejaPresent = evenementDAO.findByNoAffaire(noAffaire);
		final boolean doublon = dejaPresent != null && !dejaPresent.isEmpty();
		if (doublon) {
			LOGGER.warn(String.format("Un message ReqDes avec le même numéro d'affaire (%d) a déjà été reçu -> traitement manuel systématique.", noAffaire));
		}

		final Map<Integer, ReqDesPartiePrenante> partiesPrenantes = extractPartiesPrenantes(data.getStakeholder(), infraService);
		final List<ReqDesTransactionImmobiliere> transactions = extractTransactionsImmobilieres(data.getTransaction());

		final List<Set<Integer>> idGroupes = composeGroupes(partiesPrenantes);
		final List<List<ReqDesPartiePrenante>> groupes = new ArrayList<>(idGroupes.size());
		for (Set<Integer> idGroupe : idGroupes) {
			final List<ReqDesPartiePrenante> groupe = new ArrayList<>(idGroupe.size());
			for (Integer id : idGroupe) {
				groupe.add(partiesPrenantes.get(id));
			}
			groupes.add(groupe);
		}
		final Map<Integer, List<Pair<RoleDansActe, Integer>>> roles = extractRoles(data.getTransaction());

		// persistence des données reçues avant traitement asynchrone
		final Set<Long> idsUnitesTraitement = persistData(xmlContent, doublon, noAffaire, data.getNotarialDeed(), data.getNotarialInformation(), transactions, groupes, roles);
		lancementTraitementAsynchrone(idsUnitesTraitement);
	}

	protected void lancementTraitementAsynchrone(Set<Long> idsUnitesTraitement) {
		for (long id : idsUnitesTraitement) {
			processor.postUniteTraitement(id);
		}
	}

	/**
	 * Persiste les données en base et renvoie les identifiants des unités de traitement
	 * @param xmlContent contenu du message XML sous forme de chaîne de caractères
	 * @param doublon <code>true</code> si un acte avec les mêmes coordonnées a déjà été reçu auparavant
	 * @param noAffaire numéro unique d'affaire au niveau de ReqDes
	 * @param acteAuthentique données de l'acte
	 * @param operateurs données sur le notaire et l'opérateur
	 * @param transactions transactions immobilières présentes dans l'acte
	 * @param groupes groupes de parties prenantes qui constituent des unités de traitement
	 * @param roles les rôles des différentes parties prenantes
	 * @return l'ensemble des identifiants des unités de traitement générées
	 */
	private Set<Long> persistData(final String xmlContent, final boolean doublon, final long noAffaire,
	                              final NotarialDeed acteAuthentique, final NotarialInformation operateurs,
	                              final List<ReqDesTransactionImmobiliere> transactions,
	                              final List<List<ReqDesPartiePrenante>> groupes,
	                              final Map<Integer, List<Pair<RoleDansActe, Integer>>> roles) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {

				// on crée d'abord l'événement lui-même
				final EvenementReqDes evt = buildEvenementReqDes(acteAuthentique, operateurs, doublon, noAffaire, xmlContent);

				// toutes les autres entités seront créées avec un visa spécifique à l'événement
				AuthenticationHelper.pushPrincipal(String.format("ReqDes-%d", evt.getId()));
				try {
					// les transactions immobilières (dans le même ordre que ce que donne le message entrant)
					final List<TransactionImmobiliere> transImmobilieres = new ArrayList<>(transactions.size());
					for (ReqDesTransactionImmobiliere t : transactions) {
						transImmobilieres.add(buildTransactionImmobiliere(evt, t));
					}

					final Set<Long> ids = new HashSet<>(groupes.size());

					// on peut maintenant créer les unités de traitement
					for (List<ReqDesPartiePrenante> groupe : groupes) {
						final UniteTraitement ut = buildUniteTraitement(evt);

						// chaque unité de traitement correspond à une ou deux parties prenantes
						final Map<Integer, PartiePrenante> partiesPrenantes = new HashMap<>(groupe.size());
						for (ReqDesPartiePrenante src : groupe) {
							final PartiePrenante pp = buildPartiePrenanteNue(ut, src);
							ajouterRoles(pp, transImmobilieres, roles.get(src.getId()));
							partiesPrenantes.put(src.getId(), pp);
						}

						tisserLiensConjoint(groupe, partiesPrenantes);

						ut.setPartiesPrenantes(new HashSet<>(partiesPrenantes.values()));
						ids.add(ut.getId());
					}

					// un petit flush de la session pour s'assurer que tout le monde est bien en base avant le reset de l'authentification
					hibernateTemplate.flush();

					return ids;
				}
				finally {
					AuthenticationHelper.popPrincipal();
				}
			}
		});
	}

	private EvenementReqDes buildEvenementReqDes(NotarialDeed acteAuthentique, NotarialInformation operateurs, boolean doublon, long noAffaire, String xml) {
		final EvenementReqDes evt = new EvenementReqDes();
		evt.setXml(xml);
		evt.setDoublon(doublon);
		evt.setDateActe(XmlUtils.xmlcal2regdate(acteAuthentique.getReferenceDate()));
		evt.setNoAffaire(noAffaire);
		evt.setNumeroMinute(acteAuthentique.getDealNumber());
		evt.setNotaire(buildInformationActeur(operateurs.getSollicitor()));
		evt.setOperateur(buildInformationActeur(operateurs.getOperator()));
		return hibernateTemplate.merge(evt);
	}

	private TransactionImmobiliere buildTransactionImmobiliere(EvenementReqDes evt, ReqDesTransactionImmobiliere rdTransaction) {
		final TransactionImmobiliere ti = new TransactionImmobiliere();
		ti.setDescription(rdTransaction.getDescription());
		ti.setModeInscription(rdTransaction.getModeInscription().toCore());
		ti.setTypeInscription(rdTransaction.getTypeInscription().toCore());
		ti.setOfsCommune(rdTransaction.getOfsCommune());
		ti.setEvenementReqDes(evt);
		return hibernateTemplate.merge(ti);
	}

	private UniteTraitement buildUniteTraitement(EvenementReqDes evt) {
		final UniteTraitement ut = new UniteTraitement();
		ut.setEtat(EtatTraitement.A_TRAITER);
		ut.setEvenement(evt);
		return hibernateTemplate.merge(ut);
	}

	private static void tisserLiensConjoint(List<ReqDesPartiePrenante> groupe, Map<Integer, PartiePrenante> partiesPrenantes) {
		for (ReqDesPartiePrenante src : groupe) {
			if (src.getPartner() != null) {
				final Integer idLink = src.getPartner().getLink();
				if (idLink != null) {
					final PartiePrenante db = partiesPrenantes.get(src.getId());
					final PartiePrenante linked = partiesPrenantes.get(idLink);
					db.setConjointPartiePrenante(linked);
				}
			}
		}
	}

	private static void ajouterRoles(PartiePrenante pp, List<TransactionImmobiliere> transactions, List<Pair<RoleDansActe, Integer>> pairs) {
		if (pairs != null && !pairs.isEmpty()) {
			final Set<RolePartiePrenante> set = new HashSet<>(pairs.size());
			for (Pair<RoleDansActe, Integer> role : pairs) {
				final RolePartiePrenante rpp = new RolePartiePrenante();
				rpp.setRole(role.getLeft().toCore());
				rpp.setTransaction(transactions.get(role.getRight()));
				set.add(rpp);
			}
			pp.setRoles(set);
		}
	}

	private PartiePrenante buildPartiePrenanteNue(UniteTraitement ut, ReqDesPartiePrenante src) {
		final PartiePrenante pp = new PartiePrenante();
		pp.setNom(src.getNomPrenom().getNom());
		pp.setNomNaissance(src.getNomNaissance());
		pp.setPrenoms(src.getNomPrenom().getPrenom());
		pp.setDateNaissance(src.getDateNaissance());
		pp.setSexe(src.getSexe());
		pp.setDateDeces(src.getDateDeces());
		pp.setSourceCivile(src.isSourceCivile());
		pp.setNumeroContribuable(src.getNoCtb());
		pp.setAvs(src.getNoAvs());
		if (src.getNomPrenomMere() != null) {
			pp.setNomMere(src.getNomPrenomMere().getNom());
			pp.setPrenomsMere(src.getNomPrenomMere().getPrenom());
		}
		if (src.getNomPrenomPere() != null) {
			pp.setNomPere(src.getNomPrenomPere().getNom());
			pp.setPrenomsPere(src.getNomPrenomPere().getPrenom());
		}
		pp.setEtatCivil(EtatCivilHelper.civil2core(src.getEtatCivil()));
		pp.setDateEtatCivil(src.getDateDebutEtatCivil());
		pp.setDateSeparation(src.getDateSeparation());
		if (src.getNationalite() != null) {
			pp.setOfsPaysNationalite(src.getNationalite().getPays().getNoOFS());
		}
		if (src.getPermis() != null) {
			pp.setCategorieEtranger(CategorieEtranger.valueOf(src.getPermis().getTypePermis()));
		}
		if (src.getOrigine() != null) {
			pp.setOrigine(new OriginePersonnePhysique(src.getOrigine().getNomLieu(), src.getOrigine().getSigleCanton()));
		}
		if (src.getPartner() != null) {
			final NomPrenom nomPrenom = src.getPartner().getNomPrenom();
			if (nomPrenom != null) {
				pp.setNomConjoint(nomPrenom.getNom());
				pp.setPrenomConjoint(nomPrenom.getPrenom());
			}
		}
		if (src.getAdresseResidence() != null) {
			final Adresse adresse = src.getAdresseResidence();
			if (adresse.getCasePostale() != null) {
				pp.setTexteCasePostale(adresse.getCasePostale().getType().format());
				pp.setCasePostale(adresse.getCasePostale().getNumero());
			}
			pp.setLocalite(adresse.getLocalite());
			pp.setNumeroOrdrePostal(adresse.getNumeroOrdrePostal());
			pp.setNumeroPostal(adresse.getNumeroPostal());
			pp.setNumeroPostalComplementaire(StringUtils.isNotBlank(adresse.getNumeroPostalComplementaire()) ? Integer.valueOf(adresse.getNumeroPostalComplementaire()) : null);
			pp.setOfsPays(adresse.getNoOfsPays());
			pp.setRue(adresse.getRue());
			pp.setNumeroMaison(adresse.getNumero());
			pp.setNumeroAppartement(adresse.getNumeroAppartement());
			pp.setTitre(adresse.getTitre());
			pp.setOfsCommune(adresse.getNoOfsCommuneAdresse());
		}
		pp.setUniteTraitement(ut);
		return hibernateTemplate.merge(pp);
	}

	private static InformationsActeur buildInformationActeur(Actor actor) {
		if (actor == null) {
			return null;
		}
		return new InformationsActeur(actor.getVisa(), actor.getName(), actor.getFirstName());
	}

	protected CreationModification parse(Source xml) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return (CreationModification) ((JAXBElement) u.unmarshal(xml)).getValue();
	}

	private Schema getRequestSchema() throws SAXException, IOException {
		if (schemaCache == null) {
			buildRequestSchema();
		}
		return schemaCache;
	}

	private synchronized void buildRequestSchema() throws SAXException, IOException {
		if (schemaCache == null) {
			final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			sf.setResourceResolver(new ClasspathCatalogResolver());
			final Source[] source = getClasspathSources("event/reqdes/creation-modification-contribuables-1.xsd");
			schemaCache = sf.newSchema(source);
		}
	}

	private static Source[] getClasspathSources(String... pathes) throws IOException {
		final Source[] sources = new Source[pathes.length];
		for (int i = 0, pathLength = pathes.length; i < pathLength; i++) {
			final String path = pathes[i];
			sources[i] = new StreamSource(new ClassPathResource(path).getURL().toExternalForm());
		}
		return sources;
	}

	protected static List<ReqDesTransactionImmobiliere> extractTransactionsImmobilieres(List<Transaction> transactions) throws EsbBusinessException {
		final List<ReqDesTransactionImmobiliere> list = new ArrayList<>(transactions.size());
		for (Transaction t : transactions) {
			list.addAll(buildTransactionsImmobilieres(t));
		}
		return list;
	}

	private static List<ReqDesTransactionImmobiliere> buildTransactionsImmobilieres(Transaction transaction) throws EsbBusinessException {
		final ModeInscriptionDansActe mode = ModeInscriptionDansActe.valueOf(transaction.getInscriptionMode());
		final TypeInscriptionDansActe type = TypeInscriptionDansActe.valueOf(transaction.getInscriptionType());
		final List<Integer> ofsCommunes = transaction.getMunicipalityId();
		final List<ReqDesTransactionImmobiliere> liste = new ArrayList<>(ofsCommunes.size());
		for (int ofsCommune : ofsCommunes) {
			liste.add(new ReqDesTransactionImmobiliere(transaction.getDescription(), ofsCommune, mode, type));
		}
		return liste;
	}

	protected static Map<Integer, ReqDesPartiePrenante> extractPartiesPrenantes(List<Stakeholder> stakeholders, ServiceInfrastructureService infraService) throws EsbBusinessException {
		final Map<Integer, ReqDesPartiePrenante> map = new HashMap<>(stakeholders.size());
		for (Stakeholder sh : stakeholders) {
			map.put(sh.getId(), buildPartiePrenante(sh, infraService));
		}
		return map;
	}

	private static ReqDesPartiePrenante buildPartiePrenante(Stakeholder sh, ServiceInfrastructureService infraService) throws EsbBusinessException {
		final ReqDesPartiePrenante pp = new ReqDesPartiePrenante(sh.getId());
		pp.setSourceCivile(sh.getRegistryOrigin().getCivil() != null);
		pp.setNoCtb(sh.getRegistryOrigin().getFiscal() != null ? (long) sh.getRegistryOrigin().getFiscal().getPartyNumber() : null);

		final Identity identity = sh.getIdentity();
		final FullName name = identity.getName();
		pp.setNomPrenom(new NomPrenom(name.getOfficialName(), name.getFirstNames()));
		pp.setNomNaissance(identity.getBirthName());
		pp.setSexe(EchHelper.sexeFromEch44(identity.getSex()));
		pp.setDateNaissance(DataHelper.xmlToCore(identity.getDateOfBirth()));
		if (identity.getVn() != null) {
			pp.setNoAvs(Long.toString(identity.getVn()));
		}

		final FullName motherName = identity.getMotherName();
		if (motherName != null) {
			pp.setNomPrenomMere(new NomPrenom(motherName.getOfficialName(), motherName.getFirstNames()));
		}
		final FullName fatherName = identity.getFatherName();
		if (fatherName != null) {
			pp.setNomPrenomPere(new NomPrenom(fatherName.getOfficialName(), fatherName.getFirstNames()));
		}

		pp.setDateDeces(DataHelper.xmlToCore(sh.getDateOfDeath()));

		final MaritalStatus maritalStatus = sh.getMaritalStatus();
		if (maritalStatus != null) {
			pp.setEtatCivil(EchHelper.etatCivilFromEch11(maritalStatus.getMaritalStatus(), maritalStatus.getCancelationReason()));
			pp.setDateDebutEtatCivil(DataHelper.xmlToCore(maritalStatus.getDateOfMaritalStatus()));
			pp.setDateSeparation(DataHelper.xmlToCore(maritalStatus.getSeparationDate()));

			final Partner partner = maritalStatus.getPartner();
			if (partner != null) {
				pp.setPartner(partner.getStakeholder() != null
						              ? new LienPartenaire(partner.getStakeholder().getStakeholderId())
						              : new LienPartenaire(new NomPrenom(partner.getName().getOfficialName(), partner.getName().getFirstNames())));
			}
		}

		final Adresse adrResidence = buildAdresseResidence(sh.getResidence(), infraService);
		pp.setAdresseResidence(adrResidence);

		pp.setNationalite(buildNationalite(sh.getNationality(), infraService));
		pp.setOrigine(buildOrigine(sh.getNationality()));
		pp.setPermis(buildPermis(sh.getNationality()));
		return pp;
	}

	private static Adresse buildAdresseResidence(Residence residence, ServiceInfrastructureService infraService) {
		final Adresse adrResidence;
		if (residence.getSwissResidence() != null) {
			adrResidence = new ReqDesAdresseResidence(residence.getSwissResidence());
		}
		else {
			adrResidence = new ReqDesAdresseResidence(residence.getForeignCountryResidence(), infraService);
		}
		return adrResidence;
	}

	private static Origine buildOrigine(Nationality nat) throws EsbBusinessException {
		final Origine origine;
		if (nat.getSwiss() != null && nat.getSwiss().getOrigin() != null) {
			final PlaceOfOrigin placeOfOrigin = nat.getSwiss().getOrigin();
			origine = new Origine() {
				@Override
				public String getNomLieu() {
					return placeOfOrigin.getOriginName();
				}

				@Override
				public String getSigleCanton() {
					return placeOfOrigin.getCanton().value();
				}
			};
		}
		else {
			origine = null;
		}
		return origine;
	}

	private static Nationalite buildNationalite(Nationality nat, ServiceInfrastructureService infraService) throws EsbBusinessException {
		final Pays pays;
		if (nat.getSwiss() != null) {
			pays = infraService.getPays(ServiceInfrastructureService.noOfsSuisse, null);
		}
		else if (nat.getStateless() != null) {
			pays = infraService.getPays(ServiceInfrastructureService.noPaysApatride, null);
		}
		else {
			pays = infraService.getPays(nat.getForeignCountry(), null);
			if (pays == null) {
				throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE,
				                               String.format("Pas de pays avec le code iso '%s'", nat.getForeignCountry()),
				                               null);
			}
		}
		return new ReqDesNationalite(pays);
	}

	private static Permis buildPermis(Nationality nat) throws EsbBusinessException {
		final TypePermis type = TypePermis.getFromEvd(nat.getResidencePermit());
		return type != null ? new ReqDesPermis(type) : null;
	}

	protected static List<Set<Integer>> composeGroupes(Map<Integer, ReqDesPartiePrenante> partiesPrenantes) throws EsbBusinessException {
		// au départ, on constitue un groupe par partie prenante, puis on regroupera ce qui est à regrouper

		// ici, on a un mapping de chaque identifiant de partie prenante vers le groupe auquel il appartient
		final Map<Integer, Set<Integer>> grp = new HashMap<>(partiesPrenantes.size());
		for (Integer key : partiesPrenantes.keySet()) {
			final Set<Integer> value = new HashSet<>(partiesPrenantes.size());
			value.add(key);
			grp.put(key, value);
		}

		// on regarde les liens entre les parties prenantes (liens maritaux) pour les regrouper
		for (ReqDesPartiePrenante pp : partiesPrenantes.values()) {
			final LienPartenaire partner = pp.getPartner();
			if (partner != null && partner.getLink() != null) {
				// on fusionne les deux groupes
				final Set<Integer> groupe = grp.get(pp.getId());
				final Set<Integer> groupePartenaire = grp.get(partner.getLink());
				groupe.addAll(groupePartenaire);
				grp.put(partner.getLink(), groupe);
			}
		}

		// on extrait les groupes distincts
		final Set<IdentityKey<Set<Integer>>> distincts = new HashSet<>(grp.size());
		for (Set<Integer> group : grp.values()) {
			distincts.add(new IdentityKey<>(group));
		}

		// on termine par récupérer les groupes eux-mêmes
		final List<Set<Integer>> groupes = new ArrayList<>(distincts.size());
		for (IdentityKey<Set<Integer>> key : distincts) {
			groupes.add(key.getElt());
		}
		return groupes;
	}

	/**
	 * En sortie, la map est indexée par identifiant de partie prenante, et les valeurs sont des couples role/index transaction (l'index de la
	 * transaction étant l'index dans la liste des transactions immobilières générées à partir des transactions passées en paramètre)
	 * @param transactions la liste des transactions présentes dans l'acte
	 * @return une map des rôles de chacune des parties prenantes
	 * @throws EsbBusinessException en cas de souci d'interprétation des données
	 */
	protected static Map<Integer, List<Pair<RoleDansActe, Integer>>> extractRoles(List<Transaction> transactions) throws EsbBusinessException {
		final Map<Integer, List<Pair<RoleDansActe, Integer>>> map = new HashMap<>();
		int index = 0;
		for (Transaction t : transactions) {
			for (Integer ofsCommune : t.getMunicipalityId()) {
				for (StakeholderReferenceWithRole sh : t.getStakeholder()) {
					final Pair<RoleDansActe, Integer> role = Pair.of(RoleDansActe.valueOf(sh.getRole()), index);
					final List<Pair<RoleDansActe, Integer>> roles = map.computeIfAbsent(sh.getStakeholderId(), k -> new LinkedList<>());
					roles.add(role);
				}
				++ index;
			}
		}
		return map;
	}
}
