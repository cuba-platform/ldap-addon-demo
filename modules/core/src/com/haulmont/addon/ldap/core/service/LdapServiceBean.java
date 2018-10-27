package com.haulmont.addon.ldap.core.service;

import com.haulmont.addon.ldap.config.LdapPropertiesConfig;
import com.haulmont.addon.ldap.core.dao.CubaUserDao;
import com.haulmont.addon.ldap.core.dao.LdapConfigDao;
import com.haulmont.addon.ldap.core.dao.LdapUserAttributeDao;
import com.haulmont.addon.ldap.core.dao.LdapUserDao;
import com.haulmont.addon.ldap.dto.LdapUser;
import com.haulmont.addon.ldap.core.rule.LdapMatchingRuleContext;
import com.haulmont.addon.ldap.core.spring.AnonymousLdapContextSource;
import com.haulmont.addon.ldap.core.utils.LdapHelper;
import com.haulmont.addon.ldap.dto.GroovyScriptTestResultDto;
import com.haulmont.addon.ldap.dto.LdapContextDto;
import com.haulmont.addon.ldap.entity.LdapConfig;
import com.haulmont.addon.ldap.service.LdapService;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.haulmont.addon.ldap.dto.GroovyScriptTestResult.*;
import static javax.naming.directory.SearchControls.SUBTREE_SCOPE;

@Service(LdapService.NAME)
public class LdapServiceBean implements LdapService {

    private final static String DUMMY_FILTER = "ou=system";

    private final Logger logger = LoggerFactory.getLogger(LdapServiceBean.class);

    @Inject
    private LdapUserAttributeDao ldapUserAttributeDao;

    @Inject
    private LdapUserDao ldapUserDao;

    @Inject
    private CubaUserDao cubaUserDao;

    @Inject
    private LdapConfigDao ldapConfigDao;

    @Inject
    private Scripting scripting;

    @Inject
    private Messages messages;

    @Inject
    private LdapPropertiesConfig ldapContextConfig;

    private LdapContextSource createAuthenticatedContext(String url, String base) {
        LdapContextSource ldapContextSource = new LdapContextSource();
        ldapContextSource.setUrl(url);
        ldapContextSource.setBase(base);
        ldapContextSource.afterPropertiesSet();
        return ldapContextSource;
    }

    private LdapContextSource createAnonymousContext(String url, String base) {
        LdapContextSource ldapContextSource = new AnonymousLdapContextSource();
        ldapContextSource.setUrl(url);
        ldapContextSource.setBase(base);
        ldapContextSource.setAnonymousReadOnly(true);
        ldapContextSource.afterPropertiesSet();
        return ldapContextSource;
    }

    @Override
    public String testConnection() {

        String url = ldapContextConfig.getContextSourceUrl();
        String base = ldapContextConfig.getContextSourceBase();
        String userDn = ldapContextConfig.getContextSourceUserName();
        String password = ldapContextConfig.getContextSourcePassword();

        LdapContextSource ldapContextSource = null;
        DirContext dirContext = null;
        try {
            if (StringUtils.isEmpty(userDn) && StringUtils.isEmpty(password)) {
                ldapContextSource = createAnonymousContext(url, base);
            } else {
                ldapContextSource = createAuthenticatedContext(url, base);
            }
            dirContext = ldapContextSource.getContext(userDn, password);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SUBTREE_SCOPE);
            searchControls.setCountLimit(1L);
            dirContext.search(StringUtils.EMPTY, DUMMY_FILTER, searchControls);//try to search dummy value in specified context base
            return "SUCCESS";
        } catch (Exception e) {
            return ExceptionUtils.getStackTrace(e);
        } finally {
            LdapUtils.closeContext(dirContext);
        }
    }

    @Override
    public void fillLdapUserAttributes(String schemaBase, String objectClasses, String objectClassName, String attributeClassName) {
        LdapContextSource ldapContextSource = null;
        DirContext dirContext = null;
        List<String> schemaAttributes = new ArrayList<>();
        String url = ldapContextConfig.getContextSourceUrl();
        String userDn = ldapContextConfig.getContextSourceUserName();
        String password = ldapContextConfig.getContextSourcePassword();
        try {
            ldapContextSource = createAuthenticatedContext(url, schemaBase);
            dirContext = ldapContextSource.getContext(userDn, password);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SUBTREE_SCOPE);
            searchControls.setTimeLimit(30_000);
            String filter = LdapHelper.createSchemaFilter(objectClasses, objectClassName);
            NamingEnumeration objectClassesResult = dirContext.search(StringUtils.EMPTY, filter, searchControls);
            while (objectClassesResult.hasMore()) {
                SearchResult searchResult = (SearchResult) objectClassesResult.next();
                Attributes attributes = searchResult.getAttributes();
                schemaAttributes.addAll(LdapHelper.getSchemaAttributes(attributes, attributeClassName.split(";")));
            }
            ldapUserAttributeDao.refreshLdapUserAttributes(schemaAttributes);
        } catch (Exception e) {
            throw new RuntimeException("Can't load LDAP schema", e);
        } finally {
            LdapUtils.closeContext(dirContext);
        }
    }

    @Override
    public List<String> getLdapUserAttributesNames() {
        return ldapUserAttributeDao.getLdapUserAttributesNames();
    }

    @Override
    public GroovyScriptTestResultDto testGroovyScript(String groovyScript, String login) {
        LdapUser ldapUser = ldapUserDao.getLdapUser(login);
        if (ldapUser == null) {
            return new GroovyScriptTestResultDto(NO_USER, null);
        }
        User cubaUser = cubaUserDao.getCubaUserByLogin(login);
        LdapMatchingRuleContext ldapMatchingRuleContext =
                new LdapMatchingRuleContext(ldapUser, cubaUser);

        Map<String, Object> context = new HashMap<>();
        context.put("__context__", ldapMatchingRuleContext);
        Object scriptExecutionResult = null;
        try {
            scriptExecutionResult = scripting.evaluateGroovy(groovyScript.replace("{ldapContext}", "__context__"), context);
        } catch (CompilationFailedException e) {
            logger.error(messages.formatMessage(LdapServiceBean.class, "errorDuringGroovyScriptEvaluation", login), e);
            return new GroovyScriptTestResultDto(COMPILATION_ERROR, ExceptionUtils.getFullStackTrace(e));
        } catch (Exception e) {
            logger.error(messages.formatMessage(LdapServiceBean.class, "errorDuringGroovyScriptEvaluation", login), e);
            return new GroovyScriptTestResultDto(OTHER_ERROR, ExceptionUtils.getFullStackTrace(e));
        }

        if (scriptExecutionResult instanceof Boolean) {
            Boolean bool = (Boolean) scriptExecutionResult;
            return bool ? new GroovyScriptTestResultDto(TRUE, null) : new GroovyScriptTestResultDto(FALSE, null);
        } else {
            return new GroovyScriptTestResultDto(NON_BOOLEAN_RESULT, null);
        }
    }

    @Override
    public LdapConfig getLdapConfig() {
        return ldapConfigDao.getLdapConfig();
    }

    private LdapContextDto getLdapContextConfig() {
        return new LdapContextDto(ldapContextConfig.getContextSourceUrl(),
                ldapContextConfig.getContextSourceUserName(),
                ldapContextConfig.getContextSourceBase());
    }
}
