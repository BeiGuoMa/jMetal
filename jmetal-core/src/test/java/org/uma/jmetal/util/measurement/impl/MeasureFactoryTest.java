package org.uma.jmetal.util.measurement.impl;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Test;
import org.uma.jmetal.util.measurement.MeasureListener;
import org.uma.jmetal.util.measurement.PullMeasure;
import org.uma.jmetal.util.measurement.PushMeasure;

public class MeasureFactoryTest {

	@Test
	public void testCreatePullFromPush() {
		MeasureFactory factory = new MeasureFactory();
		SimplePushMeasure<Integer> push = new SimplePushMeasure<>();
		PullMeasure<Integer> pull = factory.createPullFromPush(push, null);

		assertEquals(null, (Object) pull.get());
		push.push(3);
		assertEquals(3, (Object) pull.get());
		push.push(5);
		assertEquals(5, (Object) pull.get());
		push.push(null);
		assertEquals(null, (Object) pull.get());
		push.push(-65);
		push.push(8);
		push.push(4);
		push.push(-10);
		assertEquals(-10, (Object) pull.get());
	}

	@Test
	public void testCreatePushFromPullNotifiesWithTheCorrectFrequency()
			throws InterruptedException {
		// create the pull meaure
		PullMeasure<Long> pull = new SimplePullMeasure<Long>() {
			@Override
			public Long get() {
				return System.currentTimeMillis();
			}
		};

		// create the push measure
		MeasureFactory factory = new MeasureFactory();
		final int period = 10;
		PushMeasure<Long> push = factory.createPushFromPull(pull, period);

		// register for notifications from now
		final long start = System.currentTimeMillis();
		final LinkedList<Long> timestamps = new LinkedList<>();
		MeasureListener<Long> listener = new MeasureListener<Long>() {

			@Override
			public void measureGenerated(Long value) {
				timestamps.add(System.currentTimeMillis() - start);
			}
		};
		push.register(listener);

		// wait for notifications to come
		Thread.sleep(10 * period);

		// check we have between 80%-120% of the expected notifications
		assertTrue("" + timestamps.size(), timestamps.size() >= 8);
		assertTrue("" + timestamps.size(), timestamps.size() <= 12);

		// check the average period is between 80%-120% of the specified one
		long average = timestamps.getLast() / timestamps.size();
		assertTrue("" + average, average >= period * 0.8);
		assertTrue("" + average, average <= period * 1.2);
	}

	@Test
	public void testCreatePushFromPullStopNotificationsWhenPullDestroyed()
			throws InterruptedException {
		// create the pull measure
		PullMeasure<Long> pull = new SimplePullMeasure<Long>() {
			@Override
			public Long get() {
				return System.currentTimeMillis();
			}
		};

		// create the push measure
		MeasureFactory factory = new MeasureFactory();
		final int period = 10;
		PushMeasure<Long> push = factory.createPushFromPull(pull, period);

		// destroy the pull measure
		pull = null;
		System.gc();
		System.gc();

		// register for notifications only from now
		final LinkedList<Long> timestamps = new LinkedList<>();
		push.register(new MeasureListener<Long>() {

			@Override
			public void measureGenerated(Long value) {
				timestamps.add(value);
			}
		});

		// check no notifications are coming anymore
		Thread.sleep(10 * period);
		assertEquals(0, timestamps.size());
	}

	@Test
	public void testCreatePushFromPullNotifiesOnlyWhenValueChanged()
			throws InterruptedException {
		// create the pull measure
		final Integer[] value = { null };
		PullMeasure<Integer> pull = new SimplePullMeasure<Integer>() {
			@Override
			public Integer get() {
				return value[0];
			}
		};

		// create the push measure
		MeasureFactory factory = new MeasureFactory();
		final int period = 10;
		PushMeasure<Integer> push = factory.createPushFromPull(pull, period);

		// register for notifications from now
		final LinkedList<Integer> notified = new LinkedList<>();
		push.register(new MeasureListener<Integer>() {

			@Override
			public void measureGenerated(Integer value) {
				notified.add(value);
			}
		});

		Thread.sleep(10 * period);
		assertEquals(0, notified.size());

		value[0] = 56;
		Thread.sleep(10 * period);
		assertEquals(1, notified.size());
		assertEquals(56, (Object) notified.get(0));

		value[0] = 43;
		Thread.sleep(10 * period);
		assertEquals(2, notified.size());
		assertEquals(56, (Object) notified.get(0));
		assertEquals(43, (Object) notified.get(1));

		value[0] = -43;
		Thread.sleep(10 * period);
		assertEquals(3, notified.size());
		assertEquals(56, (Object) notified.get(0));
		assertEquals(43, (Object) notified.get(1));
		assertEquals(-43, (Object) notified.get(2));

		Thread.sleep(10 * period);
		assertEquals(3, notified.size());
		assertEquals(56, (Object) notified.get(0));
		assertEquals(43, (Object) notified.get(1));
		assertEquals(-43, (Object) notified.get(2));
	}
}
