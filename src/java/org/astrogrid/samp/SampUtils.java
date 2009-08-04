package org.astrogrid.samp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains static utility methods for use with the SAMP toolkit.
 *
 * @author   Mark Taylor
 * @since    15 Jul 2008
 */
public class SampUtils {

    /**
     * Standard profile name of the lockfile in the user's home directory.
     * Value is "<code>{@value}</code>".
     */
    public static final String LOCKFILE_NAME = ".samp";

    /**
     * Property which can be used to override the location of the standard
     * profile lockfile.
     * Value is "<code>{@value}</code>.
     */
    public static final String LOCKFILE_PROP = "jsamp.lockfile";

    /**
     * Property which can be used to set name used for localhost in server
     * endpoints.
     * Value is "<code>{@value}</code>".
     * @see  #getLocalhost
     */
    public static final String LOCALHOST_PROP = "jsamp.localhost";

    private static final Logger logger_ =
        Logger.getLogger( SampUtils.class.getName() );
    private static String sampVersion_;
    private static String softwareVersion_;
    private static File lockFile_;
    private static final String NEWLINE = getLineSeparator();

    /**
     * Private constructor prevents instantiation.
     */
    private SampUtils() {
    }

    /**
     * Returns a <em>SAMP int</em> string representation of an integer.
     *
     * @param  i  integer value
     * @return  SAMP int string
     */
    public static String encodeInt( int i ) {
        return Integer.toString( i );
    }

    /**
     * Returns the integer value for a <em>SAMP int</em> string.
     *
     * @param   s   SAMP int string
     * @return  integer value
     * @throws  NumberFormatException  if conversion fails
     */
    public static int decodeInt( String s ) {
        return Integer.parseInt( s );
    }

    /**
     * Returns a <em>SAMP int</em> string representation of a long integer.
     *
     * @param  i  integer value
     * @return  SAMP int string
     */
    public static String encodeLong( long i ) {
        return Long.toString( i );
    }

    /**
     * Returns the integer value as a <code>long</code> for a <em>SAMP int</em>
     * string.
     *
     * @param   s   SAMP int string
     * @return  long integer value
     * @throws  NumberFormatException  if conversion fails
     */
    public static long decodeLong( String s ) {
        return Long.parseLong( s );
    }

    /**
     * Returns a <em>SAMP float</em> string representation of a floating point
     * value.
     *
     * @param d  double value
     * @return  SAMP double string
     * @throws  IllegalArgumentException  if <code>d</code> is NaN or infinite
     */
    public static String encodeFloat( double d ) {
        if ( Double.isInfinite( d ) ) {
            throw new IllegalArgumentException( "Infinite value "
                                              + "not permitted" );
        }
        if ( Double.isNaN( d ) ) {
            throw new IllegalArgumentException( "NaN not permitted" );
        }
        return Double.toString( d );
    }

    /**
     * Returns the double value for a <em>SAMP float</em> string.
     *
     * @param   s  SAMP float string
     * @return  double value
     * @throws   NumberFormatException  if conversion fails
     */
    public static double decodeFloat( String s ) {
        return Double.parseDouble( s );
    }

    /**
     * Returns a <em>SAMP boolean</em> string representation of a boolean value.
     *
     * @param   b  boolean value
     * @return  SAMP boolean string
     */
    public static String encodeBoolean( boolean b ) {
        return encodeInt( b ? 1 : 0 );
    }

    /**
     * Returns the boolean value for a <em>SAMP boolean</em> string.
     *
     * @param  s  SAMP boolean string
     * @return  false iff <code>s</code> is equal to zero
     */
    public static boolean decodeBoolean( String s ) {
        try {
            return decodeInt( s ) != 0;
        }
        catch ( NumberFormatException e ) {
            return false;
        }
    }

    /**
     * Checks that a given object is legal for use in a SAMP context.
     * This checks that it is either a String, List or Map, that
     * any Map keys are Strings, and that Map values and List elements are
     * themselves legal (recursively).
     * 
     * @param  obj  object to check
     * @throws  DataException  in case of an error
     */
    public static void checkObject( Object obj ) {
        if ( obj instanceof Map ) {
            checkMap( (Map) obj );
        }
        else if ( obj instanceof List ) {
            checkList( (List) obj );
        }
        else if ( obj instanceof String ) {
            checkString( (String) obj );
        }
        else if ( obj == null ) {
            throw new DataException( "Bad SAMP object: contains a null" );
        }
        else {
            throw new DataException( "Bad SAMP object: contains a "
                                   + obj.getClass().getName() );
        }
    }

