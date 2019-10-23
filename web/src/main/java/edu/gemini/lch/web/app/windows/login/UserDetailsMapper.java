package edu.gemini.lch.web.app.windows.login;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.services.ConfigurationService;
import org.apache.log4j.Logger;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UserDetailsContextMapper and its implementations are the base to implement any application specific authorisation
 * schemes, check the Spring Security documentation for details. We are using the LDAP user details mapper as
 * a base implementation and then add the LTTS specific role management.
 */
@Component
public class UserDetailsMapper extends LdapUserDetailsMapper {

    public static final SimpleGrantedAuthority ROLE_LTTS_ADMIN = new SimpleGrantedAuthority("ROLE_LTTS_ADMIN");
    public static final SimpleGrantedAuthority ROLE_SWG_MEMBER = new SimpleGrantedAuthority("Software");

    private static final Logger LOGGER = Logger.getLogger(UserDetailsMapper.class);

    @Resource
    private ConfigurationService configurationService;

    @Override
    public UserDetails mapUserFromContext(final DirContextOperations ctx, final String username, final Collection<? extends GrantedAuthority> authorities) {

        // use default functionality to get all relevant information from LDAP (AD)
        final UserDetails user = super.mapUserFromContext(ctx, username, authorities);

        // get all user names with admin privileges from configuration
        final List<String> admins = configurationService.getStringList(Configuration.Value.ADMIN_USERS);
        final Collection<? extends GrantedAuthority> userAuthorities = getAuthorities(user, admins);

        // now create an adapted user details record including the LTTS roles if applicable
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return userAuthorities;
            }

            @Override
            public String getPassword() {
                return user.getPassword();
            }

            @Override
            public String getUsername() {
                return user.getUsername();
            }

            @Override
            public boolean isAccountNonExpired() {
                return user.isAccountNonExpired();
            }

            @Override
            public boolean isAccountNonLocked() {
                return user.isAccountNonLocked();
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return user.isCredentialsNonExpired();
            }

            @Override
            public boolean isEnabled() {
                return user.isEnabled();
            }
        };
    }

    /**
     * Adds LTTS specific authorities based on user name.
     * Currently the only role we support is that of an administrator. All members of the software group and
     * all users which are listed in the {@link Configuration.Value#ADMIN_USERS} configuration are granted
     * administrator privileges.
     * @param user
     * @param admins
     * @return
     */
    private Collection<? extends GrantedAuthority> getAuthorities(UserDetails user, List<String> admins) {

        LOGGER.debug("current LTTS admin user names: " + admins);

        if (admins.contains(user.getUsername())) {
            LOGGER.debug("user '" + user.getUsername() +"' is a LTTS admin");
            // if this username is in the list of administrators add the LTTS admin role to its authorities
            Set<GrantedAuthority> roles = new HashSet<>(user.getAuthorities());
            roles.add(ROLE_LTTS_ADMIN);
            return roles;

        } else if (user.getAuthorities().contains(ROLE_SWG_MEMBER)) {
            LOGGER.debug("user '" + user.getUsername() +"' is a SWG member");
            // if this user is a member of the software group it gets admin privileges (for support tasks)
            Set<GrantedAuthority> newAuthorities = new HashSet<>(user.getAuthorities());
            newAuthorities.add(ROLE_LTTS_ADMIN);
            return newAuthorities;
        } else {
            LOGGER.debug("user '" + user.getUsername() +"' is not LTTS admin nor a SWG member");
            // otherwise return the original list of authorities, which will result in access to admin window
            // being denied (HTTP 403 error)
            return user.getAuthorities();
        }
    }

}
