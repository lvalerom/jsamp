package org.astrogrid.samp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
     * Property which can be used to set name used for localhost in server
     * endpoints.
     * Value is {@value}.
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
        return new JsonWriter( indent, true ).toJson( obj );
    }

    /**
     * Parses a command-line string as a SAMP object.
     * If it can be parsed as a SAMP-friendly JSON string, that interpretation
     * will be used.  Otherwise, the value is just the string as presented.
     *
     * @param   str   command-line argument
     * @return  SAMP object
     */
    public static Object parseValue( String str ) {
        if ( str == null || str.length() == 0 ) {
            return null;
        }
        else {
            try {
                Object obj = fromJson( str );
                checkObject( obj );
                return obj;
            }
            catch ( RuntimeException e ) {
                logger_.config( "String not JSON (" + e + ")" );
            }
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
     * Turns a File into a URL.
     * Unlike Sun's J2SE, this gives you a URL which conforms to RFC1738 and
     * looks like "<code>file://localhost/abs-path</code>" rather than
     * "<code>file:abs-or-rel-path</code>".
     *
     * @param   file   file
     * @return   URL
     * @see   "RFC 1738"
     * @see   <a
     *   href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6356783"
     *   >Sun Java bug 6356783</a>
     */
    public static URL fileToUrl( File file ) {
        try {
            return new URL( "file", "localhost",
                            file.toURI().toURL().getPath() );
        }
        catch ( MalformedURLException e ) {
            throw new AssertionError();
        }
    }

    /**
     * Reverses URI-style character escaping (%xy) on a string.
     * Note, unlike {@link java.net.URLDecoder},
     * this does not turn "+" characters into spaces.
     *
     * @see "RFC 2396, Section 2.4"
     * @param  text  escaped text
     * @return  unescaped text
     */
    public static String uriDecode( String text ) {
        try {
            return URLDecoder.decode( replaceChar( text, '+', "%2B" ),
                                      "UTF-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new AssertionError( "UTF-8 unsupported??" );
        }
    }

    /**
     * Performs URI-style character escaping (%xy) on a string.
     * Note, unlike {@link java.net.URLEncoder},
     * this encodes spaces as "%20" and not "+".
     *
     * @see "RFC 2396, Section 2.4"
     * @param  text  unescaped text
     * @return  escaped text
     */
    public static String uriEncode( String text ) {
        try {
            return replaceChar( URLEncoder.encode( text, "UTF-8" ),
                                '+', "%20" );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new AssertionError( "UTF-8 unsupported??" );
        }
    }

    /**
     * Attempts to interpret a URL as a file.
     * If the URL does not have the "file:" protocol, null is returned.
     *
     * @param  url  URL, may or may not be file: protocol
     * @return  file, or null
     */
    public static File urlToFile( URL url ) {
        if ( url.getProtocol().equals( "file" ) && url.getRef() == null
                                                && url.getQuery() == null ) {
            String path = uriDecode( url.getPath() );
            String filename = File.separatorChar == '/'
                            ? path
                            : path.replace( '/', File.separatorChar );
            return new File( filename );
        }
        else {
            return null;
        }
    }

    /**
     * Parses JSON text to give a SAMP object.
     * Note that double-quoted strings are the only legal scalars
     * (no unquoted numbers or booleans).
     *
     * @param  str  string to parse
     * @return  SAMP object
     */
    public static Object fromJson( String str ) {
        return new JsonReader().read( str );
    }

    /**
     * Serializes a SAMP object to a JSON string.
     *
     * @param  item to serialize
     * @param  multiline  true for formatted multiline output, false for a
     *         single line
     */
    public static String toJson( Object item, boolean multiline ) {
        checkObject( item );
        return new JsonWriter( multiline ? 2 : -1, true ).toJson( item );
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
        if ( url == null ) {
            logger_.warning( "No such resource " + rname );
            return "??";
        }
        else {
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
     * Replaces all occurrences of a single character with a given replacement
     * string.
     *
     * @param   in  input string
     * @param  oldChar  character to replace
     * @param  newText  replacement string
     * @return   modified string
     */
    private static String replaceChar( String in, char oldChar,
                                       String newTxt ) {
        int len = in.length();
        StringBuffer sbuf = new StringBuffer( len );
        for ( int i = 0; i < len; i++ ) {
            char c = in.charAt( i );
            if ( c == oldChar ) {
                sbuf.append( newTxt );
            }
            else {
                sbuf.append( c );
            }
        }
        return sbuf.toString();
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
