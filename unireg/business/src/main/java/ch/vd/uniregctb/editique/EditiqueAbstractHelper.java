package ch.vd.uniregctb.editique;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.editique.unireg.CTypeAdresse;
import ch.vd.editique.unireg.CTypeDestinataire;
import ch.vd.editique.unireg.CTypeExpediteur;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class EditiqueAbstractHelper {

	public static final Logger LOGGER = LoggerFactory.getLogger(EditiqueAbstractHelper.class);

	protected AdresseService adresseService;
	protected TiersService tiersService;
	protected ServiceInfrastructureService infraService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * @param adresseEnvoi l'adresse à laquelle on veut envoyer un document
	 * @param idEnvoiSiEtranger <code>true</code> si une adresse à l'étranger doit être en fait traitée par un idEnvoi
	 * @param valeurIdEnvoi valeur à utiliser
	 * @return un type d'affranchissement et, éventuellement, un idEnvoi à utiliser pour
	 */
	@NotNull
	protected static Pair<STypeZoneAffranchissement, String> getInformationsAffranchissement(AdresseEnvoiDetaillee adresseEnvoi, boolean idEnvoiSiEtranger, int valeurIdEnvoi) {

		final STypeZoneAffranchissement zoneAffranchissement;
		final String idEnvoi;
		if (adresseEnvoi.isIncomplete()) {
			idEnvoi = String.valueOf(ServiceInfrastructureService.noOIPM);
			zoneAffranchissement = STypeZoneAffranchissement.NA;
		}
		else if (adresseEnvoi.getTypeAffranchissement() == null) {
			idEnvoi = String.valueOf(ServiceInfrastructureService.noOIPM);
			zoneAffranchissement = STypeZoneAffranchissement.NA;
		}
		else {
			switch (adresseEnvoi.getTypeAffranchissement()) {
			case SUISSE:
				idEnvoi = null;
				zoneAffranchissement = STypeZoneAffranchissement.CH;
				break;
			case EUROPE:
				idEnvoi = idEnvoiSiEtranger ? String.valueOf(ServiceInfrastructureService.noOIPM) : null;
				zoneAffranchissement = idEnvoi != null ? STypeZoneAffranchissement.NA : STypeZoneAffranchissement.EU;
				break;
			case MONDE:
				idEnvoi = idEnvoiSiEtranger ? String.valueOf(ServiceInfrastructureService.noOIPM) : null;
				zoneAffranchissement = idEnvoi != null ? STypeZoneAffranchissement.NA : STypeZoneAffranchissement.RM;
				break;
			default:
				throw new IllegalArgumentException("Type d'affranchissement non supporté : " + adresseEnvoi.getTypeAffranchissement());
			}
		}

		return Pair.of(zoneAffranchissement, idEnvoi);
	}

	protected AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers) throws AdresseException {
		return adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
	}

	protected CTypeAdresse buildAdresse(CollectiviteAdministrative coll) throws AdresseException {
		final Tiers colAdm = tiersService.getCollectiviteAdministrative(coll.getNoColAdm());
		return buildAdresse(colAdm);
	}

	protected CTypeAdresse buildAdresse(Tiers tiers) throws AdresseException {
		final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		return buildAdresse(adresse);
	}

	@Nullable
	protected CTypeAdresse buildAdresse(AdresseEnvoi adresseEnvoi) {
		final List<String> lignes = new ArrayList<>(6);
		for (String ligne : adresseEnvoi.getLignes()) {
			if (StringUtils.isNotBlank(ligne)) {
				lignes.add(ligne);
			}
		}
		return lignes.isEmpty() ? null : new CTypeAdresse(lignes);
	}

	protected static CTypeInfoArchivage buildInfoArchivage(TypeDocumentEditique typeDocument, String cleArchivage, long noTiers, RegDate dateTraitement) {
		if (typeDocument.getCodeDocumentArchivage() == null) {
			throw new IllegalArgumentException("Archivage non-supporté pour le document de type " + typeDocument);
		}
		final CTypeInfoArchivage info = new CTypeInfoArchivage();
		info.setDatTravail(String.valueOf(dateTraitement.index()));
		info.setIdDocument(cleArchivage);
		info.setNomApplication(ConstantesEditique.APPLICATION_ARCHIVAGE);
		info.setNomDossier(FormatNumeroHelper.numeroCTBToDisplay(noTiers));
		info.setTypDocument(typeDocument.getCodeDocumentArchivage());
		info.setTypDossier(ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE);
		return info;
	}

	protected CTypeInfoEnteteDocument buildInfoEnteteDocument(DeclarationImpotOrdinairePM declaration, CollectiviteAdministrative expediteur) throws ServiceInfrastructureException, AdresseException {
		final CTypeInfoEnteteDocument entete = new CTypeInfoEnteteDocument();
		entete.setDestinataire(buildDestinataire(declaration.getTiers()));
		entete.setExpediteur(buildExpediteur(expediteur, declaration.getDateExpedition()));
		entete.setLigReference(null);
		entete.setPorteAdresse(null);
		return entete;
	}

	private CTypeDestinataire buildDestinataire(Contribuable ctb) throws AdresseException {
		final CTypeDestinataire destinataire = new CTypeDestinataire();
		destinataire.setAdresse(buildAdresse(ctb));
		destinataire.setNumContribuable(FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()));
		if (ctb instanceof Entreprise) {
			final String ide = FormatNumeroHelper.formatNumIDE(tiersService.getNumeroIDE((Entreprise) ctb));
			if (StringUtils.isNotBlank(ide)) {
				destinataire.getNumIDE().add(ide);
			}
		}
		return destinataire;
	}

	private CTypeExpediteur buildExpediteur(CollectiviteAdministrative ca, RegDate dateExpedition) throws ServiceInfrastructureException, AdresseException {
		final CTypeExpediteur expediteur = new CTypeExpediteur();
		final CTypeAdresse adresse = buildAdresse(ca);
		expediteur.setAdresse(adresse);
		expediteur.setAdrMes(ca.getAdresseEmail());
		expediteur.setDateExpedition(RegDateHelper.toIndexString(dateExpedition));
		expediteur.setLocaliteExpedition(ca.getAdresse().getLocalite());
		expediteur.setNumCCP(ca.getNoCCP());
		expediteur.setNumFax(ca.getNoFax());
		expediteur.setNumIBAN(null);
		expediteur.setNumTelephone(ca.getNoTelephone());
		expediteur.setTraitePar("");
		return expediteur;
	}
}
