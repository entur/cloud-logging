package no.entur.logging.cloud.logback.logstash.test;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.marker.LogstashBasicMarker;
import net.logstash.logback.marker.LogstashMarker;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputMarker;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputType;
import org.slf4j.Marker;

import java.io.IOException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class DelegateCompositeConsoleOutputMarker extends LogstashMarker implements CompositeConsoleOutputMarker {

    private final LogstashMarker delegate;
    private final CompositeConsoleOutputType compositeConsoleOutputType;

    public DelegateCompositeConsoleOutputMarker(LogstashMarker delegate, CompositeConsoleOutputType compositeConsoleOutputType) {
        super(DelegateCompositeConsoleOutputMarker.class.getName());
        this.delegate = delegate;
        this.compositeConsoleOutputType = compositeConsoleOutputType;
    }

    public Marker getDelegate() {
        return delegate;
    }

    public CompositeConsoleOutputType getCompositeConsoleOutputType() {
        return compositeConsoleOutputType;
    }

    @Override
    public <T extends LogstashMarker> T and(Marker reference) {
        return delegate.and(reference);
    }

    @Override
    @Deprecated
    public <T extends LogstashMarker> T with(Marker reference) {
        return delegate.with(reference);
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        delegate.writeTo(generator);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void add(Marker reference) {
        delegate.add(reference);
    }

    @Override
    public boolean hasReferences() {
        return delegate.hasReferences();
    }

    @Override
    public boolean hasChildren() {
        return delegate.hasChildren();
    }

    @Override
    public Iterator<Marker> iterator() {
        return delegate.iterator();
    }

    @Override
    public boolean remove(Marker referenceToRemove) {
        return delegate.remove(referenceToRemove);
    }

    @Override
    public boolean contains(Marker other) {
        return delegate.contains(other);
    }

    @Override
    public boolean contains(String name) {
        return delegate.contains(name);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public void forEach(Consumer<? super Marker> action) {
        delegate.forEach(action);
    }

    @Override
    public Spliterator<Marker> spliterator() {
        return delegate.spliterator();
    }
}
