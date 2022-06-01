package fish.focus.uvms.ais;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Sentence {

	private String commentBlock;
	private String payload;

	public Sentence(String commentBlock, String payload) {
		this.commentBlock = commentBlock;
		this.payload = payload;
	}

	public String getSentence() {
		return payload;
	}

	public boolean hasValidCommentBlock() {
		if (commentBlock == null) {
			return false;
		}
		String[] commentBlockAndChecksum = commentBlock.split("\\*");
        int calculatedSum = commentBlockAndChecksum[0].chars().boxed().reduce(0, (a, b) -> a ^= b);
        return Integer.parseInt(commentBlockAndChecksum[1], 16) == calculatedSum;
	}

	public Instant getCommentBlockLesTimestamp() {
		if (commentBlock == null) {
			return null;
		}
		String[] commentBlockAndChecksum = commentBlock.split("\\*");
        Map<String, String> commentBlockProperties = Arrays.asList(commentBlockAndChecksum[0].split(","))
                .stream()
                .map(pair -> pair.split(":"))
                .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
        if (!commentBlockProperties.containsKey("c")) {
            return null;
        }
        return Instant.ofEpochSecond(Long.valueOf(commentBlockProperties.get("c")));
	}
}
