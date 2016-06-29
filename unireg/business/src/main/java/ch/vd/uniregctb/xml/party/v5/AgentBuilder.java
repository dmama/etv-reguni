package ch.vd.uniregctb.xml.party.v5;

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
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataireAdapter;
import ch.vd.uniregctb.adresse.AdressesEnvoiHisto;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.address.AddressBuilder;

public class AgentBuilder {

	// TODO un jour, il faudra aller chercher ces informations dans le modèle...
	private static final String BIDON = "BIDON";
	private static final boolean WITH_COPY = true;

	@Nullable
	public static Agent newAgent(AdresseMandataire adresse, Context contexte) throws AdresseException {
		final AdresseGenerique generique = new AdresseMandataireAdapter(adresse, contexte.infraService);
		final AdresseEnvoiDetaillee envoi = contexte.adresseService.buildAdresseEnvoi(generique.getSource().getTiers(), generique, adresse.getDateFin());
		switch (adresse.getTypeMandat()) {
		case GENERAL:
			return buildGeneralAgent(adresse, envoi, WITH_COPY);
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
					agent = buildGeneralAgent(intersection, envoi, WITH_COPY);
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

	private static GeneralAgent buildGeneralAgent(DateRange range, AdresseEnvoiDetaillee adresse, boolean withCopy) {
		return new GeneralAgent(DataHelper.coreToXMLv2(range.getDateDebut()),
		                        DataHelper.coreToXMLv2(range.getDateFin()),
		                        AddressBuilder.newPostAddressV3(adresse),
		                        withCopy,
		                        null);
	}

	private static SpecialAgent buildSpecialAgent(DateRange range, AdresseEnvoiDetaillee adresse, boolean withCopy, String codeGenreImpot) {
		return new SpecialAgent(DataHelper.coreToXMLv2(range.getDateDebut()),
		                        DataHelper.coreToXMLv2(range.getDateFin()),
		                        AddressBuilder.newPostAddressV3(adresse),
		                        withCopy,
		                        codeGenreImpot,
		                        null);
	}
}
