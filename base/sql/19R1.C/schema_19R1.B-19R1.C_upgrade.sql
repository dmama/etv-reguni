-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('19R1.C', '19R1.B_19R1.C_upgrade');

--
-- FISCPROJ-983/FISCPROJ-1062 : rattrape des types de délai sur tous les délais
--

-- reset de tous les délais à 'explicite'
update DELAI_DOCUMENT_FISCAL
set TYPE_DELAI = 'EXPLICITE'
where DISCRIMINATOR = 'DELAI_DECLARATION';

-- forçage de tous les premiers délais à 'implicite' (sauf ceux déjà modifiés par e-Délai qui doivent rester à 'explicite')
update DELAI_DOCUMENT_FISCAL d
set d.TYPE_DELAI = 'IMPLICITE'
where d.ID in (
    -- retourne l'id du premier délai de la déclaration (sauf si modifié par e-Délai)
    select v.ID
    from (
             -- retourne la liste des délais par ordre croissant de traitement et numérotés
             select s.ID, s.LOG_MUSER, s.DATE_TRAITEMENT, ROW_NUMBER() over (order by s.DATE_TRAITEMENT asc) as ROW_INDEX
             from DELAI_DOCUMENT_FISCAL s
             where s.DISCRIMINATOR = 'DELAI_DECLARATION'
               and s.ANNULATION_DATE is null
               and s.DOCUMENT_FISCAL_ID = d.DOCUMENT_FISCAL_ID) v
    where v.ROW_INDEX = 1
      and v.LOG_MUSER != 'JMS-EvtDelaisDeclaration');
