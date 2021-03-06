-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('3.4.4', '3.4_1_3.4.4_upgrade');

-- Table TACHE
ALTER TABLE TACHE add (QUALIFICATION nvarchar2(16));
create index IDX_TACHE_CTB_ID on TACHE (CTB_ID);

ALTER TABLE TACHE add (CA_ID number(19,0));
alter table TACHE add constraint FK_TACH_CA_ID foreign key (CA_ID) references TIERS;

-- Adresse de retour des DIs
ALTER TABLE TACHE add (DECL_ADRESSE_RETOUR nvarchar2(4));
ALTER TABLE DECLARATION add (DELAI_RETOUR_IMPRIME number(10,0));

--
-- Suite à la refactorisation des accès aux numéros de communes
--

-- Commune Vellerat (ID = 714, OFS = 6728), canton JU
UPDATE FOR_FISCAL SET NUMERO_OFS=6728, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=714 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Acquarossa (ID = 8072, OFS = 5048), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5048, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8072 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Lavizzara (ID = 8073, OFS = 5323), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5323, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8073 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Collina d'Oro (ID = 8074, OFS = 5236), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5236, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8074 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Alto Malcantone (ID = 8075, OFS = 5237), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5237, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8075 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Willisau (ID = 8076, OFS = 1151), canton LU
UPDATE FOR_FISCAL SET NUMERO_OFS=1151, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8076 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Vernay (ID = 8077, OFS = 2052), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2052, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8077 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Blenio (ID = 8078, OFS = 5049), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5049, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8078 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Forst-Längenbühl (ID = 8079, OFS = 948), canton BE
UPDATE FOR_FISCAL SET NUMERO_OFS=948, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8079 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Rapperswil-Jona (ID = 8080, OFS = 3340), canton SG
UPDATE FOR_FISCAL SET NUMERO_OFS=3340, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8080 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Erlinsbach (SO) (ID = 8081, OFS = 2503), canton SO
UPDATE FOR_FISCAL SET NUMERO_OFS=2503, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8081 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Ferrera (ID = 8082, OFS = 3713), canton GR
UPDATE FOR_FISCAL SET NUMERO_OFS=3713, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8082 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune St.Peter-Pagig (ID = 8083, OFS = 3931), canton GR
UPDATE FOR_FISCAL SET NUMERO_OFS=3931, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8083 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Cugnasco-Gerra (ID = 8084, OFS = 5138), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5138, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8084 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Avegno Gordevio (ID = 8085, OFS = 5324), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5324, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8085 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Neckertal (ID = 8086, OFS = 3378), canton SG
UPDATE FOR_FISCAL SET NUMERO_OFS=3378, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8086 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Mundaun (ID = 8087, OFS = 3617), canton GR
UPDATE FOR_FISCAL SET NUMERO_OFS=3617, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8087 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Tomils (ID = 8088, OFS = 3671), canton GR
UPDATE FOR_FISCAL SET NUMERO_OFS=3671, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8088 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Val Müstair (ID = 8089, OFS = 3847), canton GR
UPDATE FOR_FISCAL SET NUMERO_OFS=3847, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8089 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Tschiertschen-Praden (ID = 8090, OFS = 3932), canton GR
UPDATE FOR_FISCAL SET NUMERO_OFS=3932, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8090 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Obergoms (ID = 8091, OFS = 6076), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6076, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8091 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Gampel-Bratsch (ID = 8092, OFS = 6118), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6118, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8092 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Mörel-Filet (ID = 8093, OFS = 6203), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6203, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8093 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Steg-Hohtenn (ID = 8094, OFS = 6204), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6204, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8094 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Anniviers (ID = 8095, OFS = 6252), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6252, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8095 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune La Tène (ID = 8096, OFS = 6461), canton NE
UPDATE FOR_FISCAL SET NUMERO_OFS=6461, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8096 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Val-de-Travers (ID = 8097, OFS = 6512), canton NE
UPDATE FOR_FISCAL SET NUMERO_OFS=6512, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8097 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Erlen (ID = 4479, OFS = 4476), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4476, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4479 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Clos du Doubs (ID = 8098, OFS = 6808), canton JU
UPDATE FOR_FISCAL SET NUMERO_OFS=6808, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8098 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune La Baroche (ID = 8099, OFS = 6810), canton JU
UPDATE FOR_FISCAL SET NUMERO_OFS=6810, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8099 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Hohentannen (ID = 4492, OFS = 4495), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4495, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4492 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Sulgen (ID = 4510, OFS = 4506), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4506, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4510 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Diessenhofen (ID = 4541, OFS = 4545), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4545, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4541 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Basse-Allaine (ID = 8100, OFS = 6807), canton JU
UPDATE FOR_FISCAL SET NUMERO_OFS=6807, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8100 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Haute-Ajoie (ID = 8101, OFS = 6809), canton JU
UPDATE FOR_FISCAL SET NUMERO_OFS=6809, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8101 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Hüttlingen (ID = 4583, OFS = 4590), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4590, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4583 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Thundorf (ID = 4612, OFS = 4611), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4611, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4612 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Centovalli (ID = 8102, OFS = 5397), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5397, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8102 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Breggia (ID = 8103, OFS = 5269), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5269, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8103 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Langrickenbach (ID = 4678, OFS = 4681), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4681, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4678 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Bottighofen (ID = 4686, OFS = 4643), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4643, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4686 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Wäldi (ID = 4704, OFS = 4701), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4701, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4704 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Bettwiesen (ID = 4736, OFS = 4716), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4716, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4736 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Lommis (ID = 4738, OFS = 4741), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4741, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4738 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Eschlikon (ID = 4762, OFS = 4724), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4724, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4762 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Sirnach (ID = 4764, OFS = 4761), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4761, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4764 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Braunau (ID = 4771, OFS = 4723), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4723, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4771 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Pfyn (ID = 4837, OFS = 4841), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4841, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4837 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Wagenhausen (ID = 4873, OFS = 4871), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4871, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4873 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Berg (TG) (ID = 4892, OFS = 4891), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4891, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4892 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Birwinken (ID = 4902, OFS = 4901), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4901, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4902 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Donzhausen (ID = 4912, OFS = 4511), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4511, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4912 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Hessenreuti (ID = 4913, OFS = 4512), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4512, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4913 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Wigoltingen (ID = 4954, OFS = 4951), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4951, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=4954 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Lussery-Villars (ID = 5510, OFS = 5487), canton VD
UPDATE FOR_FISCAL SET NUMERO_OFS=5487, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=5510 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');
UPDATE DECLARATION SET NO_OFS_FOR_GESTION=5487, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NO_OFS_FOR_GESTION=5510;

