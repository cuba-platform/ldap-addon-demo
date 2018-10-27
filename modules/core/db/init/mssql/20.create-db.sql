-- begin LDAP_MATCHING_RULE
alter table LDAP_MATCHING_RULE add constraint FK_LDAP_MATCHING_RULE_ON_ACCESS_GROUP foreign key (ACCESS_GROUP_ID) references SEC_GROUP(ID)^
alter table LDAP_MATCHING_RULE add constraint FK_LDAP_MATCHING_RULE_ON_MATCHING_RULE_STATUS foreign key (MATCHING_RULE_STATUS_ID) references LDAP_MATCHING_RULE_STATUS(ID)^
alter table LDAP_MATCHING_RULE add constraint FK_LDAP_MATCHING_RULE_ON_MATCHING_RULE_ORDER foreign key (MATCHING_RULE_ORDER_ID) references LDAP_MATCHING_RULE_ORDER(ID)^
create index IDX_LDAP_MATCHING_RULE_ON_ACCESS_GROUP on LDAP_MATCHING_RULE (ACCESS_GROUP_ID)^
create index IDX_LDAP_MATCHING_RULE_ON_MATCHING_RULE_STATUS on LDAP_MATCHING_RULE (MATCHING_RULE_STATUS_ID)^
create index IDX_LDAP_MATCHING_RULE_ON_MATCHING_RULE_ORDER on LDAP_MATCHING_RULE (MATCHING_RULE_ORDER_ID)^
-- end LDAP_MATCHING_RULE
-- begin LDAP_SIMPLE_RULE_CONDITION
alter table LDAP_SIMPLE_RULE_CONDITION add constraint FK_LDAP_SIMPLE_RULE_CONDITION_ON_SIMPLE_MATCHING_RULE foreign key (SIMPLE_MATCHING_RULE_ID) references LDAP_MATCHING_RULE(ID)^
create index IDX_LDAP_SIMPLE_RULE_CONDITION_ON_SIMPLE_MATCHING_RULE on LDAP_SIMPLE_RULE_CONDITION (SIMPLE_MATCHING_RULE_ID)^
-- end LDAP_SIMPLE_RULE_CONDITION
-- begin LDAP_MATCHING_RULE_ORDER
create unique index IDX_LDAP_MATCHING_RULE_ORDER_UNIQ_CUSTOM_MATCHING_RULE_ID on LDAP_MATCHING_RULE_ORDER (CUSTOM_MATCHING_RULE_ID, DELETE_TS) where CUSTOM_MATCHING_RULE_ID IS NOT NULL ^
-- end LDAP_MATCHING_RULE_ORDER
-- begin LDAP_MATCHING_RULE_STATUS
create unique index IDX_LDAP_MATCHING_RULE_STATUS_UNIQ_CUSTOM_MATCHING_RULE_ID on LDAP_MATCHING_RULE_STATUS (CUSTOM_MATCHING_RULE_ID, DELETE_TS) where CUSTOM_MATCHING_RULE_ID IS NOT NULL ^
-- end LDAP_MATCHING_RULE_STATUS
-- begin LDAP_MATCHING_RULE_ROLE_LINK
alter table LDAP_MATCHING_RULE_ROLE_LINK add constraint FK_MATRULROL_ON_ABSTRACT_DB_STORED_MATCHING_RULE foreign key (MATCHING_RULE_ID) references LDAP_MATCHING_RULE(ID)^
alter table LDAP_MATCHING_RULE_ROLE_LINK add constraint FK_MATRULROL_ON_ROLE foreign key (ROLE_ID) references SEC_ROLE(ID)^
-- end LDAP_MATCHING_RULE_ROLE_LINK
