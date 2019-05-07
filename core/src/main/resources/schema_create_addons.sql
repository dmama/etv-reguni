-- les contraintes de clés étrangères qu'il n'est plus possible de définir avec des annotations JPA
-- (parce qu'on référence directement l'id et pas l'entité associée)
alter table ADRESSE_TIERS add constraint FK_ADR_AT_TRS_ID foreign key (AUTRE_TIERS_ID) references TIERS;
alter table DOCUMENT_FISCAL add constraint FK_DECL_RET_COLL_ADMIN_ID foreign key (RETOUR_COLL_ADMIN_ID) references TIERS;
alter table RAPPORT_ENTRE_TIERS add constraint FK_RET_TRS_TUT_ID foreign key (TIERS_TUTEUR_ID) references TIERS;
alter table REQDES_PARTIE_PRENANTE add constraint FK_REQDES_PP_CTB_CREE foreign key (NO_CTB_CREE) references TIERS;
alter table SITUATION_FAMILLE add constraint FK_SIT_FAM_MC_CTB_ID foreign key (TIERS_PRINCIPAL_ID) references TIERS;
