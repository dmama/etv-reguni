package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0021.v1.Country;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseRCEnt implements Serializable, Adresse, DateRangeLimitable<AdresseRCEnt> {

    private static final long serialVersionUID = -137576466393353912L;

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
    private final TypeAdresseCivil typeAdresse;
    private final Integer egid;
    private final CasePostale casePostale;

    public static AdresseRCEnt get(DateRangeHelper.Ranged<Address> source) {
        if (source == null) {
            return null;
        }
        return new AdresseRCEnt(source.getDateDebut(), source.getDateFin(), source.getPayload());
    }

    public AdresseRCEnt limitTo(RegDate dateDebut, RegDate dateFin) {
        return new AdresseRCEnt(dateDebut == null ? this.dateDebut : dateDebut,
                                dateFin == null ? this.dateFin : dateFin,
                                this);
    }

    public AdresseRCEnt(RegDate dateDebut, RegDate dateFin, String localite, String numeroMaison, String numeroAppartement,
                        Integer numeroOrdrePostal, String numeroPostal, String numeroPostalComplementaire, Integer noOfsPays, String rue,
                        String titre, TypeAdresseCivil typeAdresse, Integer egid, CasePostale casePostale) {
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
        this.typeAdresse = typeAdresse;
        this.egid = egid;
        this.casePostale = casePostale;
    }

    private AdresseRCEnt(RegDate dateDebut, RegDate dateFin, Address address) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin, ServiceOrganisationException.class);

        this.localite = address.getTown();
        this.numeroMaison = address.getHouseNumber();
        this.numeroAppartement = address.getDwellingNumber();
        this.numeroOrdrePostal = address.getSwissZipCodeId();
        this.numeroPostal = initNPA(address);
        this.numeroPostalComplementaire = address.getSwissZipCodeAddOn();
        this.noOfsPays = initNoOfsPays(address.getCountry());
        this.rue = address.getStreet();

        // [SIFISC-13878] on prend en compte la deuxième ligne de complément si la première est vide
        this.titre = StringUtils.isBlank(address.getAddressLine1()) ? address.getAddressLine2() : address.getAddressLine1();

        // case postale
        if (StringUtils.isNotBlank(address.getPostOfficeBoxText()) || address.getPostOfficeBoxNumber() != null) {
            this.casePostale = new CasePostale(address.getPostOfficeBoxText(), address.getPostOfficeBoxNumber());
        }
        else {
            this.casePostale = null;
        }

        this.typeAdresse = TypeAdresseCivil.PRINCIPALE;
        this.egid = toInt(address.getFederalBuildingId());
    }

    private AdresseRCEnt(RegDate dateDebut, RegDate dateFin, AdresseRCEnt source) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin, ServiceOrganisationException.class);

        this.localite = source.localite;
        this.numeroMaison = source.numeroMaison;
        this.numeroAppartement = source.numeroAppartement;
        this.numeroOrdrePostal = source.numeroOrdrePostal;
        this.numeroPostal = source.numeroPostal;
        this.numeroPostalComplementaire = source.numeroPostalComplementaire;
        this.noOfsPays = source.noOfsPays;
        this.rue = source.rue;
        this.titre = source.titre;
        this.typeAdresse = source.typeAdresse;
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
        final Long swissZipCode = addressInfo.getSwissZipCode();
        if (swissZipCode != null) {
            return String.valueOf(swissZipCode);
        }
        return addressInfo.getForeignZipCode();
    }

    private static Integer initNoOfsPays(Country country) {

        if (country == null) {
            return ServiceInfrastructureRaw.noOfsSuisse;
        }

        if (country.getCountryId() != null) {
            return country.getCountryId();
        }

        final String countryCode = country.getCountryIdISO2();
        if ("CH".equals(countryCode)) {
            // short path : 90% des adresses sont en Suisse
            return ServiceInfrastructureRaw.noOfsSuisse;
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
    public boolean isValidAt(RegDate date) {
        return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
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
    public TypeAdresseCivil getTypeAdresse() {
        return typeAdresse;
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
