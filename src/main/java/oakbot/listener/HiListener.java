package oakbot.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import static oakbot.listener.Listener.reply;

/**
 * Displays a "wave" emoticon in response to another user's wave emoticon.
 * @author Michael Angstadt
 */
public class HiListener implements Listener {
	private final Pattern waveRegex = Pattern.compile("(^|\\s)(hi|hello)(\\s|$)");
	private final long timeBetweenWaves = TimeUnit.MINUTES.toMillis(5);
	private final long hesitation;
	private final String botUsername;
	private final MentionListener mentionListener;
	private final Map<Integer, Long> lastWaves = new HashMap<>();

	/**
	 * @param botUsername the bot's username
	 * @param hesitation the amount of time to wait before waving back (in
	 * milliseconds)
	 * @param mentionListener the mention listener
	 */
	public HiListener(String botUsername, long hesitation, MentionListener mentionListener) {
		this.botUsername = botUsername;
		this.hesitation = hesitation;
		this.mentionListener = mentionListener;
	}

	@Override
	public String name() {
		return "hi";
	}

	@Override
	public String description() {
		return "replies back";
	}

	@Override
	public String helpText() {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, BotContext context) {
		String content = message.getContent();
		boolean mentioned = message.isMentioned(botUsername);

		String wave;
		if (mentioned) {
			/*
			 * If mentioned, look for the emoticon somewhere within the message
			 * text.
			 */
			Matcher m = waveRegex.matcher(content);
			if (!m.find()) {
				return null;
			}

			mentionListener.ignoreNextMessage();
			wave = m.group(2);
		} else {
			/*
			 * If not mentioned, only respond if the message consists of just
			 * the emoticon.
			 */
			if (!content.equals("hi") && !content.equals("hello")) {
				return null;
			}

			int roomId = message.getRoomId();
			Long lastWave = lastWaves.get(roomId);
			if (lastWave == null) {
				lastWave = 0L;
			}

			/*
			 * Do not respond if the bot was not mentioned and it responded
			 * recently. Always wave back to admins.
			 */
			long now = System.currentTimeMillis();
			if (!context.isAuthorAdmin() && now - lastWave < timeBetweenWaves) {
				return null;
			}

			lastWaves.put(roomId, now);
			wave = content;
		}

		/*
		 * Wait for a moment to make it seem less robotic.
		 */
		try {
			Thread.sleep(hesitation);
		} catch (InterruptedException e) {
			//empty
		}

		String reply = reverse(wave);
		return reply(reply,message);
	}

	private String reverse(String wave) {
		return wave.equals("hi") ? "hi" : "hello";
	}
}
