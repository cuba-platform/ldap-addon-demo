package com.haulmont.addon.ldap.core.rule;

import com.haulmont.addon.ldap.dto.LdapUser;
import com.haulmont.addon.ldap.entity.CommonMatchingRule;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stores information about the LDAP synchronization process for the current user session.
 */
public class LdapMatchingRuleContext {

    /**
     * LDAP representation of the synchronized CUBA user.
     */
    private final LdapUser ldapUser;

    /**
     * Matching rules applied to the user.
     */
    private final Set<CommonMatchingRule> appliedRules = new LinkedHashSet<>();

    /**
     * Roles to be assigned to the user after the application of matching rules.
     */
    private final Set<Role> roles = new LinkedHashSet<>();

    /**
     * An access group to be assigned to the user after the application of matching rules.
     */
    private Group group;

    /**
     * A CUBA user that has its state synchronized in accordance with LDAP.
     */
    private final User cubaUser;

    /**
     * The value of this field defines whether the applied rule is terminal.<br>
     * If set to 'true', matching rule application for this context becomes restricted.
     */
    private boolean isTerminalRuleApply = false;

    public LdapMatchingRuleContext(LdapUser ldapUser, User cubaUser) {
        this.ldapUser = ldapUser;
        this.cubaUser = cubaUser;
    }

    public LdapUser getLdapUser() {
        return ldapUser;
    }

    public Set<CommonMatchingRule> getAppliedRules() {
        return appliedRules;
    }

    public User getCubaUser() {
        return cubaUser;
    }

    public boolean isTerminalRuleApply() {
        return isTerminalRuleApply;
    }

    public void setTerminalRuleApply(boolean terminalRuleApply) {
        this.isTerminalRuleApply = terminalRuleApply;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

}
