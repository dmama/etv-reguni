-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('19R1.C', '19R1.B_19R1.C_upgrade');

-- FISCPROJ-983 : rattrapage du type de délai sur les premiers délais créés par des zaixxx
update DELAI_DOCUMENT_FISCAL
set TYPE_DELAI = 'IMPLICITE'
where ID in (select d.ID
             from DELAI_DOCUMENT_FISCAL d
                      left join (select min(id) as MIN_ID, DOCUMENT_FISCAL_ID
                                 from DELAI_DOCUMENT_FISCAL
                                 where DISCRIMINATOR = 'DELAI_DECLARATION'
                                   and ANNULATION_DATE is null
                                 group by DOCUMENT_FISCAL_ID)
                                on d.id = MIN_ID
             where TYPE_DELAI = 'EXPLICITE'
               and (LOG_CUSER like 'ZAI%' or LOG_CUSER like 'zai%'));