    /**
     * Checks that a given Map is legal for use in a SAMP context.
     * All its keys must be strings, and its values must be legal
     * SAMP objects.
     *
     * @param  map  map to check
     * @throws  DataException in case of an error
     * @see     #checkObject
     */
    public static void checkMap( Map map ) {
        for ( Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Object key = entry.getKey();
            if ( key instanceof String ) {
                checkString( (String) key );
                checkObject( entry.getValue() );
            }
            else if ( key == null ) {
                throw new DataException( "Map key null" );
            }
            else {
                throw new DataException( "Map key not a string ("
                                       + key.getClass().getName() + ")" );
            }
        }
    }

    /**
     * Checks that a given List is legal for use in a SAMP context.
     * All its elements must be legal SAMP objects.
     *
     * @param  list  list to check
     * @throws  DataException in case of error
     * @see     #checkObject
     */
    public static void checkList( List list ) {
        for ( Iterator it = list.iterator(); it.hasNext(); ) {
            checkObject( it.next() );
        }
    }

    /**
     * Checks that a given String is legal for use in a SAMP context.
     * All its characters must be in the range 0x01 - 0x7f.
     *
     * @param  string  string to check
     * @throws  DataException  in case of error
     */
    public static void checkString( String string ) {
        int leng = string.length();
        for ( int i = 0; i < leng; i++ ) {
            char c = string.charAt( i );
            if ( ! isStringChar( c ) ) {
                throw new DataException( "Bad SAMP string; contains character "
                                       + "0x" + Integer.toHexString( c ) );
            }
        }
    }

    /**
     * Indicates whether a given character is legal to include in a SAMP
     * string.
     *
     * @return  true iff c is 0x09, 0x0a, 0x0d or 0x20--0x7f
     */
    public static boolean isStringChar( char c ) {
        switch ( c ) {
            case 0x09:
            case 0x0a:
            case 0x0d:
                return true;
            default:
                return c >= 0x20 && c <= 0x7f;
        }
    }

    /**
     * Checks that a string is a legal URL.
     *
     * @param  url  string to check
     * @throws  DataException  if <code>url</code> is not a legal URL
     */
    public static void checkUrl( String url ) {
        if ( url != null ) {
            try {
                new URL( (String) url );
            }
            catch ( MalformedURLException e ) {
                throw new DataException( "Bad URL " + url, e );
            }
        }
    }

    /**
     * Returns a string representation of a client object.
     * The name is used if present, otherwise the ID.
     *
     * @param  client   client object
     * @return  string
     */
    public static String toString( Client client ) {
        Metadata meta = client.getMetadata();
        if ( meta != null ) {
            String name = meta.getName();
            if ( name != null && name.trim().length() > 0 ) {
                return name;
            }
        }
        return client.getId();
    }

    /**
     * Pretty-prints a SAMP object.
     *
     * @param   obj  SAMP-friendly object
     * @param   indent   base indent for text block
     * @return   string containing formatted object
     */
    public static String formatObject( Object obj, int indent ) {
        checkObject( obj );
        StringBuffer sbuf = new StringBuffer();
        formatObject( sbuf, 0, indent, obj );
        return sbuf.toString();
    }

