package fish.focus.uvms.ais;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AISManagedConnectionTest {

    @Test
    public void withoutCommentBlockTest() throws IOException {
        String payload = UUID.randomUUID().toString();
        AISManagedConnection aisManagedConnection = new AISManagedConnection(null);
        aisManagedConnection.getSentences();
        BufferedReader bufferMock = mock(BufferedReader.class);
        when(bufferMock.readLine()).thenReturn("!ABVDM,1,1,0,B," + payload + ",0*7D", null);
        aisManagedConnection.read(bufferMock);
        List<Sentence> sentences = aisManagedConnection.getSentences();
        assertThat(sentences.size(), is(1));
        assertThat(sentences.get(0).hasValidCommentBlock(), is(false));
        assertThat(sentences.get(0).getCommentBlockLesTimestamp(), nullValue());
        assertThat(sentences.get(0).getSentence(), is(payload));
    }

    @Test
    public void commentBlockTest() throws IOException {
        String payload = UUID.randomUUID().toString();
        AISManagedConnection aisManagedConnection = new AISManagedConnection(null);
        aisManagedConnection.getSentences();
        BufferedReader bufferMock = mock(BufferedReader.class);
        when(bufferMock.readLine()).thenReturn("\\1G1:32,s:516,c:1652227200*5B\\!ABVDM,1,1,0,B," + payload + ",0*7D", null);
        aisManagedConnection.read(bufferMock);
        List<Sentence> sentences = aisManagedConnection.getSentences();
        assertThat(sentences.size(), is(1));
        assertThat(sentences.get(0).hasValidCommentBlock(), is(true));
        assertThat(sentences.get(0).getCommentBlockLesTimestamp(), is(Instant.ofEpochSecond(1652227200)));
        assertThat(sentences.get(0).getSentence(), is(payload));
    }

    @Test
    public void commentBlockTwoSentencesTest() throws IOException {
        String payload = UUID.randomUUID().toString();
        String payload2 = UUID.randomUUID().toString();
        AISManagedConnection aisManagedConnection = new AISManagedConnection(null);
        aisManagedConnection.getSentences();
        BufferedReader bufferMock = mock(BufferedReader.class);
        when(bufferMock.readLine()).thenReturn("\\1G1:32,s:516,c:1652227200*5B\\!ABVDM,1,1,0,B," + payload + ",0*7D",
                "\\1G1:441,s:1184,c:1652227202*57\\!ABVDM,1,1,0,B," + payload2 + ",0*7D", null);
        aisManagedConnection.read(bufferMock);
        List<Sentence> sentences = aisManagedConnection.getSentences();
        assertThat(sentences.size(), is(2));
        assertThat(sentences.get(0).hasValidCommentBlock(), is(true));
        assertThat(sentences.get(0).getCommentBlockLesTimestamp(), is(Instant.ofEpochSecond(1652227200)));
        assertThat(sentences.get(0).getSentence(), is(payload));
        assertThat(sentences.get(1).hasValidCommentBlock(), is(true));
        assertThat(sentences.get(1).getCommentBlockLesTimestamp(), is(Instant.ofEpochSecond(1652227202)));
        assertThat(sentences.get(1).getSentence(), is(payload2));
    }

    @Test
    public void commentBlockMultiPartTest() throws IOException {
        String payload = UUID.randomUUID().toString();
        String payload2 = UUID.randomUUID().toString();
        AISManagedConnection aisManagedConnection = new AISManagedConnection(null);
        aisManagedConnection.getSentences();
        BufferedReader bufferMock = mock(BufferedReader.class);
        when(bufferMock.readLine()).thenReturn("\\1G2:310,s:452,c:1652227201*6B\\!ABVDM,2,1,1,A," + payload + ",0*1A",
                "\\2G2:310*4F\\!ABVDM,2,2,1,A," + payload2 + ",2*26", null);
        aisManagedConnection.read(bufferMock);
        List<Sentence> sentences = aisManagedConnection.getSentences();
        assertThat(sentences.size(), is(1));
        assertThat(sentences.get(0).hasValidCommentBlock(), is(true));
        assertThat(sentences.get(0).getCommentBlockLesTimestamp(), is(Instant.ofEpochSecond(1652227201)));
        assertThat(sentences.get(0).getSentence(), is(payload + payload2));
    }

    @Test
    public void commentBlockMultiPartTwoSentencesTest() throws IOException {
        String payload = UUID.randomUUID().toString();
        String payload2 = UUID.randomUUID().toString();
        String payload3 = UUID.randomUUID().toString();
        AISManagedConnection aisManagedConnection = new AISManagedConnection(null);
        aisManagedConnection.getSentences();
        BufferedReader bufferMock = mock(BufferedReader.class);
        when(bufferMock.readLine()).thenReturn("\\1G2:310,s:452,c:1652227201*6B\\!ABVDM,2,1,1,A," + payload + ",0*1A",
                "\\2G2:310*4F\\!ABVDM,2,2,1,A," + payload2 + ",2*26",
                "\\1G1:32,s:516,c:1652227200*5B\\!ABVDM,1,1,0,B," + payload3 + ",0*7D", null);
        aisManagedConnection.read(bufferMock);
        List<Sentence> sentences = aisManagedConnection.getSentences();
        assertThat(sentences.size(), is(2));
        assertThat(sentences.get(0).hasValidCommentBlock(), is(true));
        assertThat(sentences.get(0).getCommentBlockLesTimestamp(), is(Instant.ofEpochSecond(1652227201)));
        assertThat(sentences.get(0).getSentence(), is(payload + payload2));
        assertThat(sentences.get(1).hasValidCommentBlock(), is(true));
        assertThat(sentences.get(1).getCommentBlockLesTimestamp(), is(Instant.ofEpochSecond(1652227200)));
        assertThat(sentences.get(1).getSentence(), is(payload3));
    }
}
