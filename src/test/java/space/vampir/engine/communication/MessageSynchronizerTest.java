package space.vampir.engine.communication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import space.vampir.engine.communication.synchronizer.ClosestMessageSynchronizer;
import space.vampir.engine.communication.synchronizer.LatestMessageSynchronizer;
import space.vampir.engine.message.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MessageSynchronizerTest {

    private static class DummyMessage extends Message {

        DummyMessage(long timestamp) {
            super(timestamp);
        }
    }

    private static void assertEquals(int[] expected, int[] actual) {
        if (expected == null || actual == null) {
            Assertions.assertNull(expected, "Expected " + Arrays.toString(expected) + " but was null");
            Assertions.assertNull(actual, "Expected null but was " + Arrays.toString(actual));
            return;
        }
        Assertions.assertEquals(expected.length, actual.length, "Arrays are different: " + Arrays.toString(expected) + " vs " + Arrays.toString(actual));
        for (int i = 0; i < expected.length; i++) {
            Assertions.assertEquals(expected[i], actual[i], "Arrays are different: " + Arrays.toString(expected) + " vs " + Arrays.toString(actual));
        }
    }

    private static void testLatest(int[] expected, List<List<? extends Message>> messageQueues) {
        var synchronizer = new LatestMessageSynchronizer(200L, List.of("0"));
        synchronizer.setTopicIndices(Map.of("0", 0));
        var closestMessages = synchronizer.getSynchronizedMessageIndices(messageQueues);
        assertEquals(expected, closestMessages);
    }

    @Test
    public void testLatestMessageSynchronizer() {
        List<List<? extends Message>> m1 = List.of(
                List.of(new DummyMessage(100), new DummyMessage(200), new DummyMessage(300)),
                List.of(new DummyMessage(150), new DummyMessage(250), new DummyMessage(350)),
                List.of(new DummyMessage(120), new DummyMessage(220), new DummyMessage(320))
        );
        testLatest(new int[]{2, 2, 2}, m1);

        List<List<? extends Message>> m2 = List.of(
                List.of(new DummyMessage(100), new DummyMessage(200), new DummyMessage(300)),
                List.of(new DummyMessage(150), new DummyMessage(250)),
                List.of(new DummyMessage(120), new DummyMessage(220), new DummyMessage(320))
        );
        testLatest(new int[]{2, 1, 2}, m2);

        List<List<? extends Message>> m3 = List.of(
                List.of(new DummyMessage(200), new DummyMessage(250)),
                List.of(new DummyMessage(250), new DummyMessage(500)),
                List.of(new DummyMessage(200), new DummyMessage(350))
        );
        testLatest(null, m3);

        List<List<? extends Message>> m4 = List.of(
                List.of(new DummyMessage(200), new DummyMessage(500)),
                List.of(new DummyMessage(250), new DummyMessage(400)),
                List.of(new DummyMessage(200), new DummyMessage(250))
        );
        testLatest(new int[]{1, 1, -1}, m4);
    }

    private static void testClosest(int[] expected, List<List<? extends Message>> messageQueues) {
        var synchronizer = new ClosestMessageSynchronizer(200L,
                List.of("0"),
                Map.of("0", 1, "1", 2, "2", 3));
        synchronizer.setTopicIndices(Map.of("0", 0, "1", 1, "2", 2));
        var closestMessages = synchronizer.getSynchronizedMessageIndices(messageQueues);
        assertEquals(expected, closestMessages);
    }

    @Test
    public void testClosestMessageSynchronizer() {
        List<List<? extends Message>> m1 = List.of(
                List.of(new DummyMessage(100), new DummyMessage(200), new DummyMessage(300)),
                List.of(new DummyMessage(150), new DummyMessage(250), new DummyMessage(350)),
                List.of(new DummyMessage(120), new DummyMessage(220), new DummyMessage(320))
        );
        testClosest(new int[]{2, 2, 2}, m1);

        List<List<? extends Message>> m2 = List.of(
                List.of(new DummyMessage(100), new DummyMessage(200), new DummyMessage(300)),
                List.of(new DummyMessage(150), new DummyMessage(250)),
                List.of(new DummyMessage(120), new DummyMessage(220), new DummyMessage(320))
        );
        testClosest(new int[]{1, 1, 1}, m2);

        List<List<? extends Message>> m3 = List.of(
                List.of(new DummyMessage(190), new DummyMessage(200), new DummyMessage(280)),
                List.of(new DummyMessage(150), new DummyMessage(250), new DummyMessage(350)),
                List.of(new DummyMessage(120), new DummyMessage(240), new DummyMessage(320))
        );
        testClosest(new int[]{2, 1, 1}, m3);

        List<List<? extends Message>> m4 = List.of(
                List.of(new DummyMessage(190), new DummyMessage(200), new DummyMessage(280)),
                List.of(new DummyMessage(150), new DummyMessage(160), new DummyMessage(170)),
                List.of(new DummyMessage(120), new DummyMessage(180), new DummyMessage(200))
        );
        testClosest(new int[]{0, 2, 1}, m4);

        List<List<? extends Message>> m5 = List.of(
                List.of(new DummyMessage(190), new DummyMessage(200), new DummyMessage(280)),
                List.of(new DummyMessage(150), new DummyMessage(160), new DummyMessage(370)),
                List.of()
        );
        testClosest(new int[]{0, 1, -1}, m5);

        List<List<? extends Message>> m6 = List.of(
                List.of(new DummyMessage(150), new DummyMessage(160), new DummyMessage(370)),
                List.of(),
                List.of()
        );
        testClosest(new int[]{2, -1, -1}, m6);

        List<List<? extends Message>> m7 = List.of(
                List.of(),
                List.of(new DummyMessage(150), new DummyMessage(160), new DummyMessage(370)),
                List.of()
        );
        testClosest(null, m7);

        List<List<? extends Message>> m8 = List.of(
                List.of(),
                List.of(),
                List.of()
        );
        testClosest(null, m8);

        List<List<? extends Message>> m9 = List.of(
                List.of(new DummyMessage(200)),
                List.of(new DummyMessage(350)),
                List.of(new DummyMessage(100))
        );
        testClosest(new int[]{0, 0, -1}, m9);

        List<List<? extends Message>> m10 = List.of(
                List.of(new DummyMessage(200)),
                List.of(),
                List.of(),
                List.of(new DummyMessage(80)),
                List.of(new DummyMessage(350))
        );
        testClosest(new int[]{0, -1, -1, 0, -1}, m10);
    }
}