    /**
     * Recursive routine which does the work for formatObject.
     *
     * @param   sbuf   string buffer to accumulate result
     * @param   level  indent level (starts at 0, increments by 1)
     * @param   indent  base block indentation (number of spaces)
     * @param   obj  object to append to sbuf
     */
    private static void formatObject( StringBuffer sbuf, int level, int indent,
                                      Object obj ) {
        if ( obj instanceof String ) {
            boolean top = sbuf.length() == 0;
            String txt = (String) obj;
            int npad = indent + level * 3;
            String[] lines = txt.split( NEWLINE );
            for ( int il = 0; il < lines.length; il++ ) {
                if ( ! top ) {
                    sbuf.append( '\n' );
                }
                for ( int ip = 0; ip < npad; ip++ ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( lines[ il ] );
            }
        }
        else if ( obj instanceof List ) {
            List list = (List) obj;
            boolean top = sbuf.length() == 0;
            if ( ! top ) {
                sbuf.append( '[' );
            }
            for ( Iterator it = list.iterator(); it.hasNext(); ) {
                formatObject( sbuf, level + 1, indent, it.next() );
            }
            if ( ! top ) {
                if ( ! list.isEmpty() ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( ']' );
            }
        }
        else if ( obj instanceof Map ) {
            Map map = (Map) obj;
            boolean top = sbuf.length() == 0;
            if ( ! top ) {
                sbuf.append( '{' );
            }
            for ( Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                Object value = entry.getValue();
                if ( value instanceof String ) {
                    formatObject( sbuf, level, indent, key + ": " + value );
                }
                else {
                    formatObject( sbuf, level, indent, key + ": " );
                    formatObject( sbuf, level + 1, indent, value );
                }
            }
            if ( ! top ) {
                if ( ! map.isEmpty() ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( '}' );
            }
        }
        else {
            assert false;
        }
    }

    /**
     * Returns the location of the Standard Profile lockfile.
     * This is the file <code>.samp</code> in the user's "home" directory.
     *
     * @return  SAMP Standard Profile lockfile
     */
    public static File getLockFile() {
        if ( lockFile_ == null ) {
            String lockname = System.getProperty( LOCKFILE_PROP );
            lockFile_ = lockname != null
                      ? new File( lockname )
                      : new File( Platform.getPlatform().getHomeDirectory(),
                                  LOCKFILE_NAME );
        }
        return lockFile_;
    }

    /**
     * Parses a command-line string as a SAMP object.
     * Currently, this just returns the same string, or null for a
     * zero-length string, but it ought to have some method of decoding 
     * (nested) list and maps as well (JSON?).
     *
     * @param   str   command-line argument
     * @return  SAMP object
     */
    public static Object parseValue( String str ) {
        if ( str == null || str.length() == 0 ) {
            return null;
        }
        else {
            Object sval = str;
            checkObject( sval );
            return sval;
        }
    }

    /**
     * Returns a string denoting the local host to be used for communicating
     * local server endpoints and so on.
     *
     * <p>The value returned by default is the loopback address, "127.0.0.1".
     * However this behaviour can be overridden by setting the
     * {@link #LOCALHOST_PROP} system property to the string which should
     * be returned instead.
     * This may be necessary if the loopback address is not appropriate,
     * for instance in the case of multiple configured loopback interfaces(?)
     * or where SAMP communication is required across different machines.
     * There are two special values which may be used for this property:
     * <ul>
     * <li><code>[hostname]</code>:
     *     uses the fully qualified domain name of the host</li>
     * <li><code>[hostnumber]</code>:
     *     uses the IP number of the host</li>
     * </ul>
     * If these determinations fail for some reason, a fallback value of
     * 127.0.0.1 will be used.
     *
     * <p>In JSAMP version 0.3-1 and prior versions, the [hostname]
     * behaviour was the default.
     * Although this might be seen as more correct, in practice it could cause
     * a lot of problems with DNS configurations which are incorrect or
     * unstable (common in laptops outside their usual networks).
     * See, for instance, AstroGrid bugzilla tickets
     * <a href="http://www.astrogrid.org/bugzilla/show_bug.cgi?id=1799"
     *    >1799</a>,
     * <a href="http://www.astrogrid.org/bugzilla/show_bug.cgi?id=2151"
     *    >2151</a>.
     *
     * <p>In JSAMP version 0.3-1 and prior versions, the property was
     * named <code>samp.localhost</code> rather than 
     * <code>jsamp.localhost</code>.  This name is still accepted for
     * backwards compatibility.
     *
     * @return  local host name
     */
    public static String getLocalhost() {
        final String defaultHost = "127.0.0.1";
        String hostname = 
            System.getProperty( LOCALHOST_PROP,
                                System.getProperty( "samp.localhost", "" ) );
        if ( hostname.length() == 0 ) {
            hostname = defaultHost;
        }
        else if ( "[hostname]".equals( hostname ) ) {
            try {
                hostname = InetAddress.getLocalHost().getCanonicalHostName();
            }
            catch ( UnknownHostException e ) {
                logger_.log( Level.WARNING,
                             "Local host determination failed - fall back to "
                           + defaultHost, e );
                hostname = defaultHost;
            }
        }
        else if ( "[hostnumber]".equals( hostname ) ) {
            try {
                hostname = InetAddress.getLocalHost().getHostAddress();
            }
            catch ( UnknownHostException e ) {
                logger_.log( Level.WARNING,
                             "Local host determination failed - fall back to "
                           + defaultHost, e );
                hostname = defaultHost;
            }
        }
        logger_.config( "Local host is " + hostname );
        return hostname;
    }

    /**
     * Returns an unused port number on the local host.
     *
     * @param   startPort  suggested port number; may or may not be used
     * @return  unused port
     */
    public static int getUnusedPort( int startPort ) throws IOException {

        // Current implementation ignores the given startPort and uses
        // findAnyPort.
        return true ? findAnyPort()
                    : scanForPort( startPort, 20 );
    }

    /**
     * Returns a string giving the version of the SAMP standard which this
     * software implements.
     *
     * @return  SAMP standard version
     */
    public static String getSampVersion() {
        if ( sampVersion_ == null ) {
            sampVersion_ = readResource( "samp.version" );
        }
        return sampVersion_;
    }

    /**
     * Returns a string giving the version of this software package.
     *
     * @return  JSAMP version
     */
    public static String getSoftwareVersion() {
        if ( softwareVersion_ == null ) {
            softwareVersion_ = readResource( "jsamp.version" );
        }
        return softwareVersion_;
    }

    /**
     * Returns the contents of a resource as a string.
     *
     * @param  rname  resource name 
     *                (in the sense of {@link java.lang.Class#getResource})
     */
    private static String readResource( String rname ) {
        URL url = SampUtils.class.getResource( rname );
        try {
            InputStream in = url.openStream();
            StringBuffer sbuf = new StringBuffer();
            for ( int c; ( c = in.read() ) >= 0; ) {
                sbuf.append( (char) c );
            }
            in.close();
            return sbuf.toString().trim();
        }
        catch ( IOException e ) {
            logger_.warning( "Failed to read resource " + url );
            return "??";
        }
    }

    /**
     * Returns the system-dependent line separator sequence.
     *
     * @return  line separator
     */
    private static String getLineSeparator() {
        try {
            return System.getProperty( "line.separator", "\n" );
        }
        catch ( SecurityException e ) {
            return "\n";
        }
    }

    /**
     * Locates an unused server port on the local host.
     * Potential problem: between when this method completes and when
     * the return value of this method is used by its caller, it's possible
     * that the port will get used by somebody else.
     * Probably this will not happen much in practice??
     *
     * @return  unused server port
     */
    private static int findAnyPort() throws IOException {
        ServerSocket socket = new ServerSocket( 0 );
        try {
            return socket.getLocalPort();
        }
        finally {
            try {
                socket.close();
            }
            catch ( IOException e ) {
            }
        }
    }

    /**
     * Two problems with this one - it may be a bit inefficient, and 
     * there's an annoying bug in the Apache XML-RPC WebServer class
     * which causes it to print "java.util.NoSuchElementException" to
     * the server's System.err for every port scanned by this routine 
     * that an org.apache.xmlrpc.WebServer server is listening on.
     *
     * @param  startPort  port to start scanning upwards from
     * @param  nTry  number of ports in sequence to try before admitting defeat
     * @return  unused server port
     */
    private static int scanForPort( int startPort, int nTry )
            throws IOException {
        for ( int iPort = startPort; iPort < startPort + nTry; iPort++ ) {
            try {
                Socket trySocket = new Socket( "localhost", iPort );
                if ( ! trySocket.isClosed() ) {
                    trySocket.shutdownOutput();
                    trySocket.shutdownInput();
                    trySocket.close();
                }
            }
            catch ( ConnectException e ) {
    
                /* Can't connect - this hopefully means that the socket is
                 * unused. */
                return iPort;
            }
        }
        throw new IOException( "Can't locate an unused port in range " +
                               startPort + " ... " + ( startPort + nTry ) );
    }
}
