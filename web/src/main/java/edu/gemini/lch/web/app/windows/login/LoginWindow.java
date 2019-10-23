package edu.gemini.lch.web.app.windows.login;

import com.vaadin.event.ShortcutAction;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.*;
import edu.gemini.lch.web.app.LchApplication;
import edu.gemini.lch.web.app.components.Footer;
import edu.gemini.lch.web.app.components.Header;
import edu.gemini.lch.web.app.components.LabeledComponent;
import edu.gemini.lch.web.app.windows.admin.AdministrationWindow;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A simple customized login window.
 * Unfortunately Spring and Vaadin don't play very well with each other and we have to do the actual login
 * manually instead of having it done by the Spring framework. Also, Spring assumes that secured/unsecured
 * parts of the application have different URL paths, however Vaadin 7 uses URL fragments to navigate between
 * views; this means we can not simply use Spring xml declarations to define which parts to protect as we
 * did with Vaadin 6, instead we do it with a ViewChangeListener.
 */
@Configurable(preConstruction = true)
public final class LoginWindow extends Panel implements View, Button.ClickListener {

    private static final Logger LOGGER = Logger.getLogger(LoginWindow.class);

    public static final String NAME = "login";

    @Autowired
    private AuthenticationManager authenticationManager;

    private final TextField username;
    private final PasswordField password;

    /**
     * Creates a login window.
     */
    public LoginWindow() {
        final Header header = new LoginWindowHeader(this);
        final Footer footer = new Footer();
        final Button loginBtn = new Button("Login", this);
        loginBtn.setClickShortcut(ShortcutAction.KeyCode.ENTER);

        username = new TextField();
        username.setCaption("Username");
        username.setId("username");                 // this will help some applications like LastPass with auto-fill
        password = new PasswordField();
        password.setCaption("Password");
        final Layout loginForm = new FormLayout() {{
            addComponent(username);
            addComponent(password);
            addComponent(loginBtn);
        }};
        final LabeledComponent login = new LabeledComponent("Please provide user name and password:", loginForm);

        final VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.addComponent(header.getComponent());
        layout.addComponent(login);
        layout.addComponent(footer.getComponent());
        layout.setComponentAlignment(login, Alignment.TOP_CENTER);
        setContent(layout);

    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent e) {
        UI.getCurrent().getPage().setTitle("LTTS Login");
    }



    /**
     * Does the user authentication using the Spring security framework.
     */
    public void buttonClick(final Button.ClickEvent event) {

        final String user = username.getValue();
        final String pass = password.getValue();

        LOGGER.debug("login attempt for user " + user);
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(user, pass);

        try {
            Authentication authentication = authenticationManager.authenticate(authRequest);
            if (authentication.isAuthenticated() && authentication.getAuthorities().contains(UserDetailsMapper.ROLE_LTTS_ADMIN)) {
                loginSuccessful(user, authentication);
            } else {
                loginFailed(user, new RuntimeException("Insufficient access rights."));
            }
        } catch (Exception e) {
            loginFailed(user, e);
        }
    }

    /**
     * Processes successful authentication and forwards to administration page.
     * Authentication with given username/password was successful with Active Directory.
     * Note: Although the user is authenticated it may still be missing the necessary authorisation to access
     * the admin window, this will result in a HTTP 403 error after being forwarded to the admin window.
     * @param username
     * @param authentication
     */
    private void loginSuccessful(final String username, final Authentication authentication) {
        LOGGER.info("login for user " + username + " successful");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ((LchApplication) UI.getCurrent()).setAuthorized(true);
        UI.getCurrent().getNavigator().navigateTo(AdministrationWindow.NAME);
    }

    /**
     * Processes unsuccessful authentication and show notification.
     * Authentication with given username/password failed with Active Diretory.
     * @param username
     * @param e
     */
    private void loginFailed(final String username, final Exception e) {
        LOGGER.info("login for user " + username + " failed: " + e.getMessage());
        Notification.show(
                "Login failed!",
                e.getMessage(),
                Notification.Type.ERROR_MESSAGE);
    }

    private final class LoginWindowHeader extends Header {
        protected LoginWindowHeader(LoginWindow window) {
            super(window);
        }
        @Override
        protected void addToMenu(MenuBar menuBar) {
            // nothing to add
        }
    }

}
