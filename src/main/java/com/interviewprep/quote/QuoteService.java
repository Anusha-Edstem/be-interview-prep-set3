package com.interviewprep.quote;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class QuoteService {

    static final List<QuoteResponse> QUOTES = List.of(
            new QuoteResponse("Simplicity is prerequisite for reliability.", "Edsger W. Dijkstra"),
            new QuoteResponse("Premature optimization is the root of all evil.", "Donald Knuth"),
            new QuoteResponse("Talk is cheap. Show me the code.", "Linus Torvalds"),
            new QuoteResponse("Make it work, make it right, make it fast.", "Kent Beck"),
            new QuoteResponse("Programs must be written for people to read.", "Harold Abelson"),
            new QuoteResponse("The best error message is the one that never shows up.", "Thomas Fuchs"));

    public QuoteResponse randomQuote() {
        return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
    }
}
