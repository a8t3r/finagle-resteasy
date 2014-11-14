package com.opower.finagle.resteasy.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jboss.netty.handler.codec.http.HttpResponse;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the {@link javax.ws.rs.core.MultivaluedMap} API on top of
 * a Netty response object, so we can get Resteasy to write headers to
 * the correct location.
 *
 * @author ed.peters
 */
public class NettyHeaderWrapper implements MultivaluedMap<String,Object> {

    private final HttpResponse nettyResponse;

    public NettyHeaderWrapper(HttpResponse nettyResponse) {
        this.nettyResponse = nettyResponse;
    }

    @Override
    public void add(String key, Object value) {
        nettyResponse.addHeader(key, value);
    }

    @Override
    public void putSingle(String key, Object value) {
        nettyResponse.setHeader(key, value);
    }

    @Override
    public Object getFirst(String key) {
        return nettyResponse.getHeader(key);
    }

    @Override
    public void addAll(String key, Object... newValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(String key, List<Object> valueList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFirst(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equalsIgnoreValueOrder(MultivaluedMap<String, Object> otherMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return nettyResponse.getHeaderNames().size();
    }

    @Override
    public boolean isEmpty() {
        return size() > 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return nettyResponse.containsHeader(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("containsValue");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> get(Object key) {
        // OK because we're returning a List
        return (List) nettyResponse.getHeaders(key.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> put(String key, List<Object> values) {
        // this is safe -- we're converting List<String> to List<Object>
        List<Object> oldValue = (List) nettyResponse.getHeaders(key.toString());
        nettyResponse.setHeader(key, values);
        return oldValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> remove(Object key) {
        // this is safe -- we're converting List<String> to List<Object>
        List<Object> oldValue = (List) nettyResponse.getHeaders(key.toString());
        nettyResponse.removeHeader(key.toString());
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<Object>> map) {
        for (String key : map.keySet()) {
            put(key, map.get(key));
        }
    }

    @Override
    public void clear() {
        nettyResponse.clearHeaders();
    }

    @Override
    public Set<String> keySet() {
        return nettyResponse.getHeaderNames();
    }

    @Override
    public Collection<List<Object>> values() {
        List<List<Object>> all = Lists.newArrayList();
        for (String key : keySet()) {
            all.add(get(key));
        }
        return all;
    }

    @Override
    public Set<Entry<String,List<Object>>> entrySet() {
        Map<String,List<Object>> map = Maps.newHashMap();
        for (String key : keySet()) {
            map.put(key, get(key));
        }
        return map.entrySet();
    }
}
