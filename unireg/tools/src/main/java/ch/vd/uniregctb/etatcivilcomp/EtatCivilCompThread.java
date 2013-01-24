package ch.vd.uniregctb.etatcivilcomp;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.taxpayer.v1.FamilyStatus;
import ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus;
import ch.vd.unireg.xml.party.taxpayer.v1.Taxpayer;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.uniregctb.perfs.PerfsAccessFileIterator;

/**
 * Thread qui exécute un certain nombre de requêtes au web-service, puis s'arrête.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EtatCivilCompThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(EtatCivilCompThread.class);

	private long queryTime = 0;
	private int queryCount = 0;
	private int tiersCount = 0;
	private final PartyWebService service;

	private final PerfsAccessFileIterator ids;
	private int errorsCount;

	private final Query query;

	public static abstract class Query {

		protected final UserLogin login = new UserLogin();

		protected Set<PartyPart> parts = null;
		protected final FileWriter outputFilename;

		public Query(String operateur, int oid, FileWriter outputFilename) {
			this.outputFilename = outputFilename;
			login.setUserId(operateur);
			login.setOid(oid);
		}

		public void setParts(Set<PartyPart> parts) {
			this.parts = Collections.unmodifiableSet(parts);
		}

		public abstract Object execute(PartyWebService service, int id) throws WebServiceException;

		public abstract String description();

		protected String getOperateurDescription() {
			return login.getUserId() + '/' + login.getOid();
		}

		protected String getPartsDescription() {
			if (parts == null || parts.isEmpty()) {
				return "<none>";
			}
			else {
				String s = null;
				for (PartyPart e : parts) {
					if (s == null) {
						s = e.name();
					}
					else {
						s += '+' + e.name();
					}
				}
				return s;
			}
		}

		protected void log(String str) {
			if (outputFilename == null) {
				System.out.print(str);
			}
			else {
				try {
					outputFilename.write(str);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public static class CompareQuery extends Query {

		public CompareQuery(String operateur, int oid, FileWriter outputFilename) {
			super(operateur, oid, outputFilename);

			final Set<PartyPart> parts = new HashSet<PartyPart>();
			parts.add(PartyPart.RELATIONS_BETWEEN_PARTIES);
			parts.add(PartyPart.FAMILY_STATUSES);
			setParts(parts);
		}

		@Override
		public Object execute(PartyWebService service, int id) throws WebServiceException {

			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(id);
			params.getParts().addAll(parts);

			final Party party = service.getParty(params);
			final MaritalStatus etatCivil = getEtatCivil(party);
			final MaritalStatus etatFiscal = getPseudoEtatCivilFiscal(service, party);

			final String line = String.format("%s;%s;%s;%s;\n", String.valueOf(id), getPartyType(party), etatCivil == null ? StringUtils.EMPTY : etatCivil.name(),
					etatFiscal == null ? StringUtils.EMPTY : etatFiscal.name());
			log(line);

			return party;
		}

		private static MaritalStatus getEtatCivil(Party party) {
			if (party == null) {
				return null;
			}
			if (party instanceof Taxpayer) {
				final Taxpayer tp = (Taxpayer) party;
				final List<FamilyStatus> statuses = tp.getFamilyStatuses();
				if (statuses != null) {
					for (FamilyStatus s : statuses) {
						MaritalStatus maritalStatus = s.getMaritalStatus();
						if (s.getCancellationDate() == null && s.getDateTo() == null) {
							if (maritalStatus == MaritalStatus.REGISTERED_PARTNER) { // on ne fait pas de distinction entre mariés et pacsés
								maritalStatus = MaritalStatus.MARRIED;
							}
							if (maritalStatus == MaritalStatus.SEPARATED) { // on ne fait pas de distinction entre divorcés et séparés
								maritalStatus = MaritalStatus.DIVORCED;
							}
							return maritalStatus;
						}
					}
				}
			}
			return null;
		}

		private MaritalStatus getPseudoEtatCivilFiscal(PartyWebService service, Party party) throws WebServiceException {

			MaritalStatus etatFiscal = getPseudoEtatCivilFiscalDepuisAppartenancesMenage(party);

			// on corrige le pseudo état-civil déduit des appartenances ménages pour tenir compte des éventuels décès
			if (etatFiscal == MaritalStatus.DIVORCED && party instanceof NaturalPerson) {
				final NaturalPerson person = (NaturalPerson) party;
				final Date dateFermeture = getDateFermetureDerniereAppartenanceMenage(party);

				if (person.getDateOfDeath() != null && dateFermeture.equals(person.getDateOfDeath())) {
					// le ménage s'est fermé pour cause de décès de la personne, elle était donc mariée
					etatFiscal = MaritalStatus.MARRIED;
				}
				else {
					final NaturalPerson conjoint = getDernierConjoint(person, service);
					if (conjoint.getDateOfDeath() != null && dateFermeture.equals(conjoint.getDateOfDeath())) {
						// le ménage s'est fermé pour cause de décès du conjoint, la personne est donc veuve
						etatFiscal = MaritalStatus.WIDOWED;
					}
				}
			}

			return etatFiscal;
		}

		private static MaritalStatus getPseudoEtatCivilFiscalDepuisAppartenancesMenage(Party party) {
			if (party == null) {
				return null;
			}
			MaritalStatus maritalStatus = null;
			if (party instanceof NaturalPerson) {
				maritalStatus = MaritalStatus.SINGLE;
			}
			if (party instanceof Taxpayer) {
				final Taxpayer tp = (Taxpayer) party;
				final List<RelationBetweenParties> relations = tp.getRelationsBetweenParties();
				if (relations != null) {
					for (RelationBetweenParties r : relations) {
						if (r.getType() == RelationBetweenPartiesType.HOUSEHOLD_MEMBER && r.getCancellationDate() == null) {
							if (r.getDateTo() == null) {
								maritalStatus = MaritalStatus.MARRIED; // on ne fait pas de distinction entre mariés et pacsés
							}
							else {
								maritalStatus = MaritalStatus.DIVORCED; // on ne fait pas de distinction entre divorcés et séparés
							}
						}
					}
				}
			}
			return maritalStatus;
		}

		private static Date getDateFermetureDerniereAppartenanceMenage(Party party) {
			if (party == null) {
				return null;
			}
			if (party instanceof Taxpayer) {
				final Taxpayer tp = (Taxpayer) party;
				final List<RelationBetweenParties> relations = tp.getRelationsBetweenParties();
				if (relations != null) {
					for (RelationBetweenParties r : relations) {
						if (r.getType() == RelationBetweenPartiesType.HOUSEHOLD_MEMBER && r.getCancellationDate() == null) {
							return r.getDateTo();
						}
					}
				}
			}
			return null;
		}

		private NaturalPerson getDernierConjoint(NaturalPerson person, PartyWebService service) throws WebServiceException {
			final List<RelationBetweenParties> relations = person.getRelationsBetweenParties();
			if (relations == null || relations.isEmpty()) {
				return null;
			}

			Integer conjointId = null;
			for (int i = relations.size() - 1; i >= 0; i--) {
				final RelationBetweenParties rel = relations.get(i);
				if (rel.getType() == RelationBetweenPartiesType.HOUSEHOLD_MEMBER && rel.getCancellationDate() == null) {
					conjointId = rel.getOtherPartyNumber();
					break;
				}
			}

			if (conjointId == null) {
				return null;
			}

			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(conjointId);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);
			final CommonHousehold household = (CommonHousehold) service.getParty(params);
			if (household == null) {
				return null;
			}

			final NaturalPerson mainTaxpayer = household.getMainTaxpayer();
			final NaturalPerson secondaryTaxpayer = household.getSecondaryTaxpayer();
			return mainTaxpayer.getNumber() == person.getNumber() ? secondaryTaxpayer : mainTaxpayer;
		}

		private static String getPartyType(Party party) {
			return party == null ? StringUtils.EMPTY : party.getClass().getSimpleName();
		}

		@Override
		public String description() {
			return "operateur=" + getOperateurDescription() + ", parts=" + getPartsDescription();
		}
	}

	public EtatCivilCompThread(PartyWebService service, Query query, PerfsAccessFileIterator ids) {
		this.service = service;
		LOGGER.info("[thread-" + this.getId() + "] port is " + service.toString());
		this.query = query;
		this.ids = ids;
		this.errorsCount = 0;
	}

	@Override
	public void run() {

		int i = 0;

		for (Long id = ids.getNextId(); id != null; id = ids.getNextId()) {

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Traitement du tiers n°" + id);
			}

			if (++i % 100 == 0) {
				LOGGER.info("[thread-" + this.getId() + "] traité " + i + " tiers");
			}

			try {
				long before = System.nanoTime();

				// exécution de la requête
				Object tiers = query.execute(service, id.intValue());

				long after = System.nanoTime();
				long delta = after - before;
				queryTime += delta;
				++queryCount;
				++tiersCount;

				if (LOGGER.isTraceEnabled()) {
					long ping = delta / EtatCivilCompClient.NANO_TO_MILLI;
					if (tiers != null) {
						LOGGER.trace("Récupéré le tiers n°" + id + " de type " + tiers.getClass().getSimpleName() + " (" + ping + " ms)");
					}
					else {
						LOGGER.trace("Le tiers n°" + id + " n'existe pas (" + ping + " ms)");
					}
				}
			}
			catch (Exception e) {
				LOGGER.error("Impossible de récupérer le tiers n°" + id + ". Raison: " + e.getMessage(), e);
				errorsCount++;
			}
		}
	}

	public int getErrorsCount() {
		return errorsCount;
	}

	public long getQueryTime() {
		return queryTime;
	}

	public int getTiersCount() {
		return tiersCount;
	}

	public int getQueryCount() {
		return queryCount;
	}
}
