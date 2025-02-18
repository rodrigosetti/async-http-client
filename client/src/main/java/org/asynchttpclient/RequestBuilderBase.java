/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.asynchttpclient;

import static org.asynchttpclient.util.HttpUtils.*;
import static org.asynchttpclient.util.MiscUtils.isNonEmpty;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.asynchttpclient.channel.pool.ConnectionPoolPartitioning;
import org.asynchttpclient.cookie.Cookie;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.request.body.generator.BodyGenerator;
import org.asynchttpclient.request.body.generator.ReactiveStreamsBodyGenerator;
import org.asynchttpclient.request.body.multipart.Part;
import org.asynchttpclient.resolver.JdkNameResolver;
import org.asynchttpclient.resolver.NameResolver;
import org.asynchttpclient.uri.Uri;
import org.asynchttpclient.util.UriEncoder;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for {@link Request}
 * 
 * @param <T> the builder type
 */
public abstract class RequestBuilderBase<T extends RequestBuilderBase<T>> {

    private final static Logger LOGGER = LoggerFactory.getLogger(RequestBuilderBase.class);

    private static final Uri DEFAULT_REQUEST_URL = Uri.create("http://localhost");

    // builder only fields
    protected UriEncoder uriEncoder;
    protected List<Param> queryParams;
    protected SignatureCalculator signatureCalculator;

    // request fields
    protected String method;
    protected Uri uri;
    protected InetAddress address;
    protected InetAddress localAddress;
    protected HttpHeaders headers;
    protected ArrayList<Cookie> cookies;
    protected byte[] byteData;
    protected List<byte[]> compositeByteData;
    protected String stringData;
    protected ByteBuffer byteBufferData;
    protected InputStream streamData;
    protected BodyGenerator bodyGenerator;
    protected List<Param> formParams;
    protected List<Part> bodyParts;
    protected String virtualHost;
    protected long contentLength = -1;
    protected ProxyServer proxyServer;
    protected Realm realm;
    protected File file;
    protected Boolean followRedirect;
    protected int requestTimeout;
    protected long rangeOffset;
    protected Charset charset;
    protected ConnectionPoolPartitioning connectionPoolPartitioning = ConnectionPoolPartitioning.PerHostConnectionPoolPartitioning.INSTANCE;
    protected NameResolver nameResolver = JdkNameResolver.INSTANCE;

    protected RequestBuilderBase(String method, boolean disableUrlEncoding) {
        this(method, disableUrlEncoding, true);
    }

    protected RequestBuilderBase(String method, boolean disableUrlEncoding, boolean validateHeaders) {
        this.method = method;
        this.uriEncoder = UriEncoder.uriEncoder(disableUrlEncoding);
        this.headers = new DefaultHttpHeaders(validateHeaders);
    }

    protected RequestBuilderBase(Request prototype) {
        this(prototype, false, false);
    }

    protected RequestBuilderBase(Request prototype, boolean disableUrlEncoding, boolean validateHeaders) {
        this.method = prototype.getMethod();
        this.uriEncoder = UriEncoder.uriEncoder(disableUrlEncoding);
        this.uri = prototype.getUri();
        this.address = prototype.getAddress();
        this.localAddress = prototype.getLocalAddress();
        this.headers = new DefaultHttpHeaders(validateHeaders);
        this.headers.add(prototype.getHeaders());
        if (isNonEmpty(prototype.getCookies())) {
            this.cookies = new ArrayList<>(prototype.getCookies());
        }
        this.byteData = prototype.getByteData();
        this.compositeByteData = prototype.getCompositeByteData();
        this.stringData = prototype.getStringData();
        this.byteBufferData = prototype.getByteBufferData();
        this.streamData = prototype.getStreamData();
        this.bodyGenerator = prototype.getBodyGenerator();
        if (isNonEmpty(prototype.getFormParams())) {
            this.formParams = new ArrayList<>(prototype.getFormParams());
        }
        if (isNonEmpty(prototype.getBodyParts())) {
            this.bodyParts = new ArrayList<>(prototype.getBodyParts());
        }
        this.virtualHost = prototype.getVirtualHost();
        this.contentLength = prototype.getContentLength();
        this.proxyServer = prototype.getProxyServer();
        this.realm = prototype.getRealm();
        this.file = prototype.getFile();
        this.followRedirect = prototype.getFollowRedirect();
        this.requestTimeout = prototype.getRequestTimeout();
        this.rangeOffset = prototype.getRangeOffset();
        this.charset = prototype.getCharset();
        this.connectionPoolPartitioning = prototype.getConnectionPoolPartitioning();
        this.nameResolver = prototype.getNameResolver();
    }

