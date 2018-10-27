package com.haulmont.addon.ldap.core.spring.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LdapEventListener {

    @EventListener
    public void beforeUserUpdatedFromLdapEvent(BeforeUserRolesAndAccessGroupUpdatedFromLdapEvent event) {
        int t = 1;
    }

    @EventListener
    public void afterUserUpdatedFromLdapEvent(AfterUserRolesAndAccessGroupUpdatedFromLdapEvent event) {
        int t = 1;
    }

    @EventListener
    public void userCreatedFromLdap(UserCreatedFromLdapEvent event) {
        int t = 1;
    }

    @EventListener
    public void userCreatedFromLdap(UserDeactivatedFromLdapEvent event) {
        int t = 1;
    }

    @EventListener
    public void userCreatedFromLdap(UserActivatedFromLdapEvent event) {
        int t = 1;
    }
}
