package ch.vd.uniregctb.evenement.identification.contribuable;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.fiscalite.registre.identificationContribuable.DatePartielleType;
import ch.vd.fiscalite.registre.identificationContribuable.EnAttenteIdentifManuelType;
import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument;
import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument.IdentificationCTB;
import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument.IdentificationCTB.Demande.Demande2;
import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument.IdentificationCTB.Reponse;
import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument.IdentificationCTB.Reponse.Contribuable;
import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument.IdentificationCTB.Reponse.Erreur;
import ch.vd.fiscalite.registre.identificationContribuable.InformationAdresseType;
import ch.vd.fiscalite.registre.identificationContribuable.ModeIdentificationType;
import ch.vd.fiscalite.registre.identificationContribuable.TypeErreurType;
import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse.TypeAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.type.Sexe;

/**
 * Classe qui permet de traduire un {@link IdentificationContribuable} en un {@link IdentificationCTB}, et vice-versa.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class XmlEntityAdapter {

	public static final String TYPE_MESSAGE_NCS = "CS_EMPLOYEUR";
	public static final String TYPE_MESSAGE_EFACTURE = "CYBER_EFACTURE";
	public static final String TYPE_MESSAGE_LISTE_IS = "LISTE_IS";
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlEntityAdapter.class);

	private static final Map<String, TypeDemande> TYPES_DEMANDE_PAR_TYPE_MESSAGE = buildTypeDemandeParTypeMessageMap();

	@NotNull
	private static Map<String, TypeDemande> buildTypeDemandeParTypeMessageMap() {
		final Map<String, TypeDemande> map = new HashMap<>();
		map.put(TYPE_MESSAGE_NCS, TypeDemande.NCS);
		map.put(TYPE_MESSAGE_EFACTURE, TypeDemande.E_FACTURE);
		map.put(TYPE_MESSAGE_LISTE_IS, TypeDemande.IMPOT_SOURCE);
		return Collections.unmodifiableMap(map);
	}

	private static final Map<ModeIdentificationType.Enum, Demande.ModeIdentificationType> MODE_IDENTIFICATION_MAPPING = buildModeIdentificationMapping();

	@NotNull
	private static Map<ModeIdentificationType.Enum, Demande.ModeIdentificationType> buildModeIdentificationMapping() {
		final Map<ModeIdentificationType.Enum, Demande.ModeIdentificationType> map = new HashMap<>();
		map.put(ModeIdentificationType.SANS_MANUEL, Demande.ModeIdentificationType.SANS_MANUEL);
		map.put(ModeIdentificationType.MANUEL_SANS_ACK, Demande.ModeIdentificationType.MANUEL_SANS_ACK);
		map.put(ModeIdentificationType.MANUEL_AVEC_ACK, Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		return Collections.unmodifiableMap(map);
	}

	public static IdentificationCTBDocument entity2xml(IdentificationContribuable message) {

		final IdentificationCTBDocument document = IdentificationCTBDocument.Factory.newInstance();
		final IdentificationCTB xml = document.addNewIdentificationCTB();
		// la demande et le réponse ne doivent pas cohexister selon le xsd. Lorsque les deux éléments sont présent, on préfère la réponse puisque si elle est présente c'est que le demande est déjà connue.
		if (message.getReponse() != null) {
			entity2xml(message.getReponse(), xml.addNewReponse());
		}
		else if (message.getDemande() != null) {
			entity2xml(message.getDemande(), xml.addNewDemande());
		}
		return document;
	}

	private static void entity2xml(ch.vd.uniregctb.evenement.identification.contribuable.Reponse entity, Reponse xml) {
		if (entity.getNoContribuable() != null) {
			entity2xml(entity, xml.addNewContribuable());
		}
		xml.setDate(date2xml(entity.getDate()));
		if (entity.getErreur() != null) {
			entity2xml(entity.getErreur(), xml.addNewErreur());
		}
		if (entity.isEnAttenteIdentifManuel()) {
			xml.setEnAttenteIdentifManuel(EnAttenteIdentifManuelType.TRUE);
		}
	}

	private static void entity2xml(ch.vd.uniregctb.evenement.identification.contribuable.Erreur entity, Erreur xml) {
		xml.setType(entity2xml(entity.getType()));
		xml.setCode(entity.getCode());
		xml.setMessage(entity.getMessage());
	}

	private static ch.vd.fiscalite.registre.identificationContribuable.TypeErreurType.Enum entity2xml(final TypeErreur type) {
		if (type == null) {
			return null;
		}
		final ch.vd.fiscalite.registre.identificationContribuable.TypeErreurType.Enum t;
		switch (type) {
		case TECHNIQUE:
			t = TypeErreurType.Enum.forInt(1);
			break;
		case METIER:
			t = ch.vd.fiscalite.registre.identificationContribuable.TypeErreurType.Enum.forInt(2);
			break;
		default:
			throw new IllegalArgumentException("Type d'erreur inconnu = [" + type + ']');
		}
		return t;
	}

	private static Calendar date2xml(Date date) {
		final GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(date);
		return cal;
	}

	private static void entity2xml(ch.vd.uniregctb.evenement.identification.contribuable.Reponse entity, Contribuable xml) {
		xml.setNumeroContribuableIndividuel(entity.getNoContribuable().intValue());
		if (entity.getNoMenageCommun() != null) {
			xml.setNumeroContribuableCouple(entity.getNoMenageCommun().intValue()); // [UNIREG-1911]
		}
	}

	private static void entity2xml(Demande entity, IdentificationCTBDocument.IdentificationCTB.Demande xml) {
		entity2xml(entity, xml.addNewDemande());
		entity2xml(entity.getPersonne(), xml.addNewPersonne());
	}

	private static void entity2xml(CriteresPersonne entity,
	                               IdentificationCTBDocument.IdentificationCTB.Demande.Personne xml) {
		if (entity.getAdresse() != null) {
			entity2xml(entity.getAdresse(), xml.addNewAdresse());
		}
		if (entity.getDateNaissance() != null) {
			regdate2xml(entity.getDateNaissance(), xml.addNewDateNaissance());
		}
		if (entity.getNAVS11() != null) {
			xml.setNAVS11(Long.parseLong(entity.getNAVS11()));
		}
		if (entity.getNAVS13() != null) {
			xml.setNAVS13(Long.parseLong(entity.getNAVS13()));
		}
		xml.setNom(entity.getNom());
		xml.setPrenoms(entity.getPrenoms());
		xml.setSexe(entity2xml(entity.getSexe()));
	}

	private static void entity2xml(CriteresAdresse adresse, InformationAdresseType xml) {
		xml.setChiffreComplementaire(adresse.getChiffreComplementaire());
		xml.setLieu(adresse.getLieu());
		xml.setLigneAdresse1(adresse.getLigneAdresse1());
		xml.setLigneAdresse2(adresse.getLigneAdresse2());
		xml.setLocalite(adresse.getLocalite());
		xml.setNoAppartement(adresse.getNoAppartement());
		xml.setNoPolice(adresse.getNoPolice());
		xml.setNPAEtranger(adresse.getNpaEtranger());
		final Integer npaSuisse = adresse.getNpaSuisse();
		xml.setNPASuisse(npaSuisse == null ? 0 : npaSuisse);
		final Integer noOrdrePosteSuisse = adresse.getNoOrdrePosteSuisse();
		xml.setNPASuisseId(noOrdrePosteSuisse == null ? 0 : noOrdrePosteSuisse);
		final Integer numeroCasePostale = adresse.getNumeroCasePostale();
		xml.setNumeroCasePostale(numeroCasePostale == null ? 0 : numeroCasePostale);
		xml.setPays(adresse.getCodePays());
		xml.setRue(adresse.getRue());
		xml.setTexteCasePostale(adresse.getTexteCasePostale());
	}

	private static ch.vd.fiscalite.registre.identificationContribuable.SexeType.Enum entity2xml(Sexe sexe) {
		if (sexe == null) {
			return null;
		}
		switch (sexe) {
		case MASCULIN:
			return ch.vd.fiscalite.registre.identificationContribuable.SexeType.Enum.forInt(1);
		case FEMININ:
			return ch.vd.fiscalite.registre.identificationContribuable.SexeType.Enum.forInt(2);
		default:
			throw new IllegalArgumentException("Type de sexe inconnu = [" + sexe + ']');
		}
	}

	private static void regdate2xml(RegDate dateNaissance, DatePartielleType datePartielle) {

		final GregorianCalendar cal = new GregorianCalendar();
		cal.set(Calendar.YEAR, dateNaissance.year());

		if (dateNaissance.month() != RegDate.UNDEFINED) {
			cal.set(Calendar.MONTH, dateNaissance.month() - 1);

			if (dateNaissance.day() != RegDate.UNDEFINED) {
				cal.set(Calendar.DAY_OF_MONTH, dateNaissance.day());

				datePartielle.setAnneeMoisJour(cal);
			}
			else {
				datePartielle.setAnneeMois(cal);
			}
		}
		else {
			datePartielle.setAnnee(cal);
		}
	}

	private static void entity2xml(Demande entity, Demande2 xml) {
		xml.setEmetteurId(entity.getEmetteurId());
		xml.setMessageId(entity.getMessageId());
		xml.setPeriodeFiscale(entity2xml(entity.getPeriodeFiscale()));
		xml.setPrioriteEmetteur(entity.getPrioriteEmetteur() == PrioriteEmetteur.PRIORITAIRE);
		xml.setPrioriteUtilisateur(entity.getPrioriteUtilisateur());
		xml.setTypeMessage(entity.getTypeMessage());
	}

	private static Calendar entity2xml(int periodeFiscale) {
		final GregorianCalendar cal = new GregorianCalendar();
		cal.set(Calendar.YEAR, periodeFiscale);
		return cal;
	}

	public static IdentificationContribuable xml2entity(IdentificationCTB xml) throws XmlException {
		final IdentificationContribuable entity = new IdentificationContribuable();
		entity.setDemande(xml2entity(xml.getDemande()));
		return entity;
	}


	private static Demande xml2entity(ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument.IdentificationCTB.Demande xml) throws XmlException {
		if (xml == null) {
			return null;
		}
		final Demande entity = new Demande();
		final Date dateMessage = xml.getDemande().getDateMessage() != null ? xml.getDemande().getDateMessage().getTime() : DateHelper.getCurrentDate();
		entity.setDate(dateMessage);
		entity.setEmetteurId(xml.getDemande().getEmetteurId());
		entity.setMessageId(xml.getDemande().getMessageId());
		entity.setPeriodeFiscale(xml.getDemande().getPeriodeFiscale().get(GregorianCalendar.YEAR));
		entity.setPersonne(xml2entity(xml.getPersonne()));
		final PrioriteEmetteur prioriteEmetteur = xml.getDemande().getPrioriteEmetteur() ? PrioriteEmetteur.PRIORITAIRE : PrioriteEmetteur.NON_PRIORITAIRE;
		entity.setPrioriteEmetteur(prioriteEmetteur);

		entity.setModeIdentification(translateModeIdentification(xml.getDemande().getModeIdentification()));
		entity.setPrioriteUtilisateur(xml.getDemande().getPrioriteUtilisateur());
		entity.setTypeMessage(xml.getDemande().getTypeMessage());
		entity.setTransmetteur(xml.getDemande().getTransmetteur());
		final BigInteger montant = xml.getDemande().getMontant();

		if (montant != null) {
			entity.setMontant(montant.longValue());
		}

		final TypeDemande typeDemande = Optional.of(xml)
				.map(IdentificationCTB.Demande::getDemande)
				.map(Demande2::getTypeMessage)
				.map(TYPES_DEMANDE_PAR_TYPE_MESSAGE::get)
				.orElse(TypeDemande.MELDEWESEN);
		entity.setTypeDemande(typeDemande);

		return entity;
	}

	private static Demande.ModeIdentificationType translateModeIdentification(ModeIdentificationType.Enum modeIdentification) {
		return MODE_IDENTIFICATION_MAPPING.get(modeIdentification);
	}

	private static CriteresPersonne xml2entity(ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument.IdentificationCTB.Demande.Personne xml) throws XmlException {
		if (xml == null) {
			return null;
		}
		final CriteresPersonne entity = new CriteresPersonne();
		entity.setAdresse(xml2entity(xml.getAdresse()));
		final RegDate dateNaissance = xml2regdate(xml.getDateNaissance());
		entity.setDateNaissance(dateNaissance);
		if (xml.getNAVS11() > 0) {
			entity.setNAVS11(String.valueOf(xml.getNAVS11()));
		}
		if (xml.getNAVS13() > 0) {
			entity.setNAVS13(String.valueOf(xml.getNAVS13()));
		}
		entity.setNom(xml.getNom());
		entity.setPrenoms(xml.getPrenoms());
		entity.setSexe(xml2sexe(xml.getSexe()));
		return entity;
	}

	private static CriteresAdresse xml2entity(InformationAdresseType xml) {
		if (xml == null) {
			return null;
		}
		final CriteresAdresse entity = new CriteresAdresse();
		entity.setChiffreComplementaire(xml.getChiffreComplementaire());
		entity.setCodePays(xml.getPays());
		entity.setLieu(xml.getLieu());
		entity.setLigneAdresse1(xml.getLigneAdresse1());
		entity.setLigneAdresse2(xml.getLigneAdresse2());
		entity.setLocalite(xml.getLocalite());
		entity.setNoAppartement(xml.getNoAppartement());
		if (xml.getNPASuisse() > 0) {
			entity.setNpaSuisse(xml.getNPASuisse());
		}
		if (xml.getNPASuisseId() > 0) {
			entity.setNoOrdrePosteSuisse(xml.getNPASuisseId());
		}
		entity.setNoPolice(xml.getNoPolice());
		entity.setNpaEtranger(xml.getNPAEtranger());
		if (xml.getNumeroCasePostale() > 0) {
			entity.setNumeroCasePostale(xml.getNumeroCasePostale());
		}
		entity.setRue(xml.getRue());
		entity.setTexteCasePostale(xml.getTexteCasePostale());

		final TypeAdresse typeAdresse;
		if (xml.getNPAEtranger() != null) {
			typeAdresse = TypeAdresse.ETRANGERE;
		}
		else if (xml.getNPASuisse() > 0) {
			typeAdresse = TypeAdresse.SUISSE;
		}
		else {
			typeAdresse = null;
		}
		entity.setTypeAdresse(typeAdresse);

		return entity;
	}

	private static Sexe xml2sexe(ch.vd.fiscalite.registre.identificationContribuable.SexeType.Enum sexe) {
		if (sexe == null) {
			return null;
		}

		if (sexe.intValue() == 1) {
			return Sexe.MASCULIN;
		}
		else {
			Assert.isEqual(2, sexe.intValue());
			return Sexe.FEMININ;
		}
	}

	private static RegDate xml2regdate(DatePartielleType date) throws XmlException {
		if (date == null) {
			return null;
		}

		final Calendar anneeMoisJour = date.getAnneeMoisJour();
		if (anneeMoisJour != null) {
			final int year = anneeMoisJour.get(Calendar.YEAR);
			final int month = anneeMoisJour.get(Calendar.MONTH) + 1;
			final int day = anneeMoisJour.get(Calendar.DAY_OF_MONTH);
			try {
				return RegDateHelper.get(year, month, day, DateConstants.EXTENDED_VALIDITY_RANGE);
			}
			catch (IllegalArgumentException e) {
				LOGGER.warn("Date invalide (sera ignorée) : " + date, e);
				return null;
			}
		}

		final Calendar anneeMois = date.getAnneeMois();
		if (anneeMois != null) {
			final int year = anneeMois.get(Calendar.YEAR);
			final int month = anneeMois.get(Calendar.MONTH) + 1;
			try {
				return RegDateHelper.get(year, month, DateConstants.EXTENDED_VALIDITY_RANGE);
			}
			catch (IllegalArgumentException e) {
				LOGGER.warn("Date invalide (sera ignorée) : " + date, e);
				return null;
			}
		}

		final Calendar annee = date.getAnnee();
		try {
			Assert.notNull(annee);
			return RegDateHelper.get(annee.get(Calendar.YEAR), DateConstants.EXTENDED_VALIDITY_RANGE);
		}
		catch (IllegalArgumentException e) {
			LOGGER.warn("Date invalide (sera ignorée) : " + date, e);
			return null;
		}
	}

}
