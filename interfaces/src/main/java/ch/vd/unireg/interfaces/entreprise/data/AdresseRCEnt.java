package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;

import ch.ech.ech0010.v6.Country;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.Address;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;

public abstract class AdresseRCEnt<T extends AdresseRCEnt<T>> implements Serializable, Adresse, DateRangeLimitable<T> {

    private static final long serialVersionUID = 2308060557947322636L;

    private final RegDate dateDebut;
    private final RegDate dateFin;
    private final String localite;
    private final String numeroMaison;
    private final String numeroAppartement;
    private final Integer numeroOrdrePostal;
    private final String numeroPostal;
    private final String numeroPostalComplementaire;
    private final Integer noOfsPays;
    private final String rue;
    private final String titre;
    private final Integer egid;
    private final CasePostale casePostale;

    public AdresseRCEnt(RegDate dateDebut, RegDate dateFin, String localite, String numeroMaison, String numeroAppartement,
                        Integer numeroOrdrePostal, String numeroPostal, String numeroPostalComplementaire, Integer noOfsPays, String rue,
                        String titre, Integer egid, CasePostale casePostale) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.localite = localite;
        this.numeroMaison = numeroMaison;
        this.numeroAppartement = numeroAppartement;
        this.numeroOrdrePostal = numeroOrdrePostal;
        this.numeroPostal = numeroPostal;
        this.numeroPostalComplementaire = numeroPostalComplementaire;
        this.noOfsPays = noOfsPays;
        this.rue = rue;
        this.titre = titre;
        this.egid = egid;
        this.casePostale = casePostale;
    }

    protected AdresseRCEnt(RegDate dateDebut, RegDate dateFin, Address address) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin, ServiceEntrepriseException.class);

        this.localite = address.getAddressInformation().getTown();
        this.numeroMaison = address.getAddressInformation().getHouseNumber();
        this.numeroAppartement = address.getAddressInformation().getDwellingNumber();
        this.numeroOrdrePostal = address.getAddressInformation().getSwissZipCodeId();
        this.numeroPostal = initNPA(address);
        this.numeroPostalComplementaire = address.getAddressInformation().getSwissZipCodeAddOn();
        this.noOfsPays = initNoOfsPays(address.getAddressInformation().getCountry());
        this.rue = address.getAddressInformation().getStreet();

        // [SIFISC-13878] on prend en compte la deuxième ligne de complément si la première est vide
        this.titre = StringUtils.isBlank(address.getAddressInformation().getAddressLine1()) ? address.getAddressInformation().getAddressLine2() : address.getAddressInformation().getAddressLine1();

        // case postale
        if (StringUtils.isNotBlank(address.getAddressInformation().getPostOfficeBoxText()) || address.getAddressInformation().getPostOfficeBoxNumber() != null) {
            this.casePostale = new CasePostale(address.getAddressInformation().getPostOfficeBoxText(), address.getAddressInformation().getPostOfficeBoxNumber());
        }
        else {
            this.casePostale = null;
        }

        this.egid = toInt(address.getFederalBuildingId());
    }

    protected AdresseRCEnt(RegDate dateDebut, RegDate dateFin, AdresseRCEnt source) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin, ServiceEntrepriseException.class);

        this.localite = source.localite;
        this.numeroMaison = source.numeroMaison;
        this.numeroAppartement = source.numeroAppartement;
        this.numeroOrdrePostal = source.numeroOrdrePostal;
        this.numeroPostal = source.numeroPostal;
        this.numeroPostalComplementaire = source.numeroPostalComplementaire;
        this.noOfsPays = source.noOfsPays;
        this.rue = source.rue;
        this.titre = source.titre;
        this.egid = source.egid;
        this.casePostale = source.casePostale;
    }

    private static Integer toInt(Long l) {
        if (l == null) {
            return null;
        }
        if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return l.intValue();
        }
        throw new IllegalArgumentException("Value " + l + " not castable to int");
    }

    private static String initNPA(Address addressInfo) {
        final Long swissZipCode = addressInfo.getAddressInformation().getSwissZipCode();
        if (swissZipCode != null) {
            return String.valueOf(swissZipCode);
        }
        return addressInfo.getAddressInformation().getForeignZipCode();
    }

    private static Integer initNoOfsPays(Country country) {

        if (country == null) {
            return InfrastructureConnector.noOfsSuisse;
        }

        if (country.getCountryId() != null) {
            return country.getCountryId();
        }

        final String countryCode = country.getCountryIdISO2();
        if ("CH".equals(countryCode)) {
            // short path : 90% des adresses sont en Suisse
            return InfrastructureConnector.noOfsSuisse;
        }

        return null;
    }

    private static CasePostale initCasePostale(String text, Long number) {
        if (number == null && StringUtils.isBlank(text)) {
            return null;
        }
        return new CasePostale(text, number);
    }

    @Override
    public RegDate getDateDebut() {
        return dateDebut;
    }

    @Override
    public RegDate getDateFin() {
        return dateFin;
    }

    @Override
    public CasePostale getCasePostale() {
        return casePostale;
    }

    @Override
    public String getLocalite() {
        return localite;
    }

    @Override
    public String getNumero() {
        return numeroMaison;
    }

    @Override
    public Integer getNumeroOrdrePostal() {
        return numeroOrdrePostal;
    }

    @Override
    public String getNumeroPostal() {
        return numeroPostal;
    }

    @Override
    public String getNumeroPostalComplementaire() {
        return numeroPostalComplementaire;
    }

    @Override
    public Integer getNoOfsPays() {
        return noOfsPays;
    }

    @Override
    public String getRue() {
        return rue;
    }

    @Override
    public Integer getNumeroRue() {
        return null;
    }

    @Override
    public String getNumeroAppartement() {
        return numeroAppartement;
    }

    @Override
    public String getTitre() {
        return titre;
    }

    @Override
    public Integer getEgid() {
        return egid;
    }

    @Override
    public Integer getEwid() {
        return null;
    }

    @Nullable
    @Override
    public Localisation getLocalisationPrecedente() {
        return null;
    }

    @Nullable
    @Override
    public Localisation getLocalisationSuivante() {
        return null;
    }

    @Nullable
    @Override
    public Integer getNoOfsCommuneAdresse() {
        return null;
    }
}
