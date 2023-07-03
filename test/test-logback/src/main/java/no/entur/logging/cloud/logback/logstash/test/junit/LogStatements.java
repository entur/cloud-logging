package no.entur.logging.cloud.logback.logstash.test.junit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Log statement / event wrapper (read only list).
 * 
 */

public class LogStatements implements List<LogStatement> {
	
	/**
	 * 
	 * There seems to be a small delay in receiving all the log events.
	 * 
	 */
	
	protected final List<LogStatement> statements = new ArrayList<>(1024);
	protected final long flushDelay;
	
	public LogStatements(long flushDelay) {
		super();
		this.flushDelay = flushDelay;
	}

	/**
	 * In order to support log statements both before and after this object is injected in the unit test, 
	 * make sure it collects the most recent events whenever accessed.
	 */

	protected void refresh() {
	}

	public void forEach(Consumer<? super LogStatement> action) {
		waitForFlushAndRefresh();
		statements.forEach(action);
	}

	public int size() {
		waitForFlushAndRefresh();
		return statements.size();
	}

	public boolean isEmpty() {
		// wait for flushing
		long timeout = System.currentTimeMillis() + flushDelay;

		do {
			refresh();
			if (!statements.isEmpty()) {
				return false;
			}
			Thread.yield();
		} while (System.currentTimeMillis() < timeout);

		return true;
	}

	public boolean contains(Object o) {
		if(!statements.contains(o)) {
			waitForFlushAndRefresh();
		}
		return statements.contains(o);
	}

	public Iterator<LogStatement> iterator() {
		waitForFlushAndRefresh();
		return statements.iterator();
	}

	public Object[] toArray() {
		waitForFlushAndRefresh();
		return statements.toArray();
	}

	public <T> T[] toArray(T[] a) {
		waitForFlushAndRefresh();
		return statements.toArray(a);
	}

	public boolean add(LogStatement e) {
		throw new IllegalArgumentException();
	}

	public boolean remove(Object o) {
		throw new IllegalArgumentException();
	}

	public boolean containsAll(Collection<?> c) {
		waitForFlushAndRefresh();
		return statements.containsAll(c);
	}

	public boolean addAll(Collection<? extends LogStatement> c) {
		throw new IllegalArgumentException();
	}

	public boolean addAll(int index, Collection<? extends LogStatement> c) {
		throw new IllegalArgumentException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new IllegalArgumentException();
	}

	public <T> T[] toArray(IntFunction<T[]> generator) {
		waitForFlushAndRefresh();
        return toArray(generator.apply(0));
	}

	public boolean retainAll(Collection<?> c) {
		throw new IllegalArgumentException();	}

	public void replaceAll(UnaryOperator<LogStatement> operator) {
		throw new IllegalArgumentException();
	}

	public void sort(Comparator<? super LogStatement> c) {
		waitForFlushAndRefresh();
		statements.sort(c);
	}

	public void clear() {
		waitForFlushAndRefresh();
		statements.clear();
	}

	public boolean equals(Object o) {
		waitForFlushAndRefresh();
		return statements.equals(o);
	}

	public int hashCode() {
		waitForFlushAndRefresh();
		return statements.hashCode();
	}

	public LogStatement get(int index) {
		waitForFlushAndRefresh(index + 1);
		return statements.get(index);
	}

	public boolean removeIf(Predicate<? super LogStatement> filter) {
		throw new IllegalArgumentException();
	}

	public LogStatement set(int index, LogStatement element) {
		throw new IllegalArgumentException();
	}

	public void add(int index, LogStatement element) {
		throw new IllegalArgumentException();
	}

	public LogStatement remove(int index) {
		throw new IllegalArgumentException();
	}

	public int indexOf(Object o) {
		waitForFlushAndRefresh();
		return statements.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		waitForFlushAndRefresh();
		return statements.lastIndexOf(o);
	}

	public ListIterator<LogStatement> listIterator() {
		waitForFlushAndRefresh();
		return statements.listIterator();
	}

	public ListIterator<LogStatement> listIterator(int index) {
		waitForFlushAndRefresh();
		return statements.listIterator(index);
	}

	public List<LogStatement> subList(int fromIndex, int toIndex) {
		waitForFlushAndRefresh();
		return statements.subList(fromIndex, toIndex);
	}

	public Spliterator<LogStatement> spliterator() {
		refresh();
		return statements.spliterator();
	}

	public Stream<LogStatement> stream() {
		waitForFlushAndRefresh();
		return statements.stream();
	}

	public Stream<LogStatement> parallelStream() {
		waitForFlushAndRefresh();
		return statements.parallelStream();
	}

	@Override
	public String toString() {
		waitForFlushAndRefresh();
		return statements.toString();
	}
	
	public LogStatement first() {
		return get(0);
	}
	
	public LogStatement getLast() {
		waitForFlushAndRefresh();
		return statements.get(statements.size() - 1);
	}
	
	public List<String> getMessages() {
		waitForFlushAndRefresh();
		List<String> messages = new ArrayList<String>(statements.size());
		for (LogStatement logStatement : statements) {
			messages.add(logStatement.getMessage());
		}
		return messages;
	}
	
	public List<LogStatement> forLogger(Class<?> cls) {
		return forLogger(cls.getName());
	}
	
	public LogStatements forLogger(String string) {
		return new SingleLoggerLogStatements(this, string, flushDelay);
	}

	/**
	 * Get request-/response-logger.
	 *
	 * @return log statements
	 */

	public LogStatements forHttpLogger() {
		return forLogger("org.entur.http");
	}

	public LogStatements forGrpcHttpLogger() {
		return forLogger("org.entur.logging.grpc.AbstractGrpcServerLoggingInterceptor");
	}

	protected void waitForFlushAndRefresh() {
		try {
			Thread.sleep(flushDelay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // make sonarcloud happy
			throw new RuntimeException(e);
		} finally {
			refresh();
		}
	}

	protected void waitForFlushAndRefresh(int size) {
		long timeout = System.currentTimeMillis() + flushDelay;
		
		do {
			refresh();
			if(System.currentTimeMillis() > timeout) {
				throw new IllegalArgumentException("Expected size at least " + size + ", got " + statements.size() + " after waiting " + flushDelay + "ms");
			}
			Thread.yield();
		} while(statements.size() < size);
	}
	
	protected List<LogStatement> getStatements() {
		return statements;
	}
	
	
}