-- 8000 représente une fraction de commune vaudoise (Le Sentier), on n'y touche pas...

-- 8001 représente une fraction de commune vaudoise (Le Brassus), on n'y touche pas...

-- 8002 représente une fraction de commune vaudoise (L'Orient), on n'y touche pas...

-- 8003 représente une fraction de commune vaudoise (Le Solliat), on n'y touche pas...

-- 8010 représente une fraction de commune vaudoise (Le Pont), on n'y touche pas...

-- 8011 représente une fraction de commune vaudoise (L'Abbaye), on n'y touche pas...

-- 8012 représente une fraction de commune vaudoise (Les Bioux), on n'y touche pas...

-- 8020 représente une fraction de commune vaudoise (Le Lieu), on n'y touche pas...

-- 8021 représente une fraction de commune vaudoise (Le Séchey), on n'y touche pas...

-- 8022 représente une fraction de commune vaudoise (Les Charbonnières), on n'y touche pas...

-- Commune Avry (ID = 8023, OFS = 2174), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2174, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8023 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Isorno (ID = 8024, OFS = 5137), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5137, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8024 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Capriasca (ID = 8025, OFS = 5226), canton TI
UPDATE FOR_FISCAL SET NUMERO_OFS=5226, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8025 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Haut-Intyamon (ID = 8026, OFS = 2121), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2121, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8026 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Suraua (ID = 8027, OFS = 3599), canton GR
UPDATE FOR_FISCAL SET NUMERO_OFS=3599, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8027 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Warth-Weiningen (ID = 8028, OFS = 4621), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4621, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8028 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Uesslingen-Buch (ID = 8029, OFS = 4616), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4616, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8029 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Amlikon-Bissegg (ID = 8030, OFS = 4881), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4881, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8030 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Lüterswil-Gächliwil (ID = 8031, OFS = 2456), canton SO
UPDATE FOR_FISCAL SET NUMERO_OFS=2456, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8031 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Bichelsee-Balterswil (ID = 8032, OFS = 4721), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4721, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8032 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Kemmental (ID = 8033, OFS = 4666), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4666, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8033 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Hauptwil-Gottshaus (ID = 8034, OFS = 4486), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4486, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8034 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Neunforn (ID = 8035, OFS = 4601), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4601, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8035 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Farvagny (ID = 8036, OFS = 2192), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2192, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8036 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Zihlschlacht-Sitterdorf (ID = 8037, OFS = 4511), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4511, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8037 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Misery-Courtion (ID = 8038, OFS = 2272), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2272, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8038 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Billens-Hennens (ID = 8039, OFS = 2063), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2063, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8039 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Lengwil (ID = 8040, OFS = 4683), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4683, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8040 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Wilen (TG) (ID = 8041, OFS = 4786), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4786, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8041 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Rickenbach (TG) (ID = 8042, OFS = 4751), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4751, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8042 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Tinizong-Rona (ID = 8043, OFS = 3541), canton GR
UPDATE FOR_FISCAL SET NUMERO_OFS=3541, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8043 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Schlatt (TG) (ID = 8044, OFS = 4546), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4546, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8044 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Basadingen-Schlattingen (ID = 8045, OFS = 4536), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4536, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8045 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Tobel-Tägerschen (ID = 8046, OFS = 4776), canton TG
UPDATE FOR_FISCAL SET NUMERO_OFS=4776, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8046 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Corpataux-Magnedens (ID = 8047, OFS = 2184), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2184, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8047 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Montagny (FR) (ID = 8048, OFS = 2029), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2029, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8048 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Guttet-Feschel (ID = 8049, OFS = 6117), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6117, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8049 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Grafschaft (ID = 8050, OFS = 6073), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6073, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8050 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune La Brillaz (ID = 8051, OFS = 2234), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2234, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8051 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Villorsonnens (ID = 8052, OFS = 2114), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2114, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8052 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Hauterive (FR) (ID = 8053, OFS = 2233), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2233, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8053 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Pont-en-Ogoz (ID = 8054, OFS = 2122), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2122, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8054 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Le Mouret (ID = 8055, OFS = 2220), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2220, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8055 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Le Glèbe (ID = 8056, OFS = 2223), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2223, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8056 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Riederalp (ID = 8057, OFS = 6181), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6181, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8057 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Le Flon (ID = 8058, OFS = 2337), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2337, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8058 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune La Sonnaz (ID = 8059, OFS = 2235), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2235, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8059 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Wichtrach (ID = 8060, OFS = 632), canton BE
UPDATE FOR_FISCAL SET NUMERO_OFS=632, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8060 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Torny (ID = 8061, OFS = 2115), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2115, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8061 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Les Montets (ID = 8062, OFS = 2050), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2050, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8062 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Bas-Intyamon (ID = 8063, OFS = 2162), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2162, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8063 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune La Verrerie (ID = 8064, OFS = 2338), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2338, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8064 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Wald (BE) (ID = 8065, OFS = 888), canton BE
UPDATE FOR_FISCAL SET NUMERO_OFS=888, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8065 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Delley-Portalban (ID = 8066, OFS = 2051), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2051, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8066 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune La Folliaz (ID = 8067, OFS = 2116), canton FR
UPDATE FOR_FISCAL SET NUMERO_OFS=2116, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8067 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Münster-Geschinen (ID = 8068, OFS = 6074), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6074, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8068 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Reckingen-Gluringen (ID = 8069, OFS = 6075), canton VS
UPDATE FOR_FISCAL SET NUMERO_OFS=6075, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8069 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Nesslau-Krummenau (ID = 8070, OFS = 3358), canton SG
UPDATE FOR_FISCAL SET NUMERO_OFS=3358, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8070 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

-- Commune Ehrendingen (ID = 8071, OFS = 4049), canton AG
UPDATE FOR_FISCAL SET NUMERO_OFS=4049, LOG_MUSER='[RattrapageCommunes]', LOG_MDATE=CURRENT_DATE WHERE NUMERO_OFS=8071 AND TYPE_AUT_FISC IN ('COMMUNE_OU_FRACTION_VD', 'COMMUNE_HC');

