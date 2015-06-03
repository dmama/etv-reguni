package ch.vd.uniregctb.interfaces.model;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.infrastructure.model.Rue;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.PaysImpl;
import ch.vd.uniregctb.interfaces.model.helper.EntrepriseHelper;
import ch.vd.uniregctb.type.TypeAdressePM;

public class AdresseEntrepriseImpl implements AdresseEntreprise, Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdresseEntrepriseImpl.class);

	private static final long serialVersionUID = 2830120339685146006L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final PaysImpl pays;
	private final String complement;
	private final Integer numeroTechniqueRue;
	private final String localiteAbregeMinuscule;
	private final String localiteCompletMinuscule;
	private final String numeroMaison;
	private final Integer numeroOrdrePostal;
	private final String numeroPostal;
	private final String numeroPostalComplementaire;
	private final String rue;
	private final TypeAdressePM type;

	private static final Map<Integer, Integer> LOCALITES_POSTALES_DISPARUES = buildMappingLocalitesPostalesDisparues();

	private static Map<Integer, Integer> buildMappingLocalitesPostalesDisparues() {
		final Map<Integer, Integer> map = new HashMap<>();

		// 1000 Lausanne
		map.put(106, 104);
		map.put(109, 104);
		map.put(113, 104);
		map.put(118, 104);
		map.put(124, 104);
		map.put(6207, 104);

		// 1200 Genève
		map.put(339, 329);
		map.put(348, 329);
		map.put(400, 329);

		// 1274 Signy
		map.put(481, 7420);

		// 1347 Le Solliat
		map.put(547, 7447);

		// 1400 Yverdon les bains
		map.put(593, 592);

		// 1613 La Rogivue -> 1613 Maracon
		map.put(908, 907);

		// 1700 Fribourg
		map.put(917, 916);

		// 1950 Sion
		map.put(1201, 1200);

		// 2075 Thielle-Wavre -> 2075 Thielle
		map.put(1328, 6675);

		// 2300 La Chaux-de-Fonds
		map.put(1365, 1364);

		// 2500 Bienne
		map.put(1407, 1406);

		// 3030 Berne
		map.put(1739, 5613);

		// 4025/4028/4001 Basel -> 4000 Basel
		map.put(2486, 2431);
		map.put(2488, 2431);
		map.put(6411, 2431);

		// 4571 Lüterkofen-Ichertswil -> 4571 Lüterkofen
		map.put(2771, 6274);

		// 5016 Obererlinsbach -> 5015 Erlinsbach SO
		map.put(2940, 2938);

		// 5424 Unterehrendingen -> 5420 Ehrendingen
		map.put(3063, 3061);

		// 8023/8035/8043 Zürich -> 8000 Zürich
		map.put(4429, 4384);
		map.put(4446, 4384);
		map.put(4456, 4384);

		// 8202 Schaffhausen -> 8200 Schaffhausen
		map.put(4566, 4559);

		// 9043 Trogen Kinderdorf Pestalozzi -> 9043 Trogen
		map.put(5232, 5231);

		// 9470 Buchs SG
		map.put(5375, 5374);

		// 1051 Le Mont sur Lausanne -> 1052 Le Mont-sur-Lausanne
		map.put(5733, 214);

		// 1588 Champmartin -> 1588 Cudrefin
		map.put(5838, 770);

		// 1042 Malapalud -> 1042 Assens
		map.put(5846, 208);

		// 1462 Arrissoules -> 1462 Yvonand
		map.put(5875, 669);

		// 1410 Corrençon -> 1410 Thierrens
		map.put(6188, 219);

		// 1623 La Rougève -> 1623 Semsales
		map.put(6200, 798);

		// 1806 St-Légier -> 1806 St-Légier-La Chiésaz
		map.put(6273, 1057);

		// 1218 Le Grand-Saconnex
		map.put(6501, 428);

		// 5015 Erlinsbach AG -> 5018 Erlinsbach
		map.put(6626, 7378);

		// 1196 Gland Filiale Colis -> 1196 Gland
		map.put(7484, 323);

		// 1897 Port-Valais -> 1897 Bouveret
		map.put(7802, 1148);

		// 3322 Schönbühl -> 3322 Urtenen-Schönbühl
		map.put(7806, 1956);

		return map;
	}

	/**
	 * Crée une adresse d'entreprise Unireg à partir de l'adresse d'entreprise Host-interfaces.
	 *
	 * @param target l'adresse d'entreprise Host-interfaces
	 * @param serviceInfrastructure le service infrastructure host-interface pour la résolution des numéros de rues
	 * @return l'adresse d'entreprise Unireg correspondante; ou <b>null</b> si l'adresse fournie est elle-même nulle ou si elle est située entièrement dans le futur (SIFISC-4625).
	 */
	@Nullable
	public static AdresseEntrepriseImpl get(ch.vd.registre.pm.model.AdresseEntreprise target, ServiceInfrastructure serviceInfrastructure) {
		if (target == null) {
			return null;
		}
		final RegDate today = RegDate.get();
		final AdresseEntrepriseImpl a = new AdresseEntrepriseImpl(target, today, serviceInfrastructure);
		if (a.getDateDebutValidite() != null && a.getDateDebutValidite().isAfter(today)) {
			// [SIFISC-4625] les adresses dans le futur sont ignorées
			return null;
		}
		return a;
	}

	private AdresseEntrepriseImpl(ch.vd.registre.pm.model.AdresseEntreprise target, RegDate today, ServiceInfrastructure serviceInfrastructure) {
		this.dateDebut = EntrepriseHelper.get(target.getDateDebutValidite());
		this.dateFin = initDateFin(target.getDateFinValidite(), today);
		this.pays = PaysImpl.get(target.getPays());
		this.complement = target.getComplement();
		this.localiteAbregeMinuscule = target.getLocaliteAbregeMinuscule();
		this.localiteCompletMinuscule = target.getLocaliteCompletMinuscule();
		this.numeroMaison = target.getNumeroMaison();
		this.numeroPostal = target.getNumeroPostal();
		this.numeroPostalComplementaire = target.getNumeroPostalComplementaire();
		this.type = initTypeAdresse(target.getType());
		if (target.getNumeroTechniqueRue() != null && target.getNumeroTechniqueRue() != 0) {
			try {
				final Rue rue = serviceInfrastructure.getRueByNumero(target.getNumeroTechniqueRue());
				this.rue = rue.getDesignationCourrier();
				this.numeroOrdrePostal = getNoOrdrePosteOfficiel(rue.getNoLocalite());
			}
			catch (RemoteException | InfrastructureException e) {
				throw new RuntimeException("Impossible de récupérer le libellé de la rue " + target.getNumeroTechniqueRue() + " dans le mainframe...", e);
			}
		}
		else {
			this.rue = target.getRue();
			this.numeroOrdrePostal = target.getNumeroOrdrePostal() == 0 ? null : getNoOrdrePosteOfficiel(target.getNumeroOrdrePostal());
		}
		this.numeroTechniqueRue = null;     // on ne veut plus de ces numéros de rue qui viennent du host!
	}

	private static int getNoOrdrePosteOfficiel(int noOrdrePosteMainframe) {

		// Dans le mainframe, il y avait des localités dont le numéro d'ordre postal a disparu avec RefInf (exemple : 339 / Genève)
		// -> il faut donc mettre en place un mapping des anciens vers les nouveaux (mapping en dur pour le moment, à voir s'il faut l'externaliser)

		final Integer mapping = LOCALITES_POSTALES_DISPARUES.get(noOrdrePosteMainframe);
		if (mapping != null && LOGGER.isDebugEnabled()) {
			LOGGER.debug("Localité postale " + noOrdrePosteMainframe + " disparue dans RefInf, remplacée par " + mapping);
		}
		return mapping == null ? noOrdrePosteMainframe : mapping;
	}

	private static TypeAdressePM initTypeAdresse(EnumTypeAdresseEntreprise type) {
		if (type == null) {
			return null;
		}
		if (type == EnumTypeAdresseEntreprise.COURRIER) {
			return TypeAdressePM.COURRIER;
		}
		else if (type == EnumTypeAdresseEntreprise.SIEGE) {
			return TypeAdressePM.SIEGE;
		}
		else if (type == EnumTypeAdresseEntreprise.FACTURATION) {
			return TypeAdressePM.FACTURATION;
		}
		else {
			throw new IllegalArgumentException("Type d'adresse PM inconnu = [" + type.getName() + ']');
		}
	}

	private static RegDate initDateFin(Date dateFinValidite, RegDate today) {
		final RegDate df = EntrepriseHelper.get(dateFinValidite);
		// [SIFISC-4625] les dates dans le futur sont ignorées
		return df == null || df.isAfter(today) ? null : df;
	}

	@Override
	public String getComplement() {
		return complement;
	}

	@Override
	public Integer getNumeroTechniqueRue() {
		return numeroTechniqueRue;
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public String getLocaliteAbregeMinuscule() {
		return localiteAbregeMinuscule;
	}

	@Override
	public String getLocaliteCompletMinuscule() {
		return localiteCompletMinuscule;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

	@Override
	public String getNumeroMaison() {
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
	public String getRue() {
		return rue;
	}

	@Override
	public TypeAdressePM getType() {
		return type;
	}

}
