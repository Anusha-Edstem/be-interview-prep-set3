package com.interviewprep.quote;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuoteServiceTest {

    private final QuoteService quoteService = new QuoteService();

    @Test
    @DisplayName("RL-01 random quote is always taken from the catalogue")
    void randomQuoteComesFromCatalogue() {
        QuoteResponse quote = quoteService.randomQuote();

        assertThat(QuoteService.QUOTES).contains(quote);
        assertThat(quote.text()).isNotBlank();
        assertThat(quote.author()).isNotBlank();
    }

    @Test
    @DisplayName("RL-01 random quote varies across calls")
    void randomQuoteVaries() {
        Set<QuoteResponse> seen = new HashSet<>();

        IntStream.range(0, 500).forEach(i -> seen.add(quoteService.randomQuote()));

        assertThat(seen).hasSizeGreaterThan(1).isSubsetOf(QuoteService.QUOTES);
    }
}
