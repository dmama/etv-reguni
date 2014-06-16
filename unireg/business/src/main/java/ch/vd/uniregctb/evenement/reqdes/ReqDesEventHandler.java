package ch.vd.uniregctb.evenement.reqdes;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
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
import ch.vd.unireg.xml.event.reqdes.v1.Subject;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.IdentityKey;
import ch.vd.uniregctb.common.XmlUtils;
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
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.xml.DataHelper;

public class ReqDesEventHandler implements EsbMessageHandler {

	private static final Logger LOGGER = Logger.getLogger(ReqDesEventHandler.class);
	private static final String VISA = "eReqDesEvent";

	private Schema schemaCache;

	private ServiceInfrastructureService infraService;
	private EvenementReqDesDAO evenementDAO;
	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;

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

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		final String businessId = message.getBusinessId();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Réception d'un message eReqDes {businessId='%s'}", businessId));
		}
		final long start = System.nanoTime();
		try {
			final Source src = message.getBodyAsSource();
			final String xml = message.getBodyAsString();

			AuthenticationHelper.pushPrincipal(VISA);
			try {
				onMessage(src, xml);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
		finally {
			if (LOGGER.isInfoEnabled()) {
				final long end = System.nanoTime();
				LOGGER.info(String.format("Réception du message eReqDes {businessId='%s'} traitée en %d ms", businessId, TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
		}
	}

	protected void onMessage(Source xml, String str) throws IOException, EsbBusinessException {
		final CreationModification data;
		try {
			data = parse(xml);
		}
		catch (SAXException | JAXBException e) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}

		// a-t-on déjà reçu ce message ?
		final long noAffaire = data.getNotarialDeed().getDealNumber();
		final EvenementReqDes dejaPresent = evenementDAO.findByNumeroMinute(noAffaire);
		if (dejaPresent != null) {
			LOGGER.warn(String.format("Un message ReqDes avec le même numéro de minute (%d) a déjà été reçu ; cette nouvelle réception est donc ignorée.", noAffaire));
		}
		else {
			final Map<Integer, ReqDesPartiePrenante> partiesPrenantes = extractPartiesPrenantes(data.getStakeholder(), infraService);
			final List<Set<Integer>> idGroupes = composeGroupes(partiesPrenantes);
			final List<List<ReqDesPartiePrenante>> groupes = new ArrayList<>(idGroupes.size());
			for (Set<Integer> idGroupe : idGroupes) {
				final List<ReqDesPartiePrenante> groupe = new ArrayList<>(idGroupe.size());
				for (Integer id : idGroupe) {
					groupe.add(partiesPrenantes.get(id));
				}
				groupes.add(groupe);
			}
			final Map<Integer, Set<Pair<RoleDansActe, Integer>>> roles = extractRoles(data.getSubject());

			// persistence des données reçues avant traitement asynchrone
			final Set<Long> idsUnitesTraitement = persistData(str, data.getNotarialDeed(), data.getNotarialInformation(), groupes, roles);
			lancementTraitementAsynchrone(idsUnitesTraitement);
		}
	}

	protected void lancementTraitementAsynchrone(Set<Long> idsUnitesTraitement) {
		// TODO poster les données sur la queue de traitement pour prise en charge asynchrone
	}

	/**
	 * Persiste les données en base et renvoie les identifiants des unités de traitement
	 * @param xmlContent contenu du message XML sous forme de chaîne de caractères
	 * @param acteAuthentique données de l'acte
	 * @param operateurs données sur le notaire et l'opérateur
	 * @param groupes groupes de parties prenantes qui constituent des unités de traitement
	 * @param roles les rôles des différentes parties prenantes
	 * @return l'ensemble des identifiants des unités de traitement générées
	 */
	private Set<Long> persistData(final String xmlContent, final NotarialDeed acteAuthentique, final NotarialInformation operateurs, final List<List<ReqDesPartiePrenante>> groupes, final Map<Integer, Set<Pair<RoleDansActe, Integer>>> roles) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {

				// on crée d'abord l'événement lui-même
				final EvenementReqDes evt = buildEvenementReqDes(acteAuthentique, operateurs, xmlContent);

				// toutes les autres entités seront créées avec un visa spécifique à l'événement
				AuthenticationHelper.pushPrincipal(String.format("eReqDes-%d", evt.getId()));
				try {
					final Set<Long> ids = new HashSet<>(groupes.size());

					// on commence donc par créer les unités de traitement
					for (List<ReqDesPartiePrenante> groupe : groupes) {
						final UniteTraitement ut = buildUniteTraitement(evt);

						// chaque unité de traitement correspond à une ou deux parties prenantes
						final Map<Integer, PartiePrenante> partiesPrenantes = new HashMap<>(groupe.size());
						for (ReqDesPartiePrenante src : groupe) {
							final PartiePrenante pp = buildPartiePrenanteNue(ut, src);
							ajouterRoles(pp, roles.get(src.getId()));
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

	private EvenementReqDes buildEvenementReqDes(NotarialDeed acteAuthentique, NotarialInformation operateurs, String xml) {
		final EvenementReqDes evt = new EvenementReqDes();
		evt.setXml(xml);
		evt.setDateActe(XmlUtils.xmlcal2regdate(acteAuthentique.getReferenceDate()));
		evt.setNumeroMinute(acteAuthentique.getDealNumber());
		evt.setNotaire(buildInformationActeur(operateurs.getSollicitor()));
		evt.setOperateur(buildInformationActeur(operateurs.getOperator()));
		return hibernateTemplate.merge(evt);
	}

	private UniteTraitement buildUniteTraitement(EvenementReqDes evt) {
		final UniteTraitement ut = new UniteTraitement();
		ut.setEtat(EtatTraitement.A_TRAITER);
		ut.setEvenement(evt);
		return hibernateTemplate.merge(ut);
	}

	private void tisserLiensConjoint(List<ReqDesPartiePrenante> groupe, Map<Integer, PartiePrenante> partiesPrenantes) {
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

	private static void ajouterRoles(PartiePrenante pp, Set<Pair<RoleDansActe, Integer>> pairs) {
		if (pairs != null && !pairs.isEmpty()) {
			final Set<RolePartiePrenante> set = new HashSet<>(pairs.size());
			for (Pair<RoleDansActe, Integer> role : pairs) {
				set.add(new RolePartiePrenante(role.getRight(), role.getLeft().toCore()));
			}
			pp.setRoles(set);
		}
	}

	private PartiePrenante buildPartiePrenanteNue(UniteTraitement ut, ReqDesPartiePrenante src) {
		final PartiePrenante pp = new PartiePrenante();
		pp.setNom(src.getNomPrenom().getNom());
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
			pp.setNoOfsPaysNationalite(src.getNationalite().getPays().getNoOFS());
		}
		if (src.getPermis() != null) {
			pp.setCategorieEtranger(CategorieEtranger.valueOf(src.getPermis().getTypePermis()));
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
			if (adresse.getNumeroOrdrePostal() > 0) {
				pp.setNumeroOrdrePostal(adresse.getNumeroOrdrePostal());
			}
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
		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
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
		pp.setSexe(EchHelper.sexeFromEch44(identity.getSex()));
		pp.setDateNaissance(DataHelper.xmlToCore(identity.getDateOfBirth()));
		pp.setNoAvs(Long.toString(identity.getVn()));

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
	 * En sortie, la map est indexée par identifiant de partie prenante, et les valeurs sont des couples role/ofsCommune
	 * @param subjects la liste des "immeubles" présents dans l'acte
	 * @return une map des rôles de chacune des parties prenantes
	 * @throws EsbBusinessException en cas de souci d'interprétation des données
	 */
	protected static Map<Integer, Set<Pair<RoleDansActe, Integer>>> extractRoles(List<Subject> subjects) throws EsbBusinessException {
		final Map<Integer, Set<Pair<RoleDansActe, Integer>>> map = new HashMap<>();
		for (Subject s : subjects) {
			final Integer ofsCommune = s.getMunicipalityId();
			for (StakeholderReferenceWithRole sh : s.getStakeholder()) {
				final Pair<RoleDansActe, Integer> role = Pair.of(RoleDansActe.valueOf(sh.getRole()), ofsCommune);
				Set<Pair<RoleDansActe, Integer>> roles = map.get(sh.getStakeholderId());
				if (roles == null) {
					roles = new HashSet<>();
					map.put(sh.getStakeholderId(), roles);
				}
				roles.add(role);
			}
		}
		return map;
	}
}
