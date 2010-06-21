package org.astrogrid.samp.xmlrpc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import org.astrogrid.samp.DataException;
import org.astrogrid.samp.Platform;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.SampException;

/**
 * Standard Profile implementation of ClientProfile.
 * It is normally appropriate to use one of the static methods
 * to obtain an instance based on a particular XML-RPC implementation.
 *
 * @author   Mark Taylor
 * @since    15 Jul 2008
 */
public class StandardClientProfile implements ClientProfile {

    private final SampXmlRpcClientFactory xClientFactory_;
    private final SampXmlRpcServerFactory xServerFactory_;

    private static StandardClientProfile defaultInstance_;
    private static URL lockUrl_;
    private static final Logger logger_ =
        Logger.getLogger( StandardClientProfile.class.getName() );

    /** Environment variable used for hub location ({@value}). */
    public static final String HUBLOC_ENV = "SAMP_HUB";

    /** Filename used for lockfile in home directory by default ({@value}). */
    public static final String LOCKFILE_NAME = ".samp";

    /** Prefix in SAMP_HUB value indicating lockfile URL ({@value}). */
    public static final String STDPROFILE_HUB_PREFIX = "std-lockurl:";

    /**
     * Constructs a profile given client and server factory implementations.
     *
     * @param   xClientFactory   XML-RPC client factory implementation
     * @param   xServerFactory   XML-RPC server factory implementation
     */
    public StandardClientProfile( SampXmlRpcClientFactory xClientFactory,
                                  SampXmlRpcServerFactory xServerFactory ) {
        xClientFactory_ = xClientFactory;
        xServerFactory_ = xServerFactory;
    }

    /**
     * Constructs a profile given an XmlRpcKit object.
     *
     * @param  xmlrpc  XML-RPC implementation
     */
    public StandardClientProfile( XmlRpcKit xmlrpc ) {
        this( xmlrpc.getClientFactory(), xmlrpc.getServerFactory() );
    }

    public HubConnection register() throws SampException {
        LockInfo lockInfo;
        try {
            lockInfo = getLockInfo();
        }
        catch ( SampException e ) {
            throw (SampException) e;
        }
        catch ( IOException e ) {
            throw new SampException( "Error reading lockfile", e );
        }
        if ( lockInfo == null ) {
            return null;
        }
        else {
            try {
                lockInfo.check();
            }
            catch ( DataException e ) {
                throw new SampException( "Incomplete/broken lock file", e );
            }
            SampXmlRpcClient xClient;
            URL xurl = lockInfo.getXmlrpcUrl();
            try {
                xClient = xClientFactory_.createClient( xurl );
            }
            catch ( IOException e ) {
                throw new SampException( "Can't connect to " + xurl, e );
            }
            return new XmlRpcHubConnection( xClient, xServerFactory_,
                                            lockInfo.getSecret() );
        }
    }

    /**
     * Returns the LockInfo which indicates how to locate the hub.
     * The default implementation returns {@link LockInfo#readLockFile};
     * it may be overridden to provide a non-standard client profiles.
     *
     * @return   hub location information
     */
    public LockInfo getLockInfo() throws IOException {
        return LockInfo.readLockFile( getLockUrl() );
    }

    /**
     * Returns the location of the Standard Profile lockfile.
     * By default this is the file <code>.samp</code> in the user's "home"
     * directory, unless overridden by a value of the SAMP_HUB environment
     * variable starting with "std-lockurl".
     *
     * @return   lockfile URL
     */
    public static URL getLockUrl() throws IOException {
        if ( lockUrl_ == null ) {
            Platform platform = Platform.getPlatform();
            String hubloc = platform.getEnv( HUBLOC_ENV );
            final URL lockUrl;
            if ( hubloc != null &&
                 hubloc.startsWith( STDPROFILE_HUB_PREFIX ) ) {
                lockUrl = new URL( hubloc.substring( STDPROFILE_HUB_PREFIX
                                                    .length() ) );
            }
            else {
                if ( hubloc != null ) {
                    logger_.warning( "Ignoring non-standard $" + HUBLOC_ENV
                                   + "=" + hubloc );
                }
                lockUrl =
                    SampUtils.fileToUrl( new File( platform.getHomeDirectory(),
                                                   LOCKFILE_NAME ) );
            }
            lockUrl_ = lockUrl;
        }
        return lockUrl_;
    }

    /**
     * Returns an instance based on the default XML-RPC implementation.
     * This can be configured using system properties.
     *
     * @see   XmlRpcKit#getInstance
     * @see   org.astrogrid.samp.client.DefaultClientProfile#getProfile
     * @return  a client profile instance
     */
    public static StandardClientProfile getInstance() {
        if ( defaultInstance_ == null ) {
            XmlRpcKit xmlrpc = XmlRpcKit.getInstance();
            defaultInstance_ =
                new StandardClientProfile( xmlrpc.getClientFactory(),
                                           xmlrpc.getServerFactory() );
        }
        return defaultInstance_;
    }
}
