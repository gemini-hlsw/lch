package edu.gemini.lch.web.app;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.event.ListenerMethod;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import edu.gemini.lch.services.AlarmService;
import edu.gemini.lch.services.SchedulerService;
import edu.gemini.lch.web.app.windows.admin.AdministrationWindow;
import edu.gemini.lch.web.app.windows.alarm.AlarmWindow;
import edu.gemini.lch.web.app.windows.login.LoginWindow;
import edu.gemini.lch.web.app.windows.night.NightWindow;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Vaadin application object represents a user session.
 * Each window can only exist once in a browser. Windows which are registered as listeners must be un-registered
 * when the session is closed (timeout), otherwise memory leaks will occur.
 * Note: This has been ported from Vaadin 6 application objects. Vaadin 7 has dedicated user session objects
 * VaadinSession which probably should be used to do some of the stuff that's currently done here.
 */
@Configurable(preConstruction = true)
@PreserveOnRefresh
@Theme("lch")
public class LchApplication extends UI implements ViewChangeListener
{

    private static final Logger LOGGER = Logger.getLogger(LchApplication.class);

    private Navigator navigator;
    private NightWindow nightWindow;
    private AlarmWindow alarmWindow;
    private AdministrationWindow adminWindow;
    private LoginWindow loginWindow;
    private boolean authorized = false;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private SchedulerService schedulerService;

    /**
     * Initializes the application (start of user session).
     * Creates a set of windows for the session and registers some windows as listeners.
     */
    @Override
    public void init(final VaadinRequest request) {

        navigator   = new Navigator(this, this);
        nightWindow = new NightWindow();
        alarmWindow = new AlarmWindow();
        adminWindow = new AdministrationWindow();
        loginWindow = new LoginWindow();

        schedulerService.addListener(alarmWindow);
        schedulerService.addListener(nightWindow);

        nightWindow.initNight();

        navigator.addView("", nightWindow);
        navigator.addView(NightWindow.NAME, nightWindow);
        navigator.addView(AlarmWindow.NAME, alarmWindow);
        navigator.addView(AdministrationWindow.NAME, adminWindow);
        navigator.addView(LoginWindow.NAME, loginWindow);
        navigator.addViewChangeListener(this);

        setNavigator(navigator);
        // Note: Vaadin 7 supports push which could be used instead of polling here. For now we keep using poll.
        setPollInterval(500);
        setErrorHandler(new ErrorHandler());

        // Not sure if this is the best way to do this but we somehow need to allow deep links so that
        // users can for example bookmark the alarm page and open it directly from the bookmark
        final String fragment = Page.getCurrent().getUriFragment();
        if      (fragment == null)                              navigator.navigateTo(NightWindow.NAME);
        else if (fragment.contains(NightWindow.NAME))           navigator.navigateTo(NightWindow.NAME);
        else if (fragment.contains(AlarmWindow.NAME))           navigator.navigateTo(AlarmWindow.NAME);
        else if (fragment.contains(AdministrationWindow.NAME))  navigator.navigateTo(AdministrationWindow.NAME);
        else                                                    navigator.navigateTo(NightWindow.NAME);

    }

    /**
     * Closes the application (end of user session).
     * Called when user session ends, usually after a timeout. Do all cleanup here that's needed to garbage collect
     * the application (session).
     */
    @Override
    public void close() {
        navigator.removeViewChangeListener(this);
        schedulerService.removeListener(alarmWindow);
        schedulerService.removeListener(nightWindow);

        super.close();
    }

    /**
     * Gets the applications night window.
     */
    public NightWindow getNightWindow() {
        return nightWindow;
    }

    @Override
    public boolean beforeViewChange(final ViewChangeEvent event) {
        if (event.getNewView() instanceof AdministrationWindow) {
            if (!authorized) {
                event.getNavigator().navigateTo(LoginWindow.NAME);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterViewChange(final ViewChangeEvent event) {}

    public void setAuthorized(final boolean authorized) {
        this.authorized = authorized;
    }

    /**
     * Custom implementation of a "catch-all" exception handler that avoids exceptions from going unnoticed
     * and keeps users informed about potential problems.
     */
    private class ErrorHandler extends DefaultErrorHandler {
        @Override
        public void error(final com.vaadin.server.ErrorEvent event) {
            // Don't call the default implementation on production environment.
            // (In case of an exception it shows a red exclamation mark on the upper right corner which just stays
            // there, not what we want for production. TODO: check if this changes if we turn off Vaadin debug mode)
            // As far as I know this basically just logs the error, shows the exclamation mark and doesn't do much else.
            // super.terminalError(event);

            // Some custom behaviour.
            final Throwable throwable = event.getThrowable();
            LOGGER.error("Uncaught exception occurred!", throwable);
            if (throwable instanceof ListenerMethod.MethodException) {
                // exception happened inside a listener method, user is only interested
                // in the exception that originally caused the error
                showError(throwable.getCause());
            } else {
                // otherwise show the main error
                showError(throwable);
            }
        }
    }

    /**
     * Display an error message to the user.
     */
    private void showError(final Throwable throwable) {
        Notification.show(
                "An error occured!",
                throwable.getMessage(),
                Notification.Type.ERROR_MESSAGE);
    }

}