    @SuppressWarnings("unchecked")
    private T asDerivedType() {
        return (T) this;
    }

    public T setUrl(String url) {
        return setUri(Uri.create(url));
    }

    public T setUri(Uri uri) {
        this.uri = uri;
        return asDerivedType();
    }

    public T setAddress(InetAddress address) {
        this.address = address;
        return asDerivedType();
    }

    public T setLocalAddress(InetAddress address) {
        this.localAddress = address;
        return asDerivedType();
    }

    public T setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
        return asDerivedType();
    }

    public T setHeader(CharSequence name, String value) {
        this.headers.set(name, value);
        return asDerivedType();
    }

    public T addHeader(CharSequence name, String value) {
        if (value == null) {
            LOGGER.warn("Value was null, set to \"\"");
            value = "";
        }

        this.headers.add(name, value);
        return asDerivedType();
    }

    public T setHeaders(HttpHeaders headers) {
        if (headers == null)
            this.headers.clear();
        else
            this.headers = headers;
        return asDerivedType();
    }

    public T setHeaders(Map<String, Collection<String>> headers) {
        this.headers.clear();
        if (headers != null) {
            for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                this.headers.add(headerName, entry.getValue());
            }
        }
        return asDerivedType();
    }

    public T setContentLength(int contentLength) {
        this.contentLength = contentLength;
        return asDerivedType();
    }

    private void lazyInitCookies() {
        if (this.cookies == null)
            this.cookies = new ArrayList<>(3);
    }

    public T setCookies(Collection<Cookie> cookies) {
        this.cookies = new ArrayList<>(cookies);
        return asDerivedType();
    }

    public T addCookie(Cookie cookie) {
        lazyInitCookies();
        this.cookies.add(cookie);
        return asDerivedType();
    }

    public T addOrReplaceCookie(Cookie cookie) {
        String cookieKey = cookie.getName();
        boolean replace = false;
        int index = 0;
        lazyInitCookies();
        for (Cookie c : this.cookies) {
            if (c.getName().equals(cookieKey)) {
                replace = true;
                break;
            }

            index++;
        }
        if (replace)
            this.cookies.set(index, cookie);
        else
            this.cookies.add(cookie);
        return asDerivedType();
    }

    public void resetCookies() {
        if (this.cookies != null)
            this.cookies.clear();
    }

    public void resetQuery() {
        queryParams = null;
        this.uri = this.uri.withNewQuery(null);
    }

    public void resetFormParams() {
        this.formParams = null;
    }

    public void resetNonMultipartData() {
        this.byteData = null;
        this.compositeByteData = null;
        this.byteBufferData = null;
        this.stringData = null;
        this.streamData = null;
        this.bodyGenerator = null;
        this.contentLength = -1;
    }

    public void resetMultipartData() {
        this.bodyParts = null;
    }

    public T setBody(File file) {
        this.file = file;
        return asDerivedType();
    }

    private void resetBody() {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
    }

    public T setBody(byte[] data) {
        resetBody();
        this.byteData = data;
        return asDerivedType();
    }

    public T setBody(List<byte[]> data) {
        resetBody();
        this.compositeByteData = data;
        return asDerivedType();
    }

    public T setBody(String data) {
        resetBody();
        this.stringData = data;
        return asDerivedType();
    }

    public T setBody(ByteBuffer data) {
        resetBody();
        this.byteBufferData = data;
        return asDerivedType();
    }

    public T setBody(InputStream stream) {
        resetBody();
        this.streamData = stream;
        return asDerivedType();
    }

    public T setBody(Publisher<ByteBuffer> publisher) {
        return setBody(new ReactiveStreamsBodyGenerator(publisher));
    }

    public T setBody(BodyGenerator bodyGenerator) {
        this.bodyGenerator = bodyGenerator;
        return asDerivedType();
    }

    public T addQueryParam(String name, String value) {
        if (queryParams == null)
            queryParams = new ArrayList<>(1);
        queryParams.add(new Param(name, value));
        return asDerivedType();
    }

    public T addQueryParams(List<Param> params) {
        if (queryParams == null)
            queryParams = params;
        else
            queryParams.addAll(params);
        return asDerivedType();
    }

    public T setQueryParams(Map<String, List<String>> map) {
        return setQueryParams(Param.map2ParamList(map));
    }

    public T setQueryParams(List<Param> params) {
        // reset existing query
        if (isNonEmpty(this.uri.getQuery()))
            this.uri = this.uri.withNewQuery(null);
        queryParams = params;
        return asDerivedType();
    }

    public T addFormParam(String name, String value) {
        resetNonMultipartData();
        resetMultipartData();
        if (this.formParams == null)
            this.formParams = new ArrayList<>(1);
        this.formParams.add(new Param(name, value));
        return asDerivedType();
    }

    public T setFormParams(Map<String, List<String>> map) {
        return setFormParams(Param.map2ParamList(map));
    }

    public T setFormParams(List<Param> params) {
        resetNonMultipartData();
        resetMultipartData();
        this.formParams = params;
        return asDerivedType();
    }

    public T addBodyPart(Part bodyPart) {
        resetFormParams();
        resetNonMultipartData();
        if (this.bodyParts == null)
            this.bodyParts = new ArrayList<>();
        this.bodyParts.add(bodyPart);
        return asDerivedType();
    }

    public T setBodyParts(List<Part> bodyParts) {
        this.bodyParts = new ArrayList<>(bodyParts);
        return asDerivedType();
    }

    public T setProxyServer(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        return asDerivedType();
    }

    public T setProxyServer(ProxyServer.Builder proxyServerBuilder) {
        this.proxyServer = proxyServerBuilder.build();
        return asDerivedType();
    }

    public T setRealm(Realm realm) {
        this.realm = realm;
        return asDerivedType();
    }

    public T setFollowRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
        return asDerivedType();
    }

    public T setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return asDerivedType();
    }

    public T setRangeOffset(long rangeOffset) {
        this.rangeOffset = rangeOffset;
        return asDerivedType();
    }

    public T setMethod(String method) {
        this.method = method;
        return asDerivedType();
    }

    public T setCharset(Charset charset) {
        this.charset = charset;
        return asDerivedType();
    }

    public T setConnectionPoolPartitioning(ConnectionPoolPartitioning connectionPoolPartitioning) {
        this.connectionPoolPartitioning = connectionPoolPartitioning;
        return asDerivedType();
    }

    public T setNameResolver(NameResolver nameResolver) {
        this.nameResolver = nameResolver;
        return asDerivedType();
    }

    public T setSignatureCalculator(SignatureCalculator signatureCalculator) {
        this.signatureCalculator = signatureCalculator;
        return asDerivedType();
    }

    private RequestBuilderBase<?> executeSignatureCalculator() {
        if (signatureCalculator == null)
            return this;

        // build a first version of the request, without signatureCalculator in play
        RequestBuilder rb = new RequestBuilder(this.method);
        // make copy of mutable collections so we don't risk affecting
        // original RequestBuilder
        // call setFormParams first as it resets other fields
        if (this.formParams != null)
            rb.setFormParams(this.formParams);
        if (this.headers != null)
            rb.headers.add(this.headers);
        if (this.cookies != null)
            rb.setCookies(this.cookies);
        if (this.bodyParts != null)
            rb.setBodyParts(this.bodyParts);

        // copy all other fields
        // but rb.signatureCalculator, that's the whole point here
        rb.uriEncoder = this.uriEncoder;
        rb.queryParams = this.queryParams;
        rb.uri = this.uri;
        rb.address = this.address;
        rb.localAddress = this.localAddress;
        rb.byteData = this.byteData;
        rb.compositeByteData = this.compositeByteData;
        rb.stringData = this.stringData;
        rb.byteBufferData = this.byteBufferData;
        rb.streamData = this.streamData;
        rb.bodyGenerator = this.bodyGenerator;
        rb.virtualHost = this.virtualHost;
        rb.contentLength = this.contentLength;
        rb.proxyServer = this.proxyServer;
        rb.realm = this.realm;
        rb.file = this.file;
        rb.followRedirect = this.followRedirect;
        rb.requestTimeout = this.requestTimeout;
        rb.rangeOffset = this.rangeOffset;
        rb.charset = this.charset;
        rb.connectionPoolPartitioning = this.connectionPoolPartitioning;
        rb.nameResolver = this.nameResolver;
        Request unsignedRequest = rb.build();
        signatureCalculator.calculateAndAddSignature(unsignedRequest, rb);
        return rb;
    }

    private Charset computeCharset() {
        if (this.charset == null) {
            try {
                final String contentType = this.headers.get(HttpHeaders.Names.CONTENT_TYPE);
                if (contentType != null) {
                    final Charset charset = parseCharset(contentType);
                    if (charset != null) {
                        // ensure that if charset is provided with the
                        // Content-Type header,
                        // we propagate that down to the charset of the Request
                        // object
                        return charset;
                    }
                }
            } catch (Throwable e) {
                // NoOp -- we can't fix the Content-Type or charset from here
            }
        }
        return this.charset;
    }

    private long computeRequestContentLength() {
        if (this.contentLength < 0 && this.streamData == null) {
            // can't concatenate content-length
            final String contentLength = this.headers.get(HttpHeaders.Names.CONTENT_LENGTH);

            if (contentLength != null) {
                try {
                    return Long.parseLong(contentLength);
                } catch (NumberFormatException e) {
                    // NoOp -- we won't specify length so it will be chunked?
                }
            }
        }
        return this.contentLength;
    }

    private Uri computeUri() {

        Uri tempUri = this.uri;
        if (tempUri == null) {
            LOGGER.debug("setUrl hasn't been invoked. Using {}", DEFAULT_REQUEST_URL);
            tempUri = DEFAULT_REQUEST_URL;
        } else {
            validateSupportedScheme(tempUri);
        }

        return uriEncoder.encode(tempUri, queryParams);
    }

    public Request build() {
        RequestBuilderBase<?> rb = executeSignatureCalculator();
        Uri finalUri = rb.computeUri();
        Charset finalCharset = rb.computeCharset();
        long finalContentLength = rb.computeRequestContentLength();

        // make copies of mutable internal collections
        List<Cookie> cookiesCopy = rb.cookies == null ? Collections.emptyList() : new ArrayList<>(rb.cookies);
        List<Param> formParamsCopy = rb.formParams == null ? Collections.emptyList() : new ArrayList<>(rb.formParams);
        List<Part> bodyPartsCopy = rb.bodyParts == null ? Collections.emptyList() : new ArrayList<>(rb.bodyParts);

        return new DefaultRequest(rb.method,//
                finalUri,//
                rb.address,//
                rb.localAddress,//
                rb.headers,//
                cookiesCopy,//
                rb.byteData,//
                rb.compositeByteData,//
                rb.stringData,//
                rb.byteBufferData,//
                rb.streamData,//
                rb.bodyGenerator,//
                formParamsCopy,//
                bodyPartsCopy,//
                rb.virtualHost,//
                finalContentLength,//
                rb.proxyServer,//
                rb.realm,//
                rb.file,//
                rb.followRedirect,//
                rb.requestTimeout,//
                rb.rangeOffset,//
                finalCharset,//
                rb.connectionPoolPartitioning,//
                rb.nameResolver);
    }
}
