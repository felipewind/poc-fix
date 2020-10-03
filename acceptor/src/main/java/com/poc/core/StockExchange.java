package com.poc.core;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Acceptor;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

@Singleton
public class StockExchange {

    @Inject
    SessionSettingsFactory sessionSettingsFactory;

    @Inject
    StockExchangeApplication stockExchangeApplication;

    private static final Logger LOG = LoggerFactory.getLogger(StockExchange.class.getName());
    private boolean acceptorStarted;
    private SessionSettings sessionSettings;
    private Acceptor acceptor;

    public StockExchange() {
        LOG.info("Constructor");
    }

    public void init() {
        LOG.info("init");

        sessionSettings = sessionSettingsFactory.getSessionSettings();
        LOG.info("SessionSettings created:\n" + sessionSettings.toString());

        MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();

        LogFactory logFactory = new ScreenLogFactory(sessionSettings);

        MessageFactory messageFactory = new DefaultMessageFactory();
        LOG.info("MessageFactory created - DefaultMessageFactory");

        try {
            acceptor = new SocketAcceptor(stockExchangeApplication, messageStoreFactory, sessionSettings, logFactory,
                    messageFactory);
            LOG.info("Acceptor created - SocketAcceptor");

            start();

        } catch (ConfigError e) {
            LOG.error("ConfigError \n" + e);
            e.printStackTrace();
            if (acceptorStarted) {
                stop();
            }
        }
    }

    public synchronized void start() throws ConfigError {
        LOG.info("start");
        if (!acceptorStarted) {
            acceptor.start();
            LOG.info("Acceptor start - \nSessionID created: " + acceptor.getSessions() + "\n");
            acceptorStarted = true;
        } else {
            for (SessionID sessionID : acceptor.getSessions()) {
                Session.lookupSession(sessionID).logon();
                LOG.info("Logon solicited \nSessionID created: " + acceptor.getSessions() + "\n");
            }
        }
    }

    public synchronized void stop() {
        LOG.info("stop");
        if (acceptorStarted) {
            acceptor.stop();
            acceptorStarted = false;
        }
    }

    public synchronized void logout() {
        LOG.info("logout");
        for (SessionID sessionId : acceptor.getSessions()) {
            Session.lookupSession(sessionId).logout("user requested");
        }
    }

    public SessionID getSessionID() {
        if (acceptorStarted) {
            return acceptor.getSessions().get(0);
        } else {
            throw new RuntimeException("acceptor stopped");
        }
    }

    public SessionSettings getSessionSettings() {
        return sessionSettings;
    }

}