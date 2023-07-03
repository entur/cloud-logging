package no.entur.logging.cloud.api;

import org.slf4j.Marker;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * Default marker implementation, copied from Logback / BasicMarker.
 *
 */
public class DevOpsMarker implements Marker {

	public static DevOpsMarker errorTellMeTomorrow() {
		return new DevOpsMarker(DevOpsLevel.ERROR_TELL_ME_TOMORROW);
	}

	public static DevOpsMarker errorWakeMeUpRightNow() {
		return new DevOpsMarker(DevOpsLevel.ERROR_WAKE_ME_UP_RIGHT_NOW);
	}

	public static DevOpsMarker errorInterruptMyDinner() {
		return new DevOpsMarker(DevOpsLevel.ERROR_INTERRUPT_MY_DINNER);
	}

	public static DevOpsMarker errorTellMeTomorrow(Marker marker) {
		DevOpsMarker devOpsMarker = errorTellMeTomorrow();
		marker.add(devOpsMarker);
		return devOpsMarker;
	}

	public static DevOpsMarker errorWakeMeUpRightNow(Marker marker) {
		DevOpsMarker devOpsMarker = errorWakeMeUpRightNow();
		marker.add(devOpsMarker);
		return devOpsMarker;
	}

	public static DevOpsMarker errorInterruptMyDinner(Marker marker) {
		DevOpsMarker devOpsMarker = errorInterruptMyDinner();
		marker.add(devOpsMarker);
		return devOpsMarker;
	}

	public static final String MARKER_NAME = "DEVOPS_MARKER";

	private final DevOpsLevel devOpsLevel;

	public DevOpsMarker(DevOpsLevel devOpsLevel) {
		this.devOpsLevel = devOpsLevel;
	}

	/**
	 * Referenced markers - initialized the first time a marker is added
	 */
	private volatile List<Marker> referenceList;

	public DevOpsLevel getDevOpsLevel() {
		return devOpsLevel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		try {
			throw new RuntimeException();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return MARKER_NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(Marker reference) {
		Objects.requireNonNull(reference);

		// no point in adding the reference multiple times
		if (this.contains(reference)) {
			return;
		}
		if (reference.contains(this)) { // avoid recursion, a potential reference should not hold its future "parent" as a reference
			return;
		}

		if (referenceList == null) {
			synchronized (this) {
				if (referenceList == null) {
					referenceList = new CopyOnWriteArrayList<>();
				}
			}
		}
		referenceList.add(reference);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasReferences() {
		return referenceList != null && !referenceList.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Deprecated
	@Override
	public boolean hasChildren() {
		return hasReferences();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Marker> iterator() {
		if (referenceList == null) {
			return Collections.emptyIterator();
		}
		return referenceList.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Marker referenceToRemove) {
		if (hasReferences()) {
			return referenceList.remove(referenceToRemove);
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Marker other) {
		if (this.equals(other)) {
			return true;
		}

		if (hasReferences()) {
			for (Marker ref : referenceList) {
				if (ref.contains(other)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(String name) {
		if (this.MARKER_NAME.equals(name)) {
			return true;
		}

		if (hasReferences()) {
			for (Marker ref : referenceList) {
				if (ref.contains(name)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DevOpsMarker that = (DevOpsMarker) o;
		return devOpsLevel == that.devOpsLevel;
	}

	@Override
	public int hashCode() {
		return Objects.hash(devOpsLevel);
	}

	public String toString() {
		if (!this.hasReferences()) {
			return this.getName();
		}
		StringBuilder sb = new StringBuilder(this.getName())
				.append(" [ ");
		Iterator<Marker> it = this.iterator();
		while (it.hasNext()) {
			sb.append(it.next().getName());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(" ]");

		return sb.toString();
	}

	public static DevOpsLevel searchSeverityMarker(Marker marker) {
		if (marker instanceof DevOpsMarker) {
			DevOpsMarker devOpsMarker = (DevOpsMarker) marker;

			return devOpsMarker.getDevOpsLevel();
		} else if(marker.hasReferences()) {
			Iterator<Marker> iterator = marker.iterator();
			while(iterator.hasNext()) {
				DevOpsLevel level = searchSeverityMarker(iterator.next());
				if(level != null) {
					return level;
				}
			}
		}
		return null;
	}
}
