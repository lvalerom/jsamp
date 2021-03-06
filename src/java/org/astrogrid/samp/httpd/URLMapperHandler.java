package org.astrogrid.samp.httpd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler implementation which allows the server to serve resources which
 * are available to it as URLs.  The main use for this is if the URLs
 * are jar:-type ones which are available to the JVM in which the server
 * is running, but not to it's clients.
 * Either a single resource or a whole tree may be served.
 * 
 * @author   Mark Taylor
 * @since    8 Jan 2009
 */
public class URLMapperHandler implements HttpServer.Handler {
    private final String basePath_;
    private final URL baseUrl_;
    private final URL sourceUrl_;
    private final boolean includeRelatives_;

    /**
     * Constructor.
     *
     * @param   server     server within which this handler will be used
     * @param   basePath   path of served resources relative to the base path
     *                     of the server itself
     * @param   sourceUrl  URL of the resource which is to be made available
     *                     at the basePath by this handler
     * @param   includeRelatives  if true, relative URLs based at 
     *                     <code>basePath</code>
     *                     may be requested (potentially giving access to 
     *                     for instance the entire tree of classpath resources);
     *                     if false, only the exact resource named by 
     *                     <code>sourceUrl</code> is served
     */
    public URLMapperHandler( HttpServer server, String basePath, URL sourceUrl,
                             boolean includeRelatives )
            throws MalformedURLException {
        if ( ! basePath.startsWith( "/" ) ) {
            basePath = "/" + basePath;
        }
        if ( ! basePath.endsWith( "/" ) && includeRelatives ) {
            basePath = basePath + "/";
        }
        basePath_ = basePath;
        baseUrl_ = new URL( server.getBaseUrl(), basePath );
        sourceUrl_ = sourceUrl;
        includeRelatives_ = includeRelatives;
    }

    /**
     * Returns the base URL for this handler.
     * If not including relatives, this will be the only URL served.
     *
     * @return  URL
     */
    public URL getBaseUrl() {
        return baseUrl_;
    }

    public HttpServer.Response serveRequest( HttpServer.Request request ) {

        // Determine the source URL from which the data will be obtained.
        String path = request.getUrl();
        if ( ! path.startsWith( basePath_ ) ) {
            return null;
        }
        String relPath = path.substring( basePath_.length() );
        final URL srcUrl;
        if ( includeRelatives_ ) {
            try {
                srcUrl = new URL( sourceUrl_, relPath );
            }
            catch ( MalformedURLException e ) {
                return HttpServer
                      .createErrorResponse( 500, "Internal server error", e );
            }
        }
        else {
            if ( relPath.length() == 0 ) {
                srcUrl = sourceUrl_;
            }
            else {
                return HttpServer.createErrorResponse( 403, "Forbidden" );
            }
        }

        // Forward header and data from the source URL to the response.
        return mapUrlResponse( request.getMethod(), srcUrl );
    }

    /**
     * Repackages a resource from a given target URL as an HTTP response.
     * The data and relevant headers are copied straight through.
     * GET and HEAD methods are served.
     *
     * @param  method  HTTP method
     * @param  targetUrl  URL containing the resource to forward
     * @return   response redirecting to the given target URL
     */
    public static HttpServer.Response mapUrlResponse( String method,
                                                      URL targetUrl ) {
        final URLConnection conn;
        try {
            conn = targetUrl.openConnection();
            conn.connect();
        }
        catch ( IOException e ) {
            return HttpServer.createErrorResponse( 404, "Not found", e );
        }
        try {
            Map hdrMap = new LinkedHashMap();
            String contentType = conn.getContentType();
            if ( contentType != null ) {
                hdrMap.put( "Content-Type", contentType );
            }
            int contentLength = conn.getContentLength();
            if ( contentLength >= 0 ) {
                hdrMap.put( "Content-Length",
                            Integer.toString( contentLength ) );
            }
            String contentEncoding = conn.getContentEncoding();
            if ( contentEncoding != null ) {
                hdrMap.put( "Content-Encoding", contentEncoding );
            }
            if ( "GET".equals( method ) ) {
                return new HttpServer.Response( 200, "OK", hdrMap ) {
                    public void writeBody( OutputStream out )
                            throws IOException {
                        UtilServer.copy( conn.getInputStream(), out );
                    }
                };
            }
            else if ( "HEAD".equals( method ) ) {
                return new HttpServer.Response( 200, "OK", hdrMap ) {
                    public void writeBody( OutputStream out ) {
                    }
                };
            }
            else {
                return HttpServer
                      .create405Response( new String[] { "HEAD", "GET" } );
            }
        }
        catch ( Exception e ) {
            return HttpServer
                  .createErrorResponse( 500, "Internal server error", e );
        }
    }
}
