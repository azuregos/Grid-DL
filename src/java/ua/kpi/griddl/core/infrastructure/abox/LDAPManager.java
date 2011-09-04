/*
 * Copyright 2011 NTUU "KPI".
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ua.kpi.griddl.core.infrastructure.abox;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class LDAPManager {

    private DirContext ctx;

    public LDAPManager(String ldapServer, String ldapServerPort) throws Exception {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapServerPort);
        env.put(Context.SECURITY_AUTHENTICATION, "none");
        env.put(LdapContext.CONTROL_FACTORIES, "com.sun.jndi.ldap.ControlFactory");
        ctx = new InitialDirContext(env);
    }

    public NamingEnumeration<SearchResult> retriveSites() {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(null);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            return ctx.search("mds-vo-name=local, o=grid", "objectclass=GlueSite", ctls);
        } catch (NamingException ex) {
            Logger.getLogger(LDAPManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public NamingEnumeration<SearchResult> retriveServices() {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(null);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            return ctx.search("mds-vo-name=local, o=grid", "objectclass=GlueService", ctls);
        } catch (NamingException ex) {
            Logger.getLogger(LDAPManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public NamingEnumeration<SearchResult> retriveCEs() {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(null);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            return ctx.search("mds-vo-name=local, o=grid", "objectclass=GlueCE", ctls);
        } catch (NamingException ex) {
            Logger.getLogger(LDAPManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public NamingEnumeration<SearchResult> retriveVOViews() {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(null);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            return ctx.search("mds-vo-name=local, o=grid", "objectclass=GlueVOView", ctls);
        } catch (NamingException ex) {
            Logger.getLogger(LDAPManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public NamingEnumeration<SearchResult> retriveClusters() {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(null);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            return ctx.search("mds-vo-name=local, o=grid", "objectclass=GlueCluster", ctls);
        } catch (NamingException ex) {
            Logger.getLogger(LDAPManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public NamingEnumeration<SearchResult> retriveSubClusters() {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(null);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            return ctx.search("mds-vo-name=local, o=grid", "objectclass=GlueSubCluster", ctls);
        } catch (NamingException ex) {
            Logger.getLogger(LDAPManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public NamingEnumeration<SearchResult> retriveSEs() {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(null);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            return ctx.search("mds-vo-name=local, o=grid", "objectclass=GlueSE", ctls);
        } catch (NamingException ex) {
            Logger.getLogger(LDAPManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void close() {
        try {
            ctx.close();
        } catch (NamingException ex) {
            Logger.getLogger(LDAPManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
