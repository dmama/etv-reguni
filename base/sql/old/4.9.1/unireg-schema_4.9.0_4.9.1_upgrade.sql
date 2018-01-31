-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.9.1', '4.9.0_4.9.1_upgrade');

--
-- [UNIREG-3134] date envoi de courrier pour la sommation des DI :
-- il ne fallait pas toucher au DI sommées avant la première MeP d'unireg
--

UPDATE ETAT_DECLARATION SET DATE_OBTENTION = DATE_ENVOI_COURRIER WHERE TYPE = 'SOMMEE' AND DATE_ENVOI_COURRIER < 20090713;