package ch.vd.unireg.xml.party.v5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.xml.party.agent.v1.Agent;
import ch.vd.unireg.xml.party.agent.v1.GeneralAgent;
import ch.vd.unireg.xml.party.agent.v1.SpecialAgent;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseMandataireAdapter;
import ch.vd.unireg.adresse.AdressesEnvoiHisto;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.address.AddressBuilder;

public class AgentBuilder {

	@Nullable
	public static Agent newAgent(AdresseMandataire adresse, Context contexte) throws AdresseException {
		final AdresseGenerique generique = new AdresseMandataireAdapter(adresse, contexte.infraService);
		final AdresseEnvoiDetaillee envoi = contexte.adresseService.buildAdresseEnvoi(generique.getSource().getTiers(), generique, adresse.getDateFin());
		switch (adresse.getTypeMandat()) {
		case GENERAL:
			return buildGeneralAgent(adresse, envoi, adresse.isWithCopy(), adresse.getPersonneContact(), adresse.getNoTelephoneContact());
		case SPECIAL:
			return buildSpecialAgent(adresse, envoi, adresse.isWithCopy(), adresse.getPersonneContact(), adresse.getNoTelephoneContact(), adresse.getCodeGenreImpot());
		case TIERS:
			return null;
		default:
			throw new IllegalArgumentException("Type de mandat non-supporté : " + adresse.getTypeMandat());
		}
	}

	@NotNull
	public static List<Agent> newAgents(Mandat mandat, Context contexte) throws AdresseException {
		final Tiers mandataire = contexte.tiersService.getTiers(mandat.getObjetId());
		final AdressesEnvoiHisto histo = contexte.adresseService.getAdressesEnvoiHisto(mandataire, false);
		final List<AdresseEnvoiDetaillee> envois = histo.ofType(TypeAdresseFiscale.REPRESENTATION);
		if (envois == null || envois.isEmpty()) {
			return Collections.emptyList();
		}
		final List<Agent> agents = new ArrayList<>(envois.size());
		for (AdresseEnvoiDetaillee envoi : envois) {
			final DateRange intersection = DateRangeHelper.intersection(envoi, mandat);
			if (intersection != null) {
				final Agent agent;
				switch (mandat.getTypeMandat()) {
				case GENERAL:
					agent = buildGeneralAgent(intersection, envoi, neverNull(mandat.getWithCopy(), false), mandat.getPersonneContact(), mandat.getNoTelephoneContact());
					break;
				case SPECIAL:
					agent = buildSpecialAgent(intersection, envoi, neverNull(mandat.getWithCopy(), false), mandat.getPersonneContact(), mandat.getNoTelephoneContact(), mandat.getCodeGenreImpot());
					break;
				case TIERS:
					agent = null;
					break;
				default:
					throw new IllegalArgumentException("Type de mandat non-supporté : " + mandat.getTypeMandat());
				}
				if (agent != null) {
					agents.add(agent);
				}
			}
		}
		return agents;
	}

	private static boolean neverNull(@Nullable Boolean bool, boolean defaultValue) {
		return bool != null ? bool : defaultValue;
	}

	private static GeneralAgent buildGeneralAgent(DateRange range, AdresseEnvoiDetaillee adresse, boolean withCopy, String personneContact, String telephoneContact) {
		return new GeneralAgent(DataHelper.coreToXMLv2(range.getDateDebut()),
		                        DataHelper.coreToXMLv2(range.getDateFin()),
		                        AddressBuilder.newPostAddressV3(adresse),
		                        withCopy,
		                        personneContact,
		                        telephoneContact,
		                        0,
		                        null);
	}

	private static SpecialAgent buildSpecialAgent(DateRange range, AdresseEnvoiDetaillee adresse, boolean withCopy, String personneContact, String telephoneContact, String codeGenreImpot) {
		return new SpecialAgent(DataHelper.coreToXMLv2(range.getDateDebut()),
		                        DataHelper.coreToXMLv2(range.getDateFin()),
		                        AddressBuilder.newPostAddressV3(adresse),
		                        withCopy,
		                        codeGenreImpot,
		                        personneContact,
		                        telephoneContact,
		                        0,
		                        null);
	}
}
