package com.qcl.launcher.utils.io;

import com.google.gson.JsonParseException;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.Logging;
import com.qcl.launcher.utils.function.ExceptionalFunction;
import com.qcl.launcher.utils.gson.JsonUtils;
import fi.iki.elonen.NanoHTTPD;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes2.dex */
public class HttpServer extends NanoHTTPD {
    protected final List<Route> routes;
    private int traceId;

    public HttpServer(int i) {
        super(i);
        this.traceId = 0;
        this.routes = new ArrayList();
    }

    public HttpServer(String str, int i) {
        super(str, i);
        this.traceId = 0;
        this.routes = new ArrayList();
    }

    public String getRootUrl() {
        return "http://localhost:" + getListeningPort();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addRoute(NanoHTTPD.Method method, Pattern pattern, ExceptionalFunction<Request, NanoHTTPD.Response, ?> exceptionalFunction) {
        this.routes.add(new DefaultRoute(method, pattern, exceptionalFunction));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static NanoHTTPD.Response ok(Object obj) {
        Logging.LOG.info(String.format("Response %s", JsonUtils.GSON.toJson(obj)));
        return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/json", JsonUtils.GSON.toJson(obj));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static NanoHTTPD.Response notFound() {
        return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", "404 not found");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static NanoHTTPD.Response noContent() {
        return newFixedLengthResponse(NanoHTTPD.Response.Status.NO_CONTENT, "text/html", "");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static NanoHTTPD.Response badRequest() {
        return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "text/html", "400 bad request");
    }

    protected static NanoHTTPD.Response internalError() {
        return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/html", "500 internal error");
    }

    @Override // fi.iki.elonen.NanoHTTPD
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession iHTTPSession) {
        NanoHTTPD.Response response;
        int i = this.traceId;
        this.traceId = i + 1;
        Logging.LOG.info(String.format("[%d] %s --> %s", Integer.valueOf(i), iHTTPSession.getMethod().name(), iHTTPSession.getUri() + ((String) Optional.ofNullable(iHTTPSession.getQueryParameterString()).map(new Function() { // from class: com.qcl.launcher.utils.io.HttpServer$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return HttpServer.lambda$serve$0((String) obj);
            }
        }).orElse(""))));
        Iterator<Route> it = this.routes.iterator();
        while (true) {
            if (!it.hasNext()) {
                response = null;
                break;
            }
            Route next = it.next();
            if (next.method == iHTTPSession.getMethod()) {
                Matcher matcher = next.pathPattern.matcher(iHTTPSession.getUri());
                if (matcher.find()) {
                    response = next.serve(new Request(matcher, Lang.mapOf(NetworkUtils.parseQuery(iHTTPSession.getQueryParameterString())), iHTTPSession));
                    break;
                }
            }
        }
        if (response == null) {
            response = notFound();
        }
        Logging.LOG.info(String.format("[%d] %s <--", Integer.valueOf(i), response.getStatus()));
        return response;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String lambda$serve$0(String str) {
        return "?" + str;
    }

    /* loaded from: classes2.dex */
    public static abstract class Route {
        NanoHTTPD.Method method;
        Pattern pathPattern;

        public abstract NanoHTTPD.Response serve(Request request);

        public Route(NanoHTTPD.Method method, Pattern pattern) {
            this.method = method;
            this.pathPattern = pattern;
        }

        public NanoHTTPD.Method getMethod() {
            return this.method;
        }

        public Pattern getPathPattern() {
            return this.pathPattern;
        }
    }

    /* loaded from: classes2.dex */
    public static class DefaultRoute extends Route {
        private final ExceptionalFunction<Request, NanoHTTPD.Response, ?> server;

        public DefaultRoute(NanoHTTPD.Method method, Pattern pattern, ExceptionalFunction<Request, NanoHTTPD.Response, ?> exceptionalFunction) {
            super(method, pattern);
            this.server = exceptionalFunction;
        }

        @Override // com.qcl.launcher.utils.io.HttpServer.Route
        public NanoHTTPD.Response serve(Request request) {
            try {
                return this.server.apply(request);
            } catch (JsonParseException unused) {
                return HttpServer.badRequest();
            } catch (Exception e) {
                Logging.LOG.log(Level.SEVERE, "Error handling " + request.getSession().getUri(), (Throwable) e);
                return HttpServer.internalError();
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class Request {
        Matcher pathVariables;
        Map<String, String> query;
        NanoHTTPD.IHTTPSession session;

        public Request(Matcher matcher, Map<String, String> map, NanoHTTPD.IHTTPSession iHTTPSession) {
            this.pathVariables = matcher;
            this.query = map;
            this.session = iHTTPSession;
        }

        public Matcher getPathVariables() {
            return this.pathVariables;
        }

        public Map<String, String> getQuery() {
            return this.query;
        }

        public NanoHTTPD.IHTTPSession getSession() {
            return this.session;
        }
    }
}
