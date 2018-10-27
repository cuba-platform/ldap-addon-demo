package com.haulmont.addon.ldap.core.dao;

import com.haulmont.addon.ldap.config.LdapPropertiesConfig;
import com.haulmont.addon.ldap.core.rule.LdapMatchingRuleContext;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.TypedQuery;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.haulmont.addon.ldap.core.dao.CubaUserDao.NAME;
import static java.util.stream.Collectors.toList;

@Component(NAME)
public class CubaUserDao {
    public final static String NAME = "ldap_CubaUserDao";

    @Inject
    private Persistence persistence;

    @Inject
    private Metadata metadata;

    @Inject
    private UserSynchronizationLogDao userSynchronizationLogDao;

    @Inject
    private DaoHelper daoHelper;

    @Inject
    private LdapPropertiesConfig ldapPropertiesConfig;

    @Transactional(readOnly = true)
    public User getCubaUserByLogin(String login) {
        TypedQuery<User> query = persistence.getEntityManager()
                .createQuery("select cu from sec$User cu where cu.login = :login", User.class);
        query.setParameter("login", login);
        query.setViewName("sec-user-view-with-group-roles");

        User cubaUser = query.getFirstResult();
        if (cubaUser == null) {
            cubaUser = metadata.create(User.class);
            cubaUser.setUserRoles(new ArrayList<>());
            cubaUser.setLogin(login);
        }
        return cubaUser;
    }

    @Transactional(readOnly = true)
    public List<String> getCubaUsersLoginsAndGroup() {
        List<String> groups = ldapPropertiesConfig.getCubaGroupForSynchronization();
        Boolean inverseGroups = ldapPropertiesConfig.getCubaGroupForSynchronizationInverse();
        TypedQuery<String> query = getCubaUsersLoginsAndGroupQuery(groups, inverseGroups);
        return query.getResultList();
    }

    @Transactional
    public void saveCubaUser(User cubaUser, User beforeRulesApplyUserState, LdapMatchingRuleContext ldapMatchingRuleContext) {
        if (beforeRulesApplyUserState.getActive() || cubaUser.getActive()) {
            EntityManager entityManager = persistence.getEntityManager();
            User mergedUser = daoHelper.persistOrMerge(cubaUser);
            List<Role> newRoles = mergedUser.getUserRoles().stream()
                    .map(UserRole::getRole)
                    .collect(toList());
            beforeRulesApplyUserState.getUserRoles().stream()
                    .filter(ur -> !newRoles.contains(ur.getRole()))
                    .forEach(entityManager::remove);
            mergedUser.getUserRoles().forEach(entityManager::persist);
        }
        userSynchronizationLogDao.logUserSynchronization(ldapMatchingRuleContext, beforeRulesApplyUserState);
    }

    @Transactional(readOnly = true)
    public List<User> getCubaUsersByLogin(List<String> logins) {
        TypedQuery<User> query = persistence.getEntityManager()
                .createQuery("select cu from sec$User cu where cu.login in :logins", User.class);
        query.setParameter("logins", logins);
        query.setViewName("sec-user-view-with-group-roles");

        return query.getResultList();
    }

    @Transactional
    public void save(User cubaUser) {
        daoHelper.persistOrMerge(cubaUser);
    }

    private TypedQuery<String> getCubaUsersLoginsAndGroupQuery(List<String> groups, Boolean inverseGroups) {
        String queryString = "select cu.login from sec$User cu";
        TypedQuery<String> query;
        if (CollectionUtils.isNotEmpty(groups)) {
            queryString = queryString + " inner join fetch cu.group cuGroup where upper(cuGroup.name) " +
                                          (inverseGroups ? "not in" : "in") + " :groups";
            query = persistence.getEntityManager().createQuery(queryString, String.class);
            query.setParameter("groups", groups.stream().map(String::toUpperCase).collect(toList()));
        } else {
            query = persistence.getEntityManager().createQuery(queryString, String.class);
        }

        return query;
    }
}